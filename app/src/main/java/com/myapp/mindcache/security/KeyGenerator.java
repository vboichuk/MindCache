package com.myapp.mindcache.security;

import javax.crypto.SecretKey;

public interface KeyGenerator {

    SecretKey deriveSecretKey(char[] userPassword, byte[] salt) throws Exception;

    byte[] generateSalt();

    byte[] deriveKey(char[] password, byte[] salt) throws CryptoException;

    byte[] generateMasterKey();
}
