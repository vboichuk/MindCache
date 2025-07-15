package com.myapp.mindcache.datastorage;

import android.content.Context;
import android.security.keystore.UserNotAuthenticatedException;
import android.text.TextUtils;
import android.util.Log;

import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.security.KeyGenerator;
import com.myapp.mindcache.security.KeyGeneratorImpl;
import com.myapp.mindcache.security.NoteEncryptionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class NoteRepository {
    private static final String TAG = NoteRepository.class.getSimpleName();
    private final NoteDao noteDao;
    private final NoteEncryptionService encryptionService;

    public NoteRepository(Context context, NoteEncryptionService encryptionService) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.noteDao = db.noteDao();
        this.encryptionService = encryptionService;
    }

    public Flowable<List<Note>> getAllNotes(char[] password) {
        validatePassword(password);
        return noteDao.getAllNotes()
                .subscribeOn(Schedulers.io())
                .map(notes -> {
                    try {
                        List<Note> decryptedNotes = new ArrayList<>();
                        for (Note note : notes) {
                            decryptedNotes.add(encryptionService.decryptNote(note, password));
                        }
                        return decryptedNotes;
                    } finally {
                        clearPassword(password);
                    }
                })
                .doOnError(e -> Log.e(TAG, "Error loading notes", e));
    }

    public Completable insertNote(String title, String content, char[] password) {

        validatePassword(password);
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return Completable.error(new IllegalArgumentException("Title/content cannot be empty"));
        }

        return Completable.fromAction(() -> {
                    try {
                        Note note = new Note(title, content, System.currentTimeMillis());
                        KeyGenerator generator = new KeyGeneratorImpl();
                        byte[] salt = generator.generateSalt();
                        note.setSalt(Base64.getEncoder().encodeToString(salt));
                        long id = noteDao.insert(encryptionService.encryptNote(note, password));
                        Log.e(TAG, "Added note id:" + id);
                    } catch (Exception e) {
                        Log.e(TAG, "Encryption failed: " + e.getMessage()); // Без stacktrace
                        throw new SecurityException("Encryption error");
                    } finally {
                        clearPassword(password);
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error adding note", e));
    }

    public Single<Note> getNoteById(long id, char[] password) {
        validatePassword(password);
        return Single.fromCallable(() -> {
            try {
                Note note = noteDao.getById(id);
                if (note == null)
                    return null;
                note = encryptionService.decryptNote(note, password);
                return note;
            } finally {
                clearPassword(password);
            }
        }).subscribeOn(Schedulers.io());
    }

    public Completable deleteNote(long id) {

        return Completable.fromAction(() -> {
                    try {
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

    public Completable updateNote(long id, String title, String content, char[] password) {
        validatePassword(password);
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        return Completable.fromAction(() -> {
                    try {
                        Note existingNote = noteDao.getById(id);
                        if (existingNote == null) {
                            throw new IllegalArgumentException("Note with ID " + id + " not found");
                        }
                        existingNote.setTitle(title);
                        existingNote.setContent(content);
                        existingNote = encryptionService.encryptNote(existingNote, password);

                        noteDao.update(existingNote);
                        Log.d(TAG, "Note title and content updated for ID: " + id);
                    } catch (UserNotAuthenticatedException e) {
                        Log.e(TAG, "Authentication expired", e);
                        throw e;
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating note title and content", e);
                        throw new SecurityException("Failed to update note", e);
                    } finally {
                        clearPassword(password);
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error updating note title and content", e));
    }

    private void validatePassword(char[] password) throws IllegalArgumentException {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    private void clearPassword(char[] password) {
        Arrays.fill(password, '\0');
        Log.e(TAG, "password was cleared");
    }
}