package com.myapp.mindcache.datastorage;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.myapp.mindcache.model.Note;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.crypto.SecretKey;

import io.reactivex.Completable;
import io.reactivex.Flowable;
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

    // В NoteRepository
    public Flowable<List<Note>> getAllDecryptedNotes() {
        return noteDao.getAllNotes()
                .subscribeOn(Schedulers.io())
                .map(notes -> {
                    List<Note> decryptedNotes = new ArrayList<>();
                    for (Note note : notes) {
                        LocalDateTime dateTime =
                                LocalDateTime.ofInstant(Instant.ofEpochMilli(note.createdAt),
                                        TimeZone.getDefault().toZoneId());

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

    public void deleteNote(int id) {
        noteDao.delete(id);
    }
}