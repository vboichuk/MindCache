package com.myapp.mindcache.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import io.reactivex.Single;

import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;
import com.myapp.mindcache.model.NotePreview;

import java.util.List;

@Dao
public interface NoteDao {

    @Query("SELECT " +
            "id, " +
            "created_at as createdAt, " +
            "CASE WHEN salt IS NULL THEN 0 ELSE 1 END as isSecret, " +
            "CASE WHEN salt IS NULL AND title IS NOT NULL " +
            "     THEN CASE WHEN LENGTH(title) > 32 " +
            "               THEN SUBSTR(title, 1, 32) || '...' " +
            "               ELSE title END " +
            "     ELSE NULL END as titleHint " +
            "FROM notes ORDER BY created_at DESC")
    LiveData<List<NoteMetadata>> getNotesMetadata();

    // Полная заметка по ID (зашифрованная)
    @Query("SELECT * FROM notes WHERE id = :id")
    Single<Note> getEncryptedNoteById(long id);

    // Неполная заметка по ID (зашифрованная)
    @Query("SELECT " +
            "id, " +
            "CASE WHEN salt IS NULL THEN 0 ELSE 1 END as isSecret, " +
            "title, " +
            "preview, " +
            "salt, " +
            "created_at as createdAt " +
            "FROM notes " +
            "WHERE id = :id " +
            "LIMIT 1")
    Single<NotePreview> getEncryptedPreviewById(long id);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getById(long id);

    @Insert
    long insert(Note note);

    @Query("DELETE FROM notes WHERE id = :id")
    int delete(long id);

    @Update
    void update(Note note);

    @Query("UPDATE notes SET created_at = :millisBack WHERE id = :id")
    void changeDatetime(Long id, long millisBack);


}