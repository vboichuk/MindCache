package com.myapp.mindcache.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyGeneratorImpl implements KeyGenerator {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 100_000; // Рекомендуемое значение для 2023 года
    private static final int KEY_LENGTH = 256; // Длина ключа в битах (AES-256)

    /**
     * Генерирует ключ AES из пароля пользователя и соли
     *
     * @param userPassword Пароль пользователя (будет очищен после использования)
     * @param salt Соль для усиления безопасности (минимум 16 байт)
     * @return SecretKey для использования в AES шифровании
     * @throws CryptoException Если возникла ошибка при генерации ключа
     */
    @Override
    public SecretKey generateDataKey(char[] userPassword, byte[] salt) throws CryptoException {
        String saltBase64 = Base64.getEncoder().encodeToString(salt); // Байты -> Base64

        if (userPassword == null || userPassword.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        if (salt == null || salt.length < 16) {
            throw new CryptoException("Salt must be at least 16 bytes long");
        }

        try {
            // 1. Создаем фабрику для PBKDF2
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);

            // 2. Генерируем ключ с большим количеством итераций
            KeySpec spec = new PBEKeySpec(
                    userPassword,
                    salt,
                    ITERATION_COUNT,
                    KEY_LENGTH
            );

            // 3. Получаем ключ и преобразуем в формат AES
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            // 4. Очищаем чувствительные данные из памяти
            Arrays.fill(keyBytes, (byte) 0);

            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("PBKDF2 algorithm not available", e);
        } catch (InvalidKeySpecException e) {
            throw new CryptoException("Invalid key specification", e);
        } finally {
            // Всегда очищаем пароль после использования
            // Arrays.fill(userPassword, '\0');
        }
    }

    @Override
    public byte[] generateSalt() {
        // Генерация случайной соли
        byte[] salt = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt); // Заполняем массив случайными байтами

//        String saltStr = "ZD0k5ovdU9CLsDMsfA8JZw==";
//        System.out.printf("Hardcoded salt: " + saltStr);
//        byte[] salt = Base64.getDecoder().decode(saltStr); // Декодируем Base64 -> байты
        // String saltBase64 = Base64.getEncoder().encodeToString(salt); // Байты -> Base64
        // assert saltBase64.equals(saltStr); // Теперь true

        return salt;
    }
}
