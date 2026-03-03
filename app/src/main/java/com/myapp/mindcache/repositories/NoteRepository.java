package com.myapp.mindcache.repositories;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.myapp.mindcache.datastorage.AppDatabase;
import com.myapp.mindcache.dao.NoteDao;
import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.dto.NoteUpdateDto;
import com.myapp.mindcache.mappers.NodeMapper;
import com.myapp.mindcache.model.EncryptedNote;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.security.NoteEncryptionService;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
        return noteDao.getNotesMetadata();
    }

    public Single<NotePreview> getDecryptedPreview(long noteId, byte[] masterKey) {
        return noteDao.getEncryptedPreviewById(noteId)
                .map(encryptedNote -> encryptionService.decryptPreview(encryptedNote, masterKey));
    }

    public Single<Note> getDecryptedNote(long noteId, byte[] masterKey) {
        return noteDao.getEncryptedNoteById(noteId)
                .map(encryptedNote -> encryptionService.decryptNote(encryptedNote, masterKey));
    }

    public Single<NotePreview> insertNote(NoteCreateDto dto, byte[] masterKey) {
        if (dto == null || TextUtils.isEmpty(dto.getTitle()) || TextUtils.isEmpty(dto.getContent())) {
            return Single.error(new IllegalArgumentException("Note data is invalid"));
        }

        return Single.fromCallable(() -> {
                    Note note = NodeMapper.fromDto(dto);
                    EncryptedNote encryptedNote = encryptionService.encryptNote(note, masterKey);
                    long id = noteDao.insert(encryptedNote);
                    note.setId(id);
                    return NodeMapper.toPreview(note);
                })
                .subscribeOn(Schedulers.io());
    }

    public static String getPreview(String content) {
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
                .subscribeOn(Schedulers.io());
    }

    public Single<NotePreview> updateNote(NoteUpdateDto dto, byte[] masterKey) {
        if (TextUtils.isEmpty(dto.getTitle()) || TextUtils.isEmpty(dto.getContent())) {
            return Single.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        return Single.fromCallable(() -> {
                    EncryptedNote existing = noteDao.getById(dto.getId());
                    if (existing == null) {
                        throw new IllegalArgumentException("Note with ID " + dto.getId() + " not found");
                    }

                    existing.setTitle(dto.getTitle());
                    existing.setContent(dto.getContent());
                    existing.setPreview(getPreview(dto.getContent()));

                    Note note = NodeMapper.fromDto(dto);
                    EncryptedNote encrypted = encryptionService.encryptNote(note, masterKey);
                    noteDao.update(encrypted);
                    Log.d(TAG, "Note title and content updated for ID: " + dto.getId());
                    return NodeMapper.toPreview(note);
                })
                .subscribeOn(Schedulers.io());
    }

    public Completable updateDateTime(long id, LocalDateTime datetime) {
        return Completable.fromAction(() -> {
                    long millis = datetime.atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();
                    noteDao.changeDatetime(id, millis);
                })
                .subscribeOn(Schedulers.io())
                .doOnComplete(() -> Log.d(TAG, "Date updated: " + id))
                .doOnError(error -> Log.e(TAG, "Date update failed: " + id, error));
    }
}