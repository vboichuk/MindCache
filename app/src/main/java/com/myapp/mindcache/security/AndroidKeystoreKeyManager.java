package com.myapp.mindcache.security;

import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;

import javax.crypto.SecretKey;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class AndroidKeystoreKeyManager {

    private static final String TAG = AndroidKeystoreKeyManager.class.getSimpleName();
    private static final String KEY_STORE = "AndroidKeyStore";
    private static final int KEY_VALIDITY_IN_SECONDS = 5 * 60;

    private final KeyStore keyStore;

    public AndroidKeystoreKeyManager() throws
            KeyStoreException,
            IOException,
            NoSuchAlgorithmException,
            CertificateException {
        this.keyStore = KeyStore.getInstance(KEY_STORE);
        this.keyStore.load(null);
    }

    public SecretKey getSecretKey(String keyAlias) throws
            UnrecoverableKeyException,
            KeyStoreException,
            NoSuchAlgorithmException {
        if (!keyStore.containsAlias(keyAlias))
            throw new KeyStoreException("Key with alias '" + keyAlias + "' does not exist");

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
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(KEY_VALIDITY_IN_SECONDS)
                .build();

        if (!keyStore.containsAlias(keyAlias)) {
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            keyStore.setEntry(keyAlias, secretKeyEntry, keyProtection);
            Log.i(TAG, "Key successfully imported under alias: " + keyAlias);
        } else
            Log.w(TAG, "Key with alias " + keyAlias + " already exists");
    }
}