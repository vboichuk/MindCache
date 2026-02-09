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
    private long createdAt = System.currentTimeMillis();

    private String title;    // encrypted
    private String content;  // encrypted
    private String salt;     // Base64-соль (null, если заметка не зашифрована)

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

    public Note(long id, String decryptedTitle, String decryptedContent, long createdAt, String salt) {
        this.id = id;
        this.title = decryptedTitle;
        this.content = decryptedContent;
        this.createdAt = createdAt;
        this.salt = salt;
    }

    public void clearSensitiveData() {
        content = null;
        title = null;
    }


    // Геттеры
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


    // Сеттеры
    public void setId(long id) {
        this.id = id;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}