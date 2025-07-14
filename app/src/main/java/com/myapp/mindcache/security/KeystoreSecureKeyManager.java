package com.myapp.mindcache.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import java.security.KeyStore;

public class KeystoreSecureKeyManager implements SecureKeyManager {

    private static final String TAG = KeystoreSecureKeyManager.class.getSimpleName();
    private static final String keyAlias = "secure_db_key";
    private static final String KEY_STORE = "AndroidKeyStore";

    private final KeyStore keyStore;

    public KeystoreSecureKeyManager() throws Exception {
        this.keyStore = KeyStore.getInstance("AndroidKeyStore");
        this.keyStore.load(null);
    }

    @Override
    public SecretKey getOrCreateKey() throws Exception {
        Log.i(TAG, "SecretKey getOrCreateKey()");

        if (!keyStore.containsAlias(keyAlias)) {
            createKey();
        }
        return (SecretKey) keyStore.getKey(keyAlias, null);
    }

    private void createKey() throws Exception {
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
                .setUserAuthenticationValidityDurationSeconds(30)
                // .setIsStrongBoxBacked(true) // Только для устройств с StrongBox
                .build();

        keyGenerator.init(keySpec);
        keyGenerator.generateKey();
    }
}