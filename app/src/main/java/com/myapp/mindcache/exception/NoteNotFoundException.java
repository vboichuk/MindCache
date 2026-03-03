package com.myapp.mindcache.exception;

public class NoteNotFoundException extends Exception {
    public NoteNotFoundException(long id) {
        super("Note with id: " + id + " not found");
    }
}
