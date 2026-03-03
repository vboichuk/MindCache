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
    private String preview;

    @Ignore
    private boolean isEncrypted;


    @Ignore
    public NotePreview(NotePreview other) {
        this.id = other.id;
        this.createdAt = other.createdAt;
        this.title = other.title;
        this.preview = other.preview;
        this.isEncrypted = other.isEncrypted;
    }

    public NotePreview(long id, String title, String preview, long createdAt) {
        this.id = id;
        this.title = title;
        this.preview = preview;
        this.createdAt = createdAt;
        this.isEncrypted = true;
    }

    public long getId() { return id; }

    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public LocalDateTime getCreatedAt() {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(createdAt),
                ZoneId.systemDefault());
    }

    public String getTitle() { return title; }

    public String getPreview() { return preview; }

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
        return id == that.id
                && createdAt == that.createdAt
                && Objects.equals(title, that.title)
                && Objects.equals(preview, that.preview);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, createdAt, title, preview);
    }
}
