package com.myapp.mindcache.model;

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
    private String salt;     // Base64-соль (null, если заметка не зашифрована)

    @Ignore
    private boolean isSecret;

    public Note(long id, String title, String content, String preview, long createdAt, boolean secret) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.preview = preview;
        this.createdAt = createdAt;
        this.salt = null;
        this.isSecret = secret;
    }

    public static Note createEmpty() {
        return new Note(0L, "", "", "", System.currentTimeMillis(), null);
    }

    public Note(long id, String title, String content, String preview, long createdAt, String salt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.preview = preview;
        this.createdAt = createdAt;
        this.salt = salt;
        this.isSecret = salt != null;
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
        this.salt = other.salt;
        this.isSecret = salt != null;
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

    public String getSalt() {
        return salt;
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
        if (!isSecret)
            salt = null;
    }

    public boolean isEncrypted() {
        return salt != null;
    }
}