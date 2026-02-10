package com.myapp.mindcache.dto;

public class NoteUpdateDto {

    private final Long id;
    private final String title;
    private final String content;
    private final boolean isSecret;

    public NoteUpdateDto(Long id, String title, String content, boolean isSecret) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.isSecret = isSecret;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isSecret() {
        return isSecret;
    }
}
