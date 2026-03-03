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
import com.myapp.mindcache.security.KeyGenerator;
import com.myapp.mindcache.security.KeyGeneratorImpl;
import com.myapp.mindcache.security.KeyManager;
import com.myapp.mindcache.security.NoteEncryptionService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    public NotesViewModel(@NonNull Application application, @NonNull KeyManager keyManager) {
        super(application);
        this.keyManager = keyManager;
        try {
            KeyGenerator generator = new KeyGeneratorImpl();
            NoteEncryptionService service = new NoteEncryptionService(generator);
            repository = new NoteRepository(application, service);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize NoteRepository", e);
        }

        observeRepository();
    }

    private void observeRepository() {
        repository.getNotesMetadata().observeForever(metadata -> {
            // Log.i(TAG, "notesMetadata updated! " + metadata.stream().map(NoteMetadata::getId).collect(Collectors.toList()));
            Log.i(TAG, "notesMetadata updated with " + metadata.size() + " items");
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

    public void prefetchNotes(List<Long> ids) {
        Log.i(TAG, "prefetchNotes: " + ids);
        for (Long noteId : ids) {
            Disposable disposable = prefetchNote(noteId)
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> Log.d(TAG, "Prefetched: " + noteId),
                            error -> Log.e(TAG, "Failed to prefetch: " + noteId, error)
                    );
            disposables.add(disposable);
        }
    }

    public Completable prefetchNote(long noteId) {

        if (isNoteCached(noteId) || isNoteLoading(noteId)) {
            return Completable.complete();
        }
        markAsLoading(noteId);

        byte[] masterKey;
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

        Optional<Note> draft = getDraftForNote(noteId);
        if (draft.isPresent()) {
            return Single.just(draft.get());
        }

        if (noteId == 0L) {
            return Single.just(Note.createEmpty());
        }

        byte[] masterKey;
        try {
            masterKey = keyManager.getMasterKey();
        } catch (AuthError | Exception e) {
            errors.postValue(e);
            return Single.error(e);
        }

        return repository.getDecryptedNote(noteId, masterKey)
                .subscribeOn(Schedulers.io());
    }

    public Completable addNote(NoteCreateDto dto) {

        if (TextUtils.isEmpty(dto.getTitle()) || TextUtils.isEmpty(dto.getContent())) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        byte[] masterKey;
        try {
            masterKey = dto.isSecret()
                    ? keyManager.getMasterKey()
                    : null;
        } catch (AuthError | Exception  e) {
            errors.postValue(e);
            return Completable.error(e);
        }

        return repository.insertNote(dto, masterKey)
                .doOnSuccess(
                        note -> {
                            addToCache(note);
                            clearDraft(0L);
                            Log.i(TAG, "Successfully added note with id: " + note.getId());
                        })
                .ignoreElement()
                .subscribeOn(Schedulers.io());
    }

    public Completable updateNote(NoteUpdateDto updateDto) {
        if (TextUtils.isEmpty(updateDto.getTitle()) || TextUtils.isEmpty(updateDto.getContent())) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }
        byte[] masterKey;
        try {
            masterKey = keyManager.getMasterKey();
        } catch (AuthError | Exception e) {
            errors.postValue(e);
            return Completable.error(e);
        }
        return repository.updateNote(updateDto, masterKey)
                .doOnSuccess(note -> {
                    addToCache(note);
                    clearDraft(updateDto.getId());
                })
                .ignoreElement()
                .subscribeOn(Schedulers.io())
                .doOnError(errors::postValue);
    }

    public void saveDraft(long noteId, String title, String content, boolean secret) {
        noteDraft = new Note(noteId, title, content, "", System.currentTimeMillis(), secret);
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
                .doOnError(error -> {
                    Log.e(TAG, error.getMessage(), error);
                    errors.postValue(error);
                });
    }

    private void addToCache(NotePreview newNote) {
        Map<Long, NotePreview> current = decryptedNotes.getValue();
        if (current != null) {
            current.put(newNote.getId(), newNote);
            decryptedNotes.postValue(current);
        }
    }

    private void addToCache(Note note) {
        NotePreview notePreview = new NotePreview(
                note.getId(),
                note.getTitle(),
                note.getPreview(),
                note.getCreatedAt(),
                note.isSecret()
        );
        addToCache(notePreview);
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

    public Completable changeNoteDate(Long id, LocalDateTime datetime) {
        return repository.updateDateTime(id, datetime)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}