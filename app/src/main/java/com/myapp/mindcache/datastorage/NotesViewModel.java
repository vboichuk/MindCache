package com.myapp.mindcache.datastorage;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;
import com.myapp.mindcache.security.CryptoHelper;
import com.myapp.mindcache.security.KeyGenerator;
import com.myapp.mindcache.security.KeyGeneratorImpl;
import com.myapp.mindcache.security.NoteEncryptionService;
import com.myapp.mindcache.security.PasswordManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.AEADBadTagException;

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
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<NoteMetadata>> notesMetadata = new MutableLiveData<>();
    private final MutableLiveData<Map<Long, Note>> decryptedNotes = new MutableLiveData<>(new HashMap<>());

    private final PasswordManager passwordManager;

    public NotesViewModel(@NonNull Application application, PasswordManager passwordManager) {
        super(application);
        System.out.println("NotesViewModel.NotesViewModel");
        this.passwordManager = passwordManager;
        try {
            KeyGenerator generator = new KeyGeneratorImpl();
            CryptoHelper cryptoHelper = new CryptoHelper();
            NoteEncryptionService service = new NoteEncryptionService(generator, cryptoHelper);
            repository = new NoteRepository(application, service);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize NoteRepository", e);
        }
    }

    public void init() {

        repository.getNotesForList().observeForever(metadata -> {
            notesMetadata.postValue(metadata);

            // 2. Сразу начинаем дешифровку первых 3-5 заметок (видимых)
            decryptFirstVisibleNotes(metadata);
        });
    }


    public LiveData<List<NoteMetadata>> getNotesMetadata() {
        return notesMetadata;
    }

    public LiveData<Map<Long, Note>> getDecryptedNotes() {
        return decryptedNotes;
    }

    public LiveData<Throwable> getErrors() {
        return errors;
    }

    private void decryptFirstVisibleNotes(List<NoteMetadata> metadata) {

        System.out.println("NotesViewModel.decryptFirstVisibleNotes");

        if (metadata == null || metadata.isEmpty()) return;

        int notesToDecrypt = Math.min(5, metadata.size());

        for (int i = 0; i < notesToDecrypt; i++) {
            decryptNote(metadata.get(i).id);
        }
    }

    public void decryptNote(long noteId) {
        char[] password = getPasswordOrThrow();

        // Обновляем конкретную заметку в UI
        disposables.add(
                repository.getDecryptedNote(noteId, password)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::addToDecrypted,
                                error -> {
                                    // Оставляем заглушку
                                    Log.e("Diary", "Failed to decrypt note: " + noteId, error);
                                }
                        )
        );
    }


    public Single<Note> getNoteById(long noteId) {
        // Проверяем кэш
        Map<Long, Note> cached = decryptedNotes.getValue();
        if (cached != null && cached.containsKey(noteId)) {
            Note cachedNote = cached.get(noteId);
            assert cachedNote != null;
            return Single.just(cachedNote);
        }

        char[] password = getPasswordOrThrow();
        return repository.getNoteById(noteId, password)
                .map(note -> {
                    addToDecrypted(note);
                    return note;
                });
    }

    public Completable addNote(String title, String content) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        isLoading.postValue(true);
        char[] password = null;
        try {
            password = passwordManager.getUserPassword().toCharArray();
        } catch (AEADBadTagException e) {
            e.printStackTrace();
            password = "".toCharArray();
        }

        Note newNote = new Note(title, content, System.currentTimeMillis());

        char[] finalPassword = password;
        return repository.insertNote(newNote, password)
                .doOnSuccess(id -> {
                    Log.i(TAG, "Successfully added note with id: " + id);
                    newNote.setId(id);
                    addToDecrypted(newNote);
                })
                .ignoreElement() // Преобразуем Single<Long> в Completable
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                })
                .doOnError(error -> {
                    Log.e(TAG, "Error adding note", error);
                    errors.postValue(error);
                    isLoading.postValue(false);
                })
                .doOnDispose(() -> {
                    // Очистка пароля при отмене операции
                    if (finalPassword != null) {
                        Arrays.fill(finalPassword, '\0');
                    }
                });
    }

    public Completable updateNote(long id, String title, String content) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        isLoading.postValue(true);
        char[] password = getPasswordOrThrow();

        char[] finalPassword = password;
        return repository.updateNote(id, title, content, password)
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                    // Не нужно обновлять notes вручную - Flowable из Room сделает это автоматически
                })
                .doOnError(error -> {
                    Log.e(TAG, "Error updating note", error);
                    errors.postValue(error);
                    isLoading.postValue(false);
                })
                .doOnDispose(() -> {
                    // Очистка пароля при отмене операции
                    if (finalPassword != null) {
                        Arrays.fill(finalPassword, '\0');
                    }
                });
    }

    public Completable deleteNote(long id) {
        isLoading.postValue(true);

        Completable completable = repository.deleteNote(id) // Уже возвращает Completable
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                    // decryptedNotes.re
//                    if (cachedNotes != null) {
//                        cachedNotes.removeIf(note -> note.getId() == id);
//                        shortNotes.postValue(convertToFeedItems(cachedNotes));
//                    }
                })
                .doOnError(error -> {
                    errors.postValue(error);
                    isLoading.postValue(false);
                });
        return completable;
    }

    private char[] getPasswordOrThrow() {
        try {
            return passwordManager.getUserPassword().toCharArray();
        } catch (AEADBadTagException e) {
            throw new RuntimeException("AEADBadTagException in getPasswordOrThrow");
        }
    }

    private void addToDecrypted(Note newNote) {
        Map<Long, Note> current = decryptedNotes.getValue();
        if (current != null) {
            current.put(newNote.getId(), newNote);
            decryptedNotes.postValue(current);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.dispose();
        Log.d(TAG, "ViewModel cleared");
    }
}