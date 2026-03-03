package com.myapp.mindcache.model;


public class NoteMetadata {

    public final Long id;
    public final Long createdAt;

    public NoteMetadata(Long id, Long createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }
}
