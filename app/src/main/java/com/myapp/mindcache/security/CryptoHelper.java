package com.myapp.mindcache.security;

import android.util.Base64;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import kotlin.NotImplementedError;

public class CryptoHelper {
    private static final String TAG = CryptoHelper.class.getSimpleName();
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private final SecretKey secretKey;

    public CryptoHelper(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public String encrypt_old(String data) throws Exception {
        throw new NotImplementedError();
    }

    public String encrypt(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] iv = cipher.getIV();
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    public String decrypt_old(String title) throws Exception {
        throw new NotImplementedError();
    }

    public String decrypt(String encryptedData, SecretKey key) throws Exception {

        byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT);
        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedBytes = new byte[combined.length - IV_LENGTH];

        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(AES_MODE);

        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
    }

    private static void showBiometricPrompt(FragmentActivity activity, Runnable onSuccess) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Требуется аутентификация")
                .setSubtitle("Подтвердите личность для доступа к данным")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                activity,
                Executors.newSingleThreadExecutor(),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        onSuccess.run();  // Вызываем колбэк после успешной аутентификации
                    }
                });
        biometricPrompt.authenticate(promptInfo);
    }

}