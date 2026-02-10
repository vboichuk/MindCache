package com.myapp.mindcache.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class NotePreview {
    private final long id;
    private final LocalDateTime dateTime;
    private String emoji;
    private String title;
    private String content; // 3 строки
    private boolean isEncrypted = true;
    private boolean isSecret = false;

    public NotePreview(long id, LocalDateTime dateTime) {
        this.id = id;
        this.dateTime = dateTime;
    }

    public static NotePreview of (long id, String title, String content, LocalDateTime dateTime, String emoji) {
        NotePreview notePreview = new NotePreview(id, dateTime);
        notePreview.title = title;
        notePreview.content = content;
        notePreview.emoji = emoji;
        notePreview.isEncrypted = false;
        return notePreview;
    }

    public static NotePreview of (long id, String titleHint, LocalDateTime dateTime) {
        NotePreview notePreview = new NotePreview(id, dateTime);
        notePreview.title = titleHint;
        notePreview.content = "";
        notePreview.emoji = "";
        return notePreview;
    }

    public long getId() { return id; }
    public LocalDateTime getDateTime() { return dateTime; }
    public String getEmoji() { return emoji; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isEncrypted() { return isEncrypted; }
    public boolean isSecret() {
        return isSecret;
    }

    public void setSecret(boolean secret) {
        isSecret = secret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotePreview that = (NotePreview) o;
        return id == that.id && isEncrypted == that.isEncrypted && isSecret == that.isSecret && Objects.equals(dateTime, that.dateTime) && Objects.equals(emoji, that.emoji) && Objects.equals(title, that.title) && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateTime, emoji, title, content, isEncrypted, isSecret);
    }
}
