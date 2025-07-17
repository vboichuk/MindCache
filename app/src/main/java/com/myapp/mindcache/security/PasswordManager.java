package com.myapp.mindcache.security;

public interface PasswordManager {
    String getUserPassword();

    void setUserPassword(char[] password);

    boolean isPasswordSet();
}
