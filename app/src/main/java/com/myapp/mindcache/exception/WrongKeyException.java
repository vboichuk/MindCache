package com.myapp.mindcache.exception;

public class WrongKeyException extends Exception {
    public WrongKeyException() {
        super("Wrong key");
    }
}
