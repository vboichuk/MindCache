package com.myapp.mindcache.datastorage;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import io.reactivex.Flowable;

import com.myapp.mindcache.model.Note;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    long insert(Note note); // Возвращает ID новой записи

    @Update
    void update(Note note); // Метод для обновления существующей записи

    @Query("SELECT COUNT(*) FROM notes")
    Flowable<Integer> getCount();

    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    Flowable<List<Note>> getAllNotes();

    @Query("DELETE FROM notes WHERE id = :id")
    int delete(long id);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getById(long id);
}