package com.myapp.mindcache.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "created_at")
    public long createdAt = System.currentTimeMillis();

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "is_encrypted", defaultValue = "1")
    public boolean isEncrypted;

    public Note(long id, String title, String content, long createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    @Ignore
    public Note(String title, String content, long createdAt) {
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    public void clearSensitiveData() {
        content = null;
        title = null;
    }
}