package com.myapp.mindcache.security;

import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;

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
        return (SecretKey) keyStore.getKey(keyAlias, null);
    }

    public void removeKey(String keyAlias) throws KeyStoreException {
        this.keyStore.deleteEntry(keyAlias);
        Log.i(TAG, "Key with alias " + keyAlias + " has been deleted");
    }

    public void addSecretKey(String keyAlias, SecretKey secretKey) throws Exception {

        KeyProtection keyProtection = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build();

        if (!keyStore.containsAlias(keyAlias)) {
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            keyStore.setEntry(keyAlias, secretKeyEntry, keyProtection);

            Log.i(TAG, "Key successfully imported under alias: " + keyAlias);
        } else
            Log.w(TAG, "Key with alias " + keyAlias + " already exists");
    }
}