package com.myapp.mindcache.model;


public class NoteMetadata {

    public final Long id;
    public final Long createdAt;
    public final boolean isSecret;
    public String titleHint;

    public NoteMetadata(Long id, Long createdAt, boolean isSecret) {
        this.id = id;
        this.createdAt = createdAt;
        this.isSecret = isSecret;
    }

    public Long getId() {
        return id;
    }
}
