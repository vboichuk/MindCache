package com.myapp.mindcache.repositories;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.room.EmptyResultSetException;

import com.myapp.mindcache.datastorage.AppDatabase;
import com.myapp.mindcache.dao.NoteDao;
import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.dto.NoteUpdateDto;
import com.myapp.mindcache.exception.NoteNotFoundException;
import com.myapp.mindcache.mappers.NoteMapper;
import com.myapp.mindcache.model.EncryptedNote;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.security.NoteEncryptionService;

import java.util.List;

import javax.crypto.SecretKey;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class NoteRepository {
    private static final String TAG = NoteRepository.class.getSimpleName();
    private final NoteEncryptionService encryptionService;
    private final Context context;

    public NoteRepository(Context context, NoteEncryptionService encryptionService) {
        this.context = context;
        this.encryptionService = encryptionService;
    }

    public LiveData<List<NoteMetadata>> getNotesMetadata() {
        return noteDao().getNotesMetadata();
    }

    public Single<NotePreview> getDecryptedPreview(long noteId, SecretKey masterKey) {
        return noteDao().getEncryptedPreviewById(noteId)
                .map(encryptedNote -> encryptionService.decryptPreview(encryptedNote, masterKey));
    }

    public Single<Note> getDecryptedNote(long noteId, SecretKey masterKey) {
        return noteDao().getEncryptedNoteById(noteId)
                .onErrorResumeNext(throwable -> {
                    Log.e(TAG, throwable.getMessage(), throwable);
                    if (throwable instanceof EmptyResultSetException) {
                        return Single.error(new NoteNotFoundException(noteId));
                    }
                    return Single.error(throwable);
                })
                .map(encryptedNote -> encryptionService.decryptNote(encryptedNote, masterKey));
    }

    public Single<NotePreview> insertNote(NoteCreateDto dto, SecretKey masterKey) {
        if (dto == null || TextUtils.isEmpty(dto.getTitle()) || TextUtils.isEmpty(dto.getContent())) {
            return Single.error(new IllegalArgumentException("Note data is invalid"));
        }

        return Single.fromCallable(() -> {
                    Note note = NoteMapper.fromDto(dto);
                    EncryptedNote encryptedNote = encryptionService.encryptNote(note, masterKey);
                    long id = noteDao().insert(encryptedNote);
                    note.setId(id);
                    return NoteMapper.toPreview(note);
                })
                .subscribeOn(Schedulers.io());
    }

    public Completable deleteNote(long id) {
        return Completable.fromAction(() -> {
                    int deletedCount = noteDao().delete(id);
                    if (deletedCount == 0) {
                        throw new NoteNotFoundException(id);
                    }
                    Log.i(TAG, "Note deleted with id: " + id);
                })
                .subscribeOn(Schedulers.io());
    }

    public Single<NotePreview> updateNote(NoteUpdateDto dto, SecretKey masterKey) {
        if (TextUtils.isEmpty(dto.getTitle()) || TextUtils.isEmpty(dto.getContent())) {
            return Single.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        return Single.fromCallable(() -> {
                    EncryptedNote existing = noteDao().getById(dto.getId());
                    if (existing == null) {
                        throw new NoteNotFoundException(dto.getId());
                    }
                    String preview = NoteMapper.generatePreview(dto.getContent());
                    existing.setTitle(encryptionService.encrypt(dto.getTitle(), masterKey));
                    existing.setContent(encryptionService.encrypt(dto.getContent(), masterKey));
                    existing.setPreview(encryptionService.encrypt(preview, masterKey));
                    existing.setCreatedAt(dto.getCreatedAt());
                    noteDao().update(existing);
                    Log.d(TAG, "Note title and content updated for ID: " + dto.getId());
                    return new NotePreview(
                            existing.getId(),
                            dto.getTitle(),
                            preview,
                            existing.getCreatedAt()
                    );
                })
                .subscribeOn(Schedulers.io());
    }

    protected NoteDao noteDao() {
        return AppDatabase.getInstance(context).noteDao();
    }
}