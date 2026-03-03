package com.myapp.mindcache.dto;

public class NoteUpdateDto {

    private final Long id;
    private final String title;
    private final String content;


    private final long createdAt;

    public NoteUpdateDto(Long id, String title, String content, long createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
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

    public long getCreatedAt() {
        return createdAt;
    }

}
