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
    private String salt;     // Base64-соль (null, если заметка не зашифрована)

    @Ignore
    private boolean isSecret;

    public Note(long id, String title, String content, long createdAt, String salt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.salt = salt;
        this.isSecret = salt != null;
    }

    @Ignore
    public Note(String title, String content, long createdAt, boolean isSecret) {
        this.title = title;
        this.content = content;
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