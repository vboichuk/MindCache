package com.myapp.mindcache.datastorage;

import android.content.Context;
import android.security.keystore.UserNotAuthenticatedException;
import android.text.TextUtils;
import android.util.Log;

import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.security.CryptoHelper;
import com.myapp.mindcache.security.KeyGenerator;
import com.myapp.mindcache.security.KeyGeneratorImpl;
import com.myapp.mindcache.security.SPSecureKeyManager;
import com.myapp.mindcache.security.SecureKeyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import kotlin.NotImplementedError;

public class NoteRepository {
    private static final String TAG = NoteRepository.class.getSimpleName();
    private final NoteDao noteDao;
    private final CryptoHelper cryptoHelper;

    public NoteRepository(Context context) throws Exception {
        AppDatabase db = AppDatabase.getInstance(context);
        this.noteDao = db.noteDao();

        // SecureKeyManager secureKeyManager = new KeystoreSecureKeyManager();
        SecureKeyManager secureKeyManager = new SPSecureKeyManager();

        SecretKey secretKey = secureKeyManager.getOrCreateKey();
        this.cryptoHelper = new CryptoHelper(secretKey);
    }

    public Flowable<List<Note>> getAllNotes() {
        return noteDao.getAllNotes()
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error loading notes", e));
    }

    public Flowable<List<Note>> getAllDecryptedNotes(char[] password) {
        return noteDao.getAllNotes()
                .subscribeOn(Schedulers.io())
                .map(notes -> {
                    List<Note> decryptedNotes = new ArrayList<>();
                    for (Note note : notes) {
                        KeyGenerator generator = new KeyGeneratorImpl();
                        byte[] salt = Base64.getDecoder().decode(note.getSalt());
                        SecretKey key = generator.generateDataKey(password, salt);
                        decryptedNotes.add(new Note(
                                note.getId(),
                                cryptoHelper.decrypt(note.getTitle(), key),
                                cryptoHelper.decrypt(note.getContent(), key),
                                note.getCreatedAt()
                        ));
                    }
                    return decryptedNotes;
                })
                .doOnError(e -> Log.e(TAG, "Error loading notes", e));
    }

    public Completable addNote(String title, String content, char[] password) {

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return Completable.error(new IllegalArgumentException("Title/content cannot be empty"));
        }

        Log.d(TAG, "Attempting to add note: " + title);

        KeyGenerator generator = new KeyGeneratorImpl();
        generator.generateSalt();
        final byte[] salt = generator.generateSalt();
        SecretKey secretKey = null;

        try {
            secretKey = generator.generateDataKey(password, salt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SecretKey finalSecretKey = secretKey;
        return Completable.fromAction(() -> {
                    try {
                        Log.d(TAG, "start: " + title);
                        String encryptedTitle = cryptoHelper.encrypt(title, finalSecretKey);
                        String encryptedContent = cryptoHelper.encrypt(content, finalSecretKey);
                        Log.d(TAG, "Encryption successful");
                        Note note = new Note(encryptedTitle, encryptedContent, System.currentTimeMillis());
                        note.setSalt(Base64.getEncoder().encodeToString(salt));

                        long id = noteDao.insert(note);
                        Log.d(TAG, "Note inserted with ID: " + id);
                    } catch (Exception e) {
                        Log.e(TAG, "Encryption failed:", e);
                        throw e;
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error adding note", e));
    }

    public Single<Note> getNoteById(long id, char[] password) {
        return Single.fromCallable(() -> {
            Note note = noteDao.getById(id);
            if (note != null) {
                byte[] salt = Base64.getDecoder().decode(note.getSalt());
                KeyGenerator generator = new KeyGeneratorImpl();
                SecretKey key = generator.generateDataKey(password, salt);
                note.setTitle(cryptoHelper.decrypt(note.getTitle(), key));
                note.setContent(cryptoHelper.decrypt(note.getContent(), key));
            }
            return note;
        }).subscribeOn(Schedulers.io());
    }

    public Completable deleteNote(long id) {

        return Completable.fromAction(() -> {
                    try {
                        Log.d(TAG, "start...");
                        int deletedCount = noteDao.delete(id);
                        if (deletedCount == 0) {
                            Log.w(TAG, "No note found with ID: " + id);
                            throw new RuntimeException("Note not found with ID: " + id);
                        }
                        Log.d(TAG, "Note deleted with ID: " + id + " (deleted " + deletedCount + " records)");
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting note with ID: " + id, e);
                        throw new RuntimeException("Failed to delete note", e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error in deleteNote", e));
    }

    public Completable updateNote(Note note, char[] password) {
        if (note == null || TextUtils.isEmpty(note.getTitle()) || TextUtils.isEmpty(note.getContent())) {
            return Completable.error(new IllegalArgumentException("Note data is invalid"));
        }

        if (true)
            throw new NotImplementedError();

        return Completable.fromAction(() -> {
                    try {
                        KeyGenerator generator = new KeyGeneratorImpl();
                        if (note.getSalt() == null)
                            throw new NotImplementedError();
                        byte[] salt = Base64.getDecoder().decode(note.getSalt());
                        SecretKey key = generator.generateDataKey(password, salt);

                        // Шифруем данные перед сохранением
                        String encryptedTitle = cryptoHelper.encrypt(note.getTitle(), key);
                        String encryptedContent = cryptoHelper.encrypt(note.getContent(), key);

                        // Создаем новую заметку с зашифрованными данными, но тем же ID
                        Note encryptedNote = new Note(
                                note.getId(),
                                encryptedTitle,
                                encryptedContent,
                                note.getCreatedAt(),
                                note.getSalt()
                        );

                        noteDao.update(encryptedNote);
                        Log.d(TAG, "Note updated with ID: " + note.getId());
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating note", e);
                        throw new SecurityException("Failed to encrypt note data", e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error updating note", e));
    }

    /**
     * Обновляет только заголовок и содержимое заметки
     * @param id ID заметки для обновления
     * @param title Новый заголовок
     * @param content Новое содержимое
     * @return Completable для отслеживания операции
     */
    public Completable updateNote(long id, String title, String content, char[] password) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        return Completable.fromAction(() -> {
                    try {
                        System.out.println("Get existing note with id: " + id + "...");
                        Note existingNote = noteDao.getById(id);
                        if (existingNote == null) {
                            throw new IllegalArgumentException("Note with ID " + id + " not found");
                        }

                        KeyGenerator generator = new KeyGeneratorImpl();
                        byte[] salt = Base64.getDecoder().decode(existingNote.getSalt());
                        SecretKey key = generator.generateDataKey(password, salt);

                        // Шифруем новые данные
                        String encryptedTitle = cryptoHelper.encrypt(title, key);
                        String encryptedContent = cryptoHelper.encrypt(content, key);

                        // Обновляем только необходимые поля
                        existingNote.setTitle(encryptedTitle);
                        existingNote.setContent(encryptedContent);

                        // Сохраняем изменения
                        noteDao.update(existingNote);
                        Log.d(TAG, "Note title and content updated for ID: " + id);
                    } catch (UserNotAuthenticatedException e) {
                        Log.e(TAG, "Authentication expired", e);
                        throw e;
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating note title and content", e);
                        throw new SecurityException("Failed to update note", e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error updating note title and content", e));
    }
}