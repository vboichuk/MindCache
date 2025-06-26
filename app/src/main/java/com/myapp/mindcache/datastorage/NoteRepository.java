package com.myapp.mindcache.datastorage;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.myapp.mindcache.model.Note;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class NoteRepository {
    private static final String TAG = NoteRepository.class.getSimpleName();
    private final NoteDao noteDao;
    private final CryptoHelper cryptoHelper;

    public NoteRepository(Context context) throws Exception {
        AppDatabase db = AppDatabase.getInstance(context);
        this.noteDao = db.noteDao();

        SecureKeyManager secureKeyManager = new SecureKeyManager();
        SecretKey secretKey = secureKeyManager.getOrCreateKey();
        this.cryptoHelper = new CryptoHelper(secretKey);
    }

    public Flowable<List<Note>> getAllNotes() {
        return noteDao.getAllNotes()
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error loading notes", e));
    }

    public Flowable<List<Note>> getAllDecryptedNotes() {
        return noteDao.getAllNotes()
                .subscribeOn(Schedulers.io())
                .map(notes -> {
                    List<Note> decryptedNotes = new ArrayList<>();
                    for (Note note : notes) {
                        decryptedNotes.add(new Note(
                                note.id,
                                cryptoHelper.decrypt(note.title),
                                cryptoHelper.decrypt(note.content),
                                note.createdAt
                        ));
                    }
                    return decryptedNotes;
                })
                .doOnError(e -> Log.e(TAG, "Error loading notes", e));
    }

    public Completable addNote(String title, String content) {

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return Completable.error(new IllegalArgumentException("Title/content cannot be empty"));
        }

        Log.d(TAG, "Attempting to add note: " + title);

        return Completable.fromAction(() -> {
                    try {
                        Log.d(TAG, "start: " + title);
                        String encryptedTitle = cryptoHelper.encrypt(title);
                        String encryptedContent = cryptoHelper.encrypt(content);
                        Log.d(TAG, "Encryption successful");
                        Note note = new Note(encryptedTitle, encryptedContent, System.currentTimeMillis());
                        long id = noteDao.insert(note);
                        Log.d(TAG, "Note inserted with ID: " + id);
                    } catch (Exception e) {
                        Log.e(TAG, "Encryption failed", e);
                        throw new SecurityException("Failed to encrypt note data", e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error adding note", e));
    }

    public Single<Note> getNoteById(long id) {
        return Single.fromCallable(() -> {
            Note note = noteDao.getById(id);
            if (note != null) {
                note.title = cryptoHelper.decrypt(note.title);
                note.content = cryptoHelper.decrypt(note.content);
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

    public Completable updateNote(Note note) {
        if (note == null || TextUtils.isEmpty(note.title) || TextUtils.isEmpty(note.content)) {
            return Completable.error(new IllegalArgumentException("Note data is invalid"));
        }

        return Completable.fromAction(() -> {
                    try {
                        // Шифруем данные перед сохранением
                        String encryptedTitle = cryptoHelper.encrypt(note.title);
                        String encryptedContent = cryptoHelper.encrypt(note.content);

                        // Создаем новую заметку с зашифрованными данными, но тем же ID
                        Note encryptedNote = new Note(
                                note.id,
                                encryptedTitle,
                                encryptedContent,
                                note.createdAt
                        );

                        noteDao.update(encryptedNote);
                        Log.d(TAG, "Note updated with ID: " + note.id);
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
    public Completable updateNote(long id, String title, String content) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        return Completable.fromAction(() -> {
                    try {
                        // Получаем существующую заметку
                        Note existingNote = noteDao.getById(id);
                        if (existingNote == null) {
                            throw new IllegalArgumentException("Note with ID " + id + " not found");
                        }

                        // Шифруем новые данные
                        String encryptedTitle = cryptoHelper.encrypt(title);
                        String encryptedContent = cryptoHelper.encrypt(content);

                        // Обновляем только необходимые поля
                        existingNote.title = encryptedTitle;
                        existingNote.content = encryptedContent;

                        // Сохраняем изменения
                        noteDao.update(existingNote);
                        Log.d(TAG, "Note title and content updated for ID: " + id);
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating note title and content", e);
                        throw new SecurityException("Failed to update note", e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error updating note title and content", e));
    }
}