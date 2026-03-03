package com.myapp.mindcache.model;

import androidx.annotation.NonNull;

public class Note {

    private long id;

    private final long createdAt;
    private String title;
    private String content;

    public Note(long id, String title, String content, long createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static Note createEmpty() {
        return new Note(0L, "", "", System.currentTimeMillis());
    }

    public Note(String title, String content, String preview, long createdAt) {
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Note(Note other) {
        this.id = other.id;
        this.title = other.title;
        this.content = other.content;
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
        return "Note{" + (title == null ? "null" : title.substring(0,3) + ", " + (content == null ? "null" : content.substring(0, 5)) + "}");
    }
}