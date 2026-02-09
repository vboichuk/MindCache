package com.myapp.mindcache.security;

import javax.crypto.AEADBadTagException;

public interface PasswordManager {
    String getUserPassword() throws AEADBadTagException;

    void setUserPassword(char[] password);

    boolean isPasswordSet();

    void resetPassword();
}
