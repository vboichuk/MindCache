package com.myapp.mindcache.datastorage;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import io.reactivex.Flowable;
import io.reactivex.Single;

import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;

import java.util.List;

@Dao
public interface NoteDao {

    // ТОЛЬКО метаданные для списка
    @Query("SELECT id, created_at as createdAt FROM notes ORDER BY created_at DESC LIMIT :limit")
    LiveData<List<NoteMetadata>> getLatestNotesMetadata(int limit);

    // Полная заметка по ID (зашифрованная)
    @Query("SELECT * FROM notes WHERE id = :id")
    Single<Note> getEncryptedNoteById(long id);

    // ========

    @Insert
    long insert(Note note);

    @Query("DELETE FROM notes WHERE id = :id")
    int delete(long id);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getById(long id);

    @Update
    void update(Note note);

    /*
    @Query("SELECT COUNT(*) FROM notes")
    LiveData<Integer> getCount();

    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    Single<List<Note>> getAllNotes();

    @Query("SELECT * FROM notes ORDER BY created_at DESC LIMIT :limit")
    LiveData<List<Note>> getLatestNotes(int limit);



    @Query("SELECT id, created_at as createdAt FROM notes ORDER BY created_at DESC")
    LiveData<List<NoteMetadata>> getAllNotesMetadata();
     */
}