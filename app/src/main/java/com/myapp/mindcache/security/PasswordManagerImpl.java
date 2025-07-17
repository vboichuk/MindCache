package com.myapp.mindcache.security;


import android.content.Context;
import android.content.SharedPreferences;

import javax.crypto.SecretKey;

public class PasswordManagerImpl implements PasswordManager {
    private static final String TAG = "PasswordManagerImpl";
    private static final String keyAlias = "user_password_key";
    private final Context context;

    private final AndroidKeystoreKeyManager keystoreKeyManager;

    public PasswordManagerImpl(Context context, AndroidKeystoreKeyManager keystoreKeyManager) {
        this.context = context;
        this.keystoreKeyManager = keystoreKeyManager;
    }

    @Override
    public String getUserPassword() {
        String encryptedPassword = loadEncryptedPassword();
        CryptoHelper helper = new CryptoHelper();
        try {
            SecretKey secretKey = keystoreKeyManager.getOrCreateKey(keyAlias);
            return helper.decrypt(encryptedPassword, secretKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUserPassword(char[] password) {
        validatePassword(password);
        CryptoHelper helper = new CryptoHelper();
        try {
            SecretKey secretKey = keystoreKeyManager.getOrCreateKey(keyAlias);
            String encrypted = helper.encrypt(new String(password), secretKey);
            saveEncryptedPassword(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void validatePassword(char[] password) {

    }

    @Override
    public boolean isPasswordSet() {
        String bytes = loadEncryptedPassword();
        return bytes != null;
    }


    private String loadEncryptedPassword() {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        return prefs.getString(keyAlias, null);
    }

    private void saveEncryptedPassword(String password) {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        prefs
                .edit()
                .putString(keyAlias, password)
                .apply();
    }
}
