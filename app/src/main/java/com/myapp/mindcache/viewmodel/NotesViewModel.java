package com.myapp.mindcache.viewmodel;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.dto.NoteUpdateDto;
import com.myapp.mindcache.exception.AuthError;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.repositories.NoteRepository;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;
import com.myapp.mindcache.security.KeyManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class NotesViewModel extends AndroidViewModel {
    private static final String TAG = NotesViewModel.class.getSimpleName();

    private final NoteRepository repository;
    private final KeyManager keyManager;

    private final CompositeDisposable disposables = new CompositeDisposable();

    private final SingleLiveEvent<Throwable> errors = new SingleLiveEvent<>();
    private final MutableLiveData<List<NoteMetadata>> notesMetadata = new MutableLiveData<>();
    private final MutableLiveData<Map<Long, NotePreview>> decryptedNotes = new MutableLiveData<>(new HashMap<>());

    private Note noteDraft = null;

    private final Set<Long> decryptingIds = ConcurrentHashMap.newKeySet();

    public NotesViewModel(@NonNull Application application,
                          @NonNull KeyManager keyManager,
                          @NonNull NoteRepository repository) {
        super(application);
        this.keyManager = keyManager;
        this.repository = repository;
        observeRepository();
    }

    private void observeRepository() {
        repository.getNotesMetadata().observeForever(metadata -> {
            Log.d(TAG, "notesMetadata updated with " + metadata.size() + " items");
            notesMetadata.postValue(metadata);
        });
    }

    public LiveData<List<NoteMetadata>> getNotesMetadata() {
        return notesMetadata;
    }

    public LiveData<Map<Long, NotePreview>> getCachedPreviews() {
        return decryptedNotes;
    }

    public LiveData<Throwable> getErrors() {
        return errors;
    }

    public void prefetchPreviews(List<Long> ids) {
        Log.d(TAG, "prefetchPreviews: " + ids);

        for (Long noteId : ids) {
            Disposable disposable = prefetchPreview(noteId)
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> {},
                            error -> Log.e(TAG, String.format("Failed to prefetch id: %d (%s)", noteId, error.getMessage())));
            disposables.add(disposable);
        }
    }

    public Completable prefetchPreview(long noteId) {

        if (isNoteCached(noteId) || isNoteLoading(noteId)) {
            return Completable.complete();
        }
        markAsLoading(noteId);

        SecretKey masterKey;
        try {
            masterKey = keyManager.getMasterKey();
        } catch (AuthError | Exception e) {
            return Completable.error(e);
        }

        return repository.getDecryptedPreview(noteId, masterKey)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(this::addToCache)
                .doFinally(() -> unmarkAsLoading(noteId))
                .ignoreElement();
    }

    public Single<Note> getNoteById(long noteId) {
        return Single.defer(() -> {
            Log.d(TAG, "getNoteById...");
            Optional<Note> draft = getDraftForNote(noteId);
            if (draft.isPresent()) {
                return Single.just(draft.get());
            }

            if (noteId == 0L) {
                return Single.just(Note.createEmpty());
            }

            SecretKey masterKey;
            try {
                masterKey = keyManager.getMasterKey();
            } catch (AuthError | Exception e) {
                return Single.error(e);
            }

            return repository.getDecryptedNote(noteId, masterKey);
        });
    }

    public Completable addNote(NoteCreateDto dto) {

        return Completable.defer(() -> {
            Log.d(TAG, "try to add Note...");
            if (TextUtils.isEmpty(dto.getTitle()) || TextUtils.isEmpty(dto.getContent())) {
                return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
            }

            SecretKey masterKey;
            try {
                masterKey = keyManager.getMasterKey();
            } catch (AuthError | Exception e) {
                Log.e(TAG, "error: " + e.getMessage());
                // errors.postValue(e);
                return Completable.error(e);
            }

            return repository.insertNote(dto, masterKey)
                    .doOnSuccess(notePreview -> {
                        addToCache(notePreview);
                        clearDraft(0L);
                        Log.i(TAG, "Successfully added note with id: " + notePreview.getId());
                    })
                    .doOnError(throwable -> Log.e(TAG, "insertNote " + throwable.getClass() + ": " + throwable.getMessage()))
                    .ignoreElement();
        });
    }

    public Completable updateNote(NoteUpdateDto updateDto) {
        return Completable.defer(() -> {
            if (TextUtils.isEmpty(updateDto.getTitle()) || TextUtils.isEmpty(updateDto.getContent())) {
                return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
            }
            SecretKey masterKey;
            try {
                masterKey = keyManager.getMasterKey();
            } catch (AuthError | Exception e) {
                return Completable.error(e);
            }
            return repository.updateNote(updateDto, masterKey)
                    .flatMapCompletable(notePreview -> {
                        addToCache(notePreview);
                        clearDraft(updateDto.getId());
                        return Completable.complete();
                    })
//                    .doOnSuccess(notePreview -> {
//                        addToCache(notePreview);
//                        clearDraft(updateDto.getId());
//                    })
//                    .ignoreElement()
                    .subscribeOn(Schedulers.io());
        });
    }

    public void saveDraft(long noteId, String title, String content) {
        noteDraft = new Note(noteId, title, content, System.currentTimeMillis());
    }

    public Optional<Note> getDraftForNote(long noteId) {
        if (noteDraft != null && noteDraft.getId() == noteId)
            return Optional.of(noteDraft);
        return Optional.empty();
    }

    public void clearDraft(Long noteId) {
        if (noteDraft != null && noteDraft.getId() == noteId)
            noteDraft = null;
    }

    public Completable deleteNote(long id) {
        return repository.deleteNote(id)
                .subscribeOn(Schedulers.io())
                .doOnComplete(() -> removeFromCache(id))
                .doOnError(error -> Log.e(TAG, error.getMessage(), error));
    }

    private void addToCache(NotePreview newNote) {
        Map<Long, NotePreview> current = decryptedNotes.getValue();
        if (current != null) {
            current.put(newNote.getId(), newNote);
            decryptedNotes.postValue(current);
        }
    }

    private void removeFromCache(long id) {
        Map<Long, NotePreview> current = decryptedNotes.getValue();
        if (current != null) {
            current.remove(id);
            Log.d(TAG, "removeFromCache " + id);
            decryptedNotes.postValue(current);
        }
    }

    private boolean isNoteCached(long noteId) {
        Map<Long, NotePreview> cache = decryptedNotes.getValue();
        return cache != null && cache.containsKey(noteId);
    }

    private boolean isNoteLoading(long noteId) {
        return decryptingIds.contains(noteId);
    }

    private void markAsLoading(long noteId) {
        decryptingIds.add(noteId);
    }

    private void unmarkAsLoading(long noteId) {
        decryptingIds.remove(noteId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.dispose();
        Log.d(TAG, "ViewModel cleared");
    }
}