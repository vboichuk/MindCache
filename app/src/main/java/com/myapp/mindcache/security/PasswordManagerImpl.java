package com.myapp.mindcache.security;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import javax.crypto.AEADBadTagException;
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
    public String getUserPassword() throws AEADBadTagException {
        String encryptedPassword = loadEncryptedPassword();
        System.out.println("encryptedPassword: " + encryptedPassword);
        CryptoHelper helper = new CryptoHelper();
        try {
            SecretKey secretKey = keystoreKeyManager.getOrCreateKey(keyAlias);
            String decrypted = helper.decrypt(encryptedPassword, secretKey);
            Log.d(TAG, "decrypted password: " + decrypted);
            // pass: 0000
            return decrypted;
        } catch (AEADBadTagException e) {
            System.out.println(e.getMessage());
            throw e;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "";
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

    @Override
    public void resetPassword() {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        prefs
                .edit()
                .remove(keyAlias)
                .apply();
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
