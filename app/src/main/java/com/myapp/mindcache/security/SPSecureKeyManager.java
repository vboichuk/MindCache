package com.myapp.mindcache.security;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

public class SPSecureKeyManager implements SecureKeyManager {

    // Размер соли (обычно 16-32 байта)
    private static final int saltLength = 16;

    @Override
    public SecretKey getOrCreateKey() throws Exception {
        return null;
    }

    public void createKey(String userPassword) throws Exception {

        KeyGenerator generator = new KeyGeneratorImpl();
        byte[] salt = generator.generateSalt();

        SecretKey secretKey = generator.generateDataKey(userPassword.toCharArray(), salt);
        byte[] encoded = secretKey.getEncoded();
        System.out.println("Data key: " + Base64.getEncoder().encodeToString(encoded));

    }
}
