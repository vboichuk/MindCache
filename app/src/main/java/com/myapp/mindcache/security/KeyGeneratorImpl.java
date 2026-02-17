package com.myapp.mindcache.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyGeneratorImpl implements KeyGenerator {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 100_000; // Рекомендуемое значение для 2023 года
    private static final int KEY_LENGTH = 256; // Длина ключа в битах (AES-256)
    private static final int SALT_LENGTH = 16;
    private static final int MIN_SALT_LENGTH = 16;

    /**
     * Генерирует ключ AES из пароля пользователя и соли
     *
     * @param password Пароль пользователя (будет очищен после использования)
     * @param salt Соль для усиления безопасности (минимум 16 байт)
     * @return SecretKey для использования в AES шифровании
     * @throws CryptoException Если возникла ошибка при генерации ключа
     */
    @Override
    public SecretKey generateDataKey(char[] password, byte[] salt) throws CryptoException {

        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        if (salt == null || salt.length < MIN_SALT_LENGTH) {
            throw new CryptoException("Salt must be at least " + MIN_SALT_LENGTH + " bytes long");
        }

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);

            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            Arrays.fill(keyBytes, (byte) 0);

            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("PBKDF2 algorithm not available", e);
        } catch (InvalidKeySpecException e) {
            throw new CryptoException("Invalid key specification", e);
        } finally {
            // Всегда очищаем пароль после использования
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Генерирует соль длиной в 16 байт
     * @return
     */
    @Override
    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt);
        return salt;
    }
}
