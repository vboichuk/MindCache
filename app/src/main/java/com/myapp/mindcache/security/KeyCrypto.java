package com.myapp.mindcache.security;

import javax.crypto.SecretKey;

public interface KeyCrypto {
    byte[] encryptDataKey(SecretKey dataKey) throws CryptoException;
    SecretKey decryptDataKey(byte[] encryptedKey) throws CryptoException;
}
