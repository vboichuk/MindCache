package com.myapp.mindcache.repositories;

import android.content.Context;
import android.security.keystore.UserNotAuthenticatedException;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.myapp.mindcache.datastorage.AppDatabase;
import com.myapp.mindcache.dao.NoteDao;
import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.dto.NoteUpdateDto;
import com.myapp.mindcache.mappers.NodeMapper;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.security.NoteEncryptionService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class NoteRepository {
    private static final String TAG = NoteRepository.class.getSimpleName();
    private static final int MAX_PREVIEW_LENGTH = 100;
    private final NoteDao noteDao;
    private final NoteEncryptionService encryptionService;

    public NoteRepository(Context context, NoteEncryptionService encryptionService) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.noteDao = db.noteDao();
        this.encryptionService = encryptionService;
    }

    public LiveData<List<NoteMetadata>> getNotesMetadata() {
        return noteDao.getNotesMetadata(20);
    }

    public Single<NotePreview> getDecryptedPreview(long noteId, char[] password) {
        validatePassword(password);
        return noteDao.getEncryptedPreviewById(noteId)
                .map(encryptedNote -> encryptedNote.isEncrypted()
                        ? encryptionService.decryptPreview(encryptedNote, password)
                        : encryptedNote)
                .subscribeOn(Schedulers.io())
                .doOnDispose(() -> clearPassword(password));
    }

    public Single<Note> getDecryptedNote(long noteId, char[] password) {
        validatePassword(password);
        return noteDao.getEncryptedNoteById(noteId)
                .map(encryptedNote -> encryptedNote.isEncrypted()
                        ? encryptionService.decryptNote(encryptedNote, password)
                        : encryptedNote)
                .subscribeOn(Schedulers.io())
                .doOnDispose(() -> clearPassword(password));
    }


    public Single<Long> insertNote(NoteCreateDto dto, char[] password) {
        // Валидация входных данных
        if (dto == null || TextUtils.isEmpty(dto.getTitle()) || TextUtils.isEmpty(dto.getContent())) {
            return Single.error(new IllegalArgumentException("Note data is invalid"));
        }

        return Single.fromCallable(() -> {
                try {
                    Note note = NodeMapper.fromDto(dto);
                    note.setPreview(dto.getContent());
                    note = encryptNoteIfNeeded(password, note);
                    return noteDao.insert(note);
                } catch (Exception e) {
                    Log.e(TAG, "Insert failed: " + e.getMessage());
                    throw new SecurityException("Encryption error", e);
                } finally {
                    clearPassword(password);
                }
            })
            .subscribeOn(Schedulers.io())
            .doOnSuccess(id -> Log.d(TAG, "Inserted note with ID: " + id))
            .doOnError(e -> Log.e(TAG, "Error inserting note", e));
    }

    private String getPreview(String content) {
        if (content == null || content.isBlank())
            return "";

        return content.length() <= MAX_PREVIEW_LENGTH
                ? content
                : content.substring(0, MAX_PREVIEW_LENGTH) + "…";
    }

    public Completable deleteNote(long id) {
        return Completable.fromAction(() -> {
                    int deletedCount = noteDao.delete(id);
                    if (deletedCount == 0) {
                        throw new RuntimeException("Note not found with id: " + id);
                    }
                    Log.i(TAG, "Note deleted with id: " + id);
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error in deleteNote", e));
    }

    public Completable updateNote(NoteUpdateDto dto, char[] password) {
        validatePassword(password);
        if (TextUtils.isEmpty(dto.getTitle()) || TextUtils.isEmpty(dto.getContent())) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        return Completable.fromAction(() -> {
                    try {
                        Note existingNote = noteDao.getById(dto.getId());
                        if (existingNote == null) {
                            throw new IllegalArgumentException("Note with ID " + dto.getId() + " not found");
                        }

                        existingNote.setTitle(dto.getTitle());
                        existingNote.setContent(dto.getContent());
                        existingNote.setPreview(getPreview(dto.getContent()));
                        existingNote.setSecret(dto.isSecret());
                        existingNote = encryptNoteIfNeeded(password, existingNote);
                        noteDao.update(existingNote);

                        Log.d(TAG, "Note title and content updated for ID: " + dto.getId());
                    } catch (UserNotAuthenticatedException e) {
                        Log.e(TAG, "Authentication expired", e);
                        throw e;
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating dto title and content", e);
                        throw new SecurityException("Failed to update dto", e);
                    } finally {
                        clearPassword(password);
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error updating dto title and content", e));
    }


    private Note encryptNoteIfNeeded(char[] password, Note existingNote) throws Exception {
        if (existingNote.isSecret()) {
            return encryptionService.encryptNote(existingNote, password);
        }
        return existingNote;
    }

    private Note decryptIfNeeded(char[] password, Note encryptedNote) throws Exception {
        return encryptedNote.isEncrypted()
                ? encryptionService.decryptNote(encryptedNote, password)
                : encryptedNote;
    }

    private NotePreview decryptIfNeeded(char[] password, NotePreview encryptedNote) throws Exception {
        return encryptedNote.isEncrypted()
                ? encryptionService.decryptPreview(encryptedNote, password)
                : encryptedNote;
    }


    private void validatePassword(char[] password) throws IllegalArgumentException {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    private void clearPassword(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
            Log.i(TAG, "password was cleared");
        }
    }

    public void updateDateTime(Long id, LocalDateTime datetime) {
        long millisBack = datetime.atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        noteDao.changeDatetime(id, millisBack);
    }


}