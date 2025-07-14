package com.myapp.mindcache.security;

import javax.crypto.SecretKey;

public interface SecureKeyManager {
    SecretKey getOrCreateKey() throws Exception;
}
