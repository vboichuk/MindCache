package com.myapp.mindcache.exception;

public class AuthError extends Throwable {
    private final Reason reason;

    public enum Reason {
        NOT_AUTHENTICATED,
        SESSION_EXPIRED
        //,
        // BIOMETRY_REQUIRED
    }

    public AuthError(Reason reason) {
        super(reason.toString());
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
