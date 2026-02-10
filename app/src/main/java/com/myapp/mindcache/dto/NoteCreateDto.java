package com.myapp.mindcache.dto;

import androidx.annotation.NonNull;

public class NoteCreateDto {
    private final String title;
    private final String content;
    private final boolean secret;

    private final long createdAt;

    public NoteCreateDto(@NonNull String title, @NonNull String content, boolean secret, long createdAt) {
        this.title = title;
        this.content = content;
        this.secret = secret;
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isSecret() {
        return secret;
    }

    public long getCreatedAt() {
        return createdAt;
    }

}
