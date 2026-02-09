package com.myapp.mindcache.datastorage;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import io.reactivex.Flowable;
import io.reactivex.Single;

import com.myapp.mindcache.model.Note;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    long insert(Note note);

    @Update
    void update(Note note);

    @Query("SELECT COUNT(*) FROM notes")
    LiveData<Integer> getCount();

    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    Single<List<Note>> getAllNotes();

    @Query("DELETE FROM notes WHERE id = :id")
    int delete(long id);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getById(long id);
}