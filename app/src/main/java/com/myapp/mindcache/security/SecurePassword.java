package com.myapp.mindcache.security;

import java.util.Arrays;

public class SecurePassword implements AutoCloseable {
    private char[] password;

    public SecurePassword(char[] password) {
        this.password = password;
    }

    public char[] getPassword() {
        return password;
    }

    @Override
    public void close() {
        if (password != null) {
            Arrays.fill(password, '\0');
            password = null;
        }
    }
}
