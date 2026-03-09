package com.myapp.mindcache.security;

import com.myapp.mindcache.exception.CryptoException;

public interface KeyGenerator {

    byte[] generateSalt();

    byte[] generatePBKDF2Key(char[] password, byte[] salt) throws CryptoException;

    byte[] generateMasterKey();
}
