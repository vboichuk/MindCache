package com.myapp.mindcache.security;

import javax.crypto.SecretKey;

public interface KeyGenerator {
    SecretKey generateDataKey(char[] userPassword, byte[] salt) throws Exception;
    byte[] generateSalt();
}
