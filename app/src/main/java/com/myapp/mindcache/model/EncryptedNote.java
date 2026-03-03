package com.myapp.mindcache.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class EncryptedNote {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "created_at")
    private final long createdAt;
    private String title;    // encrypted
    private String content;  // encrypted
    private String preview;  // encrypted

    public EncryptedNote(long id, String title, String content, String preview, long createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.preview = preview;
        this.createdAt = createdAt;
    }

    public static EncryptedNote createEmpty() {
        return new EncryptedNote(0L, "", "", "", System.currentTimeMillis());
    }

    @Ignore
    public EncryptedNote(String title, String content, String preview, long createdAt) {
        this.title = title;
        this.content = content;
        this.preview = preview;
        this.createdAt = createdAt;
    }

    @Ignore
    public EncryptedNote(long id, String title, String content, long createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    @Ignore
    public EncryptedNote(EncryptedNote other) {
        this.id = other.id;
        this.title = other.title;
        this.content = other.content;
        this.preview = other.preview;
        this.createdAt = other.createdAt;
    }

    public long getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @NonNull
    @Override
    public String toString() {
        return "EncryptedNote{id:" + id + "}";
    }
}