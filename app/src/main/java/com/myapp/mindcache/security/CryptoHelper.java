package com.myapp.mindcache.security;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class CryptoHelper {

    private CryptoHelper() { }

    private static final String TAG = CryptoHelper.class.getSimpleName();
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    public static String encrypt(String text, SecretKey key) throws Exception {
        return encrypt(text.getBytes(StandardCharsets.UTF_8), key);
    }

    public static String encrypt(char[] characters, SecretKey key) throws Exception {
        byte[] bytes = convertCharsToBytes(characters);
        return encrypt(bytes, key);
    }

    public static String encrypt(byte[] bytes, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] iv = cipher.getIV();
        if (iv.length != IV_LENGTH) {
            throw new SecurityException("Invalid IV length from cipher");
        }

        byte[] encryptedBytes = cipher.doFinal(bytes);

        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }


    public static char[] decrypt(String encryptedData, SecretKey key) throws Exception {
        validateKey(key);
        byte[] combined = Base64.decode(encryptedData, Base64.NO_WRAP);
        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedBytes = new byte[combined.length - IV_LENGTH];

        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] bytes = cipher.doFinal(encryptedBytes);
        return convertBytesToChars(bytes);
    }


    private static void validateKey(SecretKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (!"AES".equals(key.getAlgorithm())) {
            throw new IllegalArgumentException("Key must be AES algorithm");
        }
    }

    private static char[] convertBytesToChars(byte[] bytes) {
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(
                ByteBuffer.wrap(bytes)
        );
        char[] result = new char[charBuffer.remaining()];
        charBuffer.get(result);
        return result;
    }

    private static byte[] convertCharsToBytes(char[] chars) {
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(
                CharBuffer.wrap(chars)
        );
        byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        return result;
    }
}