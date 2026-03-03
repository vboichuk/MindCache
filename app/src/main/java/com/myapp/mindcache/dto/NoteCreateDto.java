package com.myapp.mindcache.dto;

import androidx.annotation.NonNull;

public class NoteCreateDto {
    private final String title;
    private final String content;
    private final long createdAt;

    public NoteCreateDto(@NonNull String title, @NonNull String content, long createdAt) {
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

}
