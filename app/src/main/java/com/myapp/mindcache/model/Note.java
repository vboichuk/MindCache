package com.myapp.mindcache.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "created_at")
    private final long createdAt;
    private String title;    // encrypted
    private String content;  // encrypted
    private String preview;  // encrypted

    @Ignore
    private boolean isSecret;

    public Note(long id, String title, String content, String preview, long createdAt, boolean secret) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.preview = preview;
        this.createdAt = createdAt;
        this.isSecret = secret;
    }

    public static Note createEmpty() {
        return new Note(0L, "", "", "", System.currentTimeMillis());
    }

    public Note(long id, String title, String content, String preview, long createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.preview = preview;
        this.createdAt = createdAt;
        this.isSecret = true;
    }

    @Ignore
    public Note(String title, String content, String preview, long createdAt, boolean isSecret) {
        this.title = title;
        this.content = content;
        this.preview = preview;
        this.createdAt = createdAt;
        this.isSecret = isSecret;
    }

    @Ignore
    public Note(long id, String title, String content, long createdAt, boolean isSecret) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.isSecret = isSecret;
    }

    @Ignore
    public Note(Note other) {
        this.id = other.id;
        this.title = other.title;
        this.content = other.content;
        this.preview = other.preview;
        this.createdAt = other.createdAt;
        this.isSecret = other.isSecret;
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

    public boolean isSecret() {
        return isSecret;
    }

    public void setSecret(boolean secret) {
        this.isSecret = secret;
    }

    @NonNull
    @Override
    public String toString() {
        return "Note{" + (title == null ? "null" : title.substring(0,3) + ", " + (content == null ? "null" : content.substring(0, 5)) + "}");
    }
}