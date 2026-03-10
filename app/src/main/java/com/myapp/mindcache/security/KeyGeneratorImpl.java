package com.myapp.mindcache.security;

import com.myapp.mindcache.exception.CryptoException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class KeyGeneratorImpl implements KeyGenerator {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 600_000;
    private static final int KEY_LENGTH = 256; // Длина ключа в битах (AES-256)
    private static final int MASTER_KEY_LENGTH = 32; // 256 bits
    private static final int SALT_LENGTH = 16;
    private static final int MIN_SALT_LENGTH = 16;

    @Override
    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt);
        return salt;
    }

    @Override
    public byte[] generateMasterKey() {
        byte[] key = new byte[MASTER_KEY_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        return key;
    }

    /**
     * Генерирует ключ PBKDF2 из пароля пользователя и соли
     *
     * @param password Пароль пользователя (будет очищен после использования)
     * @param salt Соль для усиления безопасности (минимум 16 байт)
     * @return byte[] для использования в AES шифровании
     * @throws CryptoException Если возникла ошибка при генерации ключа
     */
    @Override
    public byte[] generatePBKDF2Key(char[] password, byte[] salt) throws CryptoException {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        if (salt == null || salt.length < MIN_SALT_LENGTH) {
            throw new CryptoException("Salt must be at least " + MIN_SALT_LENGTH + " bytes long");
        }

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
            return factory.generateSecret(spec).getEncoded();

        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("PBKDF2 algorithm not available", e);
        } catch (InvalidKeySpecException e) {
            throw new CryptoException("Invalid key specification", e);
        }
    }
}
