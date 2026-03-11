package com.myapp.mindcache.security;

import android.util.Base64;
import android.util.Log;

import com.myapp.mindcache.exception.WrongKeyException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class CryptoHelper {

    private static final String TAG = CryptoHelper.class.getSimpleName();
    public static final String ALGORITHM = "AES";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private CryptoHelper() { }

    public static String encrypt(String text, SecretKey key) throws Exception {
        return toBase64(encrypt(text.getBytes(StandardCharsets.UTF_8), key));
    }

    public static byte[] encrypt(byte[] bytes, SecretKey key)
            throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            IllegalBlockSizeException,
            BadPaddingException {
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
        return combined;
    }


    public static char[] decrypt(String encryptedData, SecretKey key) throws Exception {
        byte[] combined = Base64.decode(encryptedData, Base64.NO_WRAP);
        byte[] decrypted = decrypt(combined, key);
        return convertBytesToChars(decrypted);
    }

    public static byte[] decrypt(byte[] combined, SecretKey key)
            throws NoSuchPaddingException,
                NoSuchAlgorithmException,
                InvalidAlgorithmParameterException,
                InvalidKeyException,
                IllegalBlockSizeException,
                WrongKeyException
            {
        validateKey(key);

        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedBytes = new byte[combined.length - IV_LENGTH];

        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] bytes;
        try {
            bytes = cipher.doFinal(encryptedBytes);
        } catch (BadPaddingException e) {
            throw new WrongKeyException();
        }
        return bytes;
    }


    private static void validateKey(SecretKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (!ALGORITHM.equals(key.getAlgorithm())) {
            throw new IllegalArgumentException("Key must be " + ALGORITHM + " algorithm but got " + key.getAlgorithm());
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

    public static String toBase64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}