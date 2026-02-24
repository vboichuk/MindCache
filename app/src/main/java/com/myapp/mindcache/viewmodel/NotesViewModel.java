package com.myapp.mindcache.viewmodel;

import android.app.Application;
import android.security.keystore.UserNotAuthenticatedException;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.dto.NoteUpdateDto;
import com.myapp.mindcache.exception.AuthError;
import com.myapp.mindcache.mappers.NodeMapper;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.repositories.NoteRepository;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;
import com.myapp.mindcache.security.KeyGenerator;
import com.myapp.mindcache.security.KeyGeneratorImpl;
import com.myapp.mindcache.security.KeyManager;
import com.myapp.mindcache.security.KeyManagerImpl;
import com.myapp.mindcache.security.NoteEncryptionService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class NotesViewModel extends AndroidViewModel {
    private static final String TAG = NotesViewModel.class.getSimpleName();

    private final NoteRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<Throwable> errors = new MutableLiveData<>();
    private final MutableLiveData<List<NoteMetadata>> notesMetadata = new MutableLiveData<>();
    private final MutableLiveData<Map<Long, NotePreview>> decryptedNotes = new MutableLiveData<>(new HashMap<>());

    private final KeyManager keyManager;

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
            Log.i(TAG, "notesMetadata updated! " + metadata.stream().map(NoteMetadata::getId).collect(Collectors.toList()));
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

    public void prefetchNotes(List<Long> ids) throws AuthError, Exception {
        Log.i(TAG, "prefetchNotes: " + ids);
        for (Long noteId : ids) {
            prefetchNote(noteId);
        }
    }

    public void prefetchNote(long noteId) throws AuthError, Exception {

        if (isNoteCached(noteId) || isNoteLoading(noteId)) {
            return;
        }
        markAsLoading(noteId);

        // char[] password = getPasswordOrThrow();
        byte[] masterKey = keyManager.getMasterKey();

        disposables.add(
                repository.getDecryptedPreview(noteId, masterKey)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                newNote -> {
                                    addToCache(newNote);
                                    unmarkAsDecrypting(noteId);
                                },
                                error -> {
                                    unmarkAsDecrypting(noteId);
                                    Log.e(TAG, error.getMessage());
                                }
                        )
        );
    }

    public Single<Note> getNoteById(long noteId) throws AuthError, Exception {
        byte[] password = keyManager.getMasterKey();
        return repository.getDecryptedNote(noteId, password)
                .map(note -> {
                    // addToCache(note);
                    return note;
                });
    }

    public Completable addNote(NoteCreateDto dto) throws Exception, AuthError {

        if (TextUtils.isEmpty(dto.getTitle()) || TextUtils.isEmpty(dto.getContent())) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        byte[] masterKey = dto.isSecret()
                ? keyManager.getMasterKey()
                : null;

        return repository.insertNote(dto, masterKey)
                .flatMapCompletable(note -> {  // ← Репозиторий возвращает Note
                    addToCache(note);           // ← Кэшируем готовую Note
                    Log.i(TAG, "Successfully added note with id: " + note.getId());
                    return Completable.complete();
                })
                .doOnError(error -> {
                    Log.e(TAG, "Error adding note", error);
                    errors.postValue(error);
                });
    }

    public Completable updateNote(NoteUpdateDto updateDto) throws Exception, AuthError {
        if (TextUtils.isEmpty(updateDto.getTitle()) || TextUtils.isEmpty(updateDto.getContent())) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }
        byte[] masterKey = keyManager.getMasterKey();
        return repository.updateNote(updateDto, masterKey)
                .flatMapCompletable(note -> {  // ← Репозиторий возвращает Note
                    addToCache(note);           // ← Кэшируем готовую Note
                    Log.i(TAG, "Successfully updated note with id: " + note.getId());
                    return Completable.complete();
                })
                .doOnError(error -> {
                    Log.e(TAG, "Error updating note", error);
                    errors.postValue(error);
                });
    }

    public Completable deleteNote(long id) {
        return repository.deleteNote(id)
                .doOnComplete(() -> removeFromCache(id))
                .doOnError(value -> {
                    Log.e(TAG, "Delete failed", value);
                    errors.postValue(value);
                })
                .doOnDispose(() -> { });
    }

    private void addToCache(NotePreview newNote) {
        Map<Long, NotePreview> current = decryptedNotes.getValue();
        if (current != null) {
            current.put(newNote.getId(), newNote);
            decryptedNotes.postValue(current);
        }
    }

    private void addToCache(Note note) {
        addToCache(NodeMapper.toPreview(note));
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
        // Log.d(TAG, "+ markAsDecrypting " + noteId);
        decryptingIds.add(noteId);
    }

    private void unmarkAsDecrypting(long noteId) {
        // Log.d(TAG, "- unmarkAsDecrypting " + noteId);
        decryptingIds.remove(noteId);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.dispose();
        Log.d(TAG, "ViewModel cleared");
    }

    public Completable changeNoteDate(Long id, LocalDateTime datetime) {
        return Completable.fromAction(() ->
                        repository.updateDateTime(id, datetime)
                )
                .subscribeOn(Schedulers.io()) // Фоновый поток
                .observeOn(AndroidSchedulers.mainThread()); // Результат в UI поток
    }
}