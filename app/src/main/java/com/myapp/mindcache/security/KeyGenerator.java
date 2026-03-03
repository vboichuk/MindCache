package com.myapp.mindcache.security;

import com.myapp.mindcache.exception.CryptoException;

import javax.crypto.SecretKey;

public interface KeyGenerator {

    byte[] generateSalt();

    byte[] generatePBKDF2Key(char[] password, byte[] salt) throws CryptoException;

    SecretKey generateAESKey(byte[] keyBytes);

    byte[] generateMasterKey();
}
