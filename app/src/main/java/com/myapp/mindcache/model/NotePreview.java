package com.myapp.mindcache.model;

import androidx.room.Ignore;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public class NotePreview {
    private final long id;
    private final long createdAt;
    private String title;
    private String preview; // 3 строки
    private final String salt;
    private boolean isSecret;

    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }

    @Ignore
    private boolean isEncrypted;


    @Ignore
    public NotePreview(NotePreview other) {
        this.id = other.id;
        this.createdAt = other.createdAt;
        this.title = other.title;
        this.preview = other.preview;
        this.salt = other.salt;
        this.isSecret = other.isSecret;
        this.isEncrypted = other.isEncrypted;
    }

    public NotePreview(long id, String title, String preview, long createdAt, boolean isSecret, String salt) {
        this.id = id;
        this.title = title;
        this.preview = preview;
        this.createdAt = createdAt;
        this.isSecret = isSecret;
        this.salt = salt;
        this.isEncrypted = salt != null;
    }

    public long getId() { return id; }

    public String getSalt() {
        return salt;
    }

    public LocalDateTime getCreatedAt() {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(createdAt),
                ZoneId.systemDefault());
    }

    public String getEmoji() { return ""; }

    public String getTitle() { return title; }

    public String getPreview() { return preview; }

    public boolean isEncrypted() { return isEncrypted; }

    public boolean isSecret() {
        return isSecret;
    }


    public void setSecret(boolean secret) {
        isSecret = secret;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotePreview that = (NotePreview) o;
        return id == that.id && createdAt == that.createdAt && isSecret == that.isSecret && Objects.equals(title, that.title) && Objects.equals(preview, that.preview) && Objects.equals(salt, that.salt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, createdAt, title, preview, salt, isSecret);
    }

}
