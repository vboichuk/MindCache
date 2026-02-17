package com.myapp.mindcache.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import java.security.KeyStore;
import java.security.KeyStoreException;

public class AndroidKeystoreKeyManager {

    private static final String TAG = AndroidKeystoreKeyManager.class.getSimpleName();
    private static final String KEY_STORE = "AndroidKeyStore";

    private final KeyStore keyStore;

    public AndroidKeystoreKeyManager() throws Exception {
        this.keyStore = KeyStore.getInstance(KEY_STORE);
        this.keyStore.load(null);
    }

    public SecretKey getSecretKey(String keyAlias) throws Exception {
        // Log.i(TAG, "getOrCreateKey()");

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
                .setUserAuthenticationValidityDurationSeconds(120)
                // .setIsStrongBoxBacked(true) // Только для устройств с StrongBox
                .build();

        keyGenerator.init(keySpec);
        keyGenerator.generateKey();
    }

    public void removeKey(String keyAlias) {
        try {
            this.keyStore.deleteEntry(keyAlias);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }
}