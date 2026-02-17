package com.myapp.mindcache.security;


import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.myapp.mindcache.exception.NoPasswordSavedException;

import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;

public class PasswordManagerImpl implements PasswordManager {
    private static final String TAG = PasswordManagerImpl.class.getSimpleName();
    private static final String KEY_ALIAS = "user_password_key";
    private static final String PREFS_ENCRYPTED_KEY = "encrypted_master_key";
    private final Context context;

    private final AndroidKeystoreKeyManager keystoreKeyManager;

    public PasswordManagerImpl(Context context, AndroidKeystoreKeyManager keystoreKeyManager) {
        this.context = context;
        this.keystoreKeyManager = keystoreKeyManager;
    }

    @Override
    public char[] getUserPassword() throws NoPasswordSavedException, AEADBadTagException, UserNotAuthenticatedException {
        if (!isPasswordSet())
            throw new NoPasswordSavedException();

        String encryptedPassword = loadEncryptedPassword(); // TODO: нужно ли постоянно обращаться в Preferences?

        try {
            SecretKey secretKey = keystoreKeyManager.getSecretKey(KEY_ALIAS);
            return CryptoHelper.decrypt(encryptedPassword, secretKey);
        } catch (AEADBadTagException | UserNotAuthenticatedException e) {
            Log.e(TAG, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return "".toCharArray();
        }
    }

    @Override
    public void setUserPassword(char[] password) {
        Log.i(TAG, "setUserPassword");
        validatePassword(password);
        try {
            String encrypted = encrypt(password);
            saveEncryptedPassword(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }


    private String encrypt(char[] password) throws Exception {
        SecretKey secretKey = keystoreKeyManager.getSecretKey(KEY_ALIAS);
        return CryptoHelper.encrypt(password, secretKey);
    }

    private void validatePassword(char[] password) {

    }

    @Override
    public void resetPassword() {
        keystoreKeyManager.removeKey(KEY_ALIAS);
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        prefs
                .edit()
                .remove(PREFS_ENCRYPTED_KEY)
                .apply();
    }

    @Override
    public boolean isPasswordSet() {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        return prefs.contains(PREFS_ENCRYPTED_KEY);
    }

    private String loadEncryptedPassword() {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        return prefs.getString(PREFS_ENCRYPTED_KEY, null);
    }

    private void saveEncryptedPassword(String password) {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        prefs
                .edit()
                .putString(PREFS_ENCRYPTED_KEY, password)
                .apply();
    }
}
