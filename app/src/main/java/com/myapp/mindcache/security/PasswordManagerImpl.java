package com.myapp.mindcache.security;


import android.util.Log;

import javax.crypto.SecretKey;

public class PasswordManagerImpl implements PasswordManager {
    private static final String TAG = "PasswordManagerImpl";
    private static final String keyAlias = "user_password_key";

    final KeystoreSecureKeyManager secureKeyManager;

    public PasswordManagerImpl(KeystoreSecureKeyManager manager) {
        this.secureKeyManager = manager;
    }

    @Override
    public char[] getUserPassword() {
        final String crypted = "Xy9B4iiTPIxx5mpP2a44NLgnRv3NO5s9b5lyjguWI1cE4Ktb";
        try {
            SecretKey secretKey = secureKeyManager.getOrCreateKey(keyAlias);
            CryptoHelper helper = new CryptoHelper();
            String decrypted = helper.decrypt(crypted, secretKey);
            Log.d(TAG, "decrypted: [" + decrypted + "]");
            return decrypted.toCharArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
