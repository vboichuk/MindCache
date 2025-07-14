package com.myapp.mindcache.security;

import java.security.KeyManagementException;

import javax.crypto.SecretKey;

public interface KeyStorage {
    SecretKey getOrCreateKey() throws KeyManagementException;
    boolean isUserAuthenticationRequired();
}
