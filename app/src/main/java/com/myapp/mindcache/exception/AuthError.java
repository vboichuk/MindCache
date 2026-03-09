package com.myapp.mindcache.exception;

public class AuthError extends Throwable {
    private final Reason reason;

    public enum Reason {

        NOT_AUTHENTICATED("User not authenticated"),
        USER_CANCELED("Authentication canceled by user"),
        SESSION_EXPIRED("Session expired, please authenticate again"),
        BIOMETRY_FAILED("Biometric authentication failed");

        private final String message;

        Reason(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

    }

    public AuthError(Reason reason) {
        super(reason.getMessage());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}