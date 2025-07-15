package com.myapp.mindcache.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import java.security.KeyStore;

public class KeystoreSecureKeyManager {

    private static final String TAG = "KeystoreSecureKeyManager";
    private static final String KEY_STORE = "AndroidKeyStore";

    private final KeyStore keyStore;

    public KeystoreSecureKeyManager() throws Exception {
        this.keyStore = KeyStore.getInstance("AndroidKeyStore");
        this.keyStore.load(null);
    }

    public SecretKey getOrCreateKey(String keyAlias) throws Exception {
        Log.i(TAG, "getOrCreateKey()");

        if (!keyStore.containsAlias(keyAlias)) {
            createKey(keyAlias);
        }
        return (SecretKey) keyStore.getKey(keyAlias, null);
    }

    private void createKey(String keyAlias) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEY_STORE
        );

        KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(600)
                // .setIsStrongBoxBacked(true) // Только для устройств с StrongBox
                .build();

        keyGenerator.init(keySpec);
        keyGenerator.generateKey();
    }
}