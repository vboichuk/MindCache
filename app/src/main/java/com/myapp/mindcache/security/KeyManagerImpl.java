package com.myapp.mindcache.security;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.myapp.mindcache.exception.AuthError;
import com.myapp.mindcache.model.MasterKeyEntity;
import com.myapp.mindcache.repositories.MasterKeyRepository;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class KeyManagerImpl implements KeyManager {

    private static final String TAG = KeyManagerImpl.class.getSimpleName();
    private static final String PREFS_PASSWORD_HASH = "password_hash";
    private static final String PREFS_PASSWORD_SALT = "password_salt";
    public static final String AUTH_PREFS = "auth_prefs";

    private final Context context;
    private final KeyGenerator keyGenerator;
    private final MasterKeyRepository masterKeyRepository;

    private byte[] cachedMasterKey;
    private long cacheTimestamp;

    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 минут

    public KeyManagerImpl(Context context,
                          KeyGenerator keyGenerator) {
        this.context = context;
        this.keyGenerator = keyGenerator;
        this.masterKeyRepository = new MasterKeyRepository(context);
    }

    @Override
    public Single<Boolean> isUserRegistered() {
        return masterKeyRepository.exists();
    }

    @Override
    public Completable login(char[] password) {

        return Completable.fromAction(() -> authorize(password))
                .andThen(masterKeyRepository.getMasterKeySingle().timeout(5, TimeUnit.SECONDS))
                .flatMap(mk -> {
                    SecretKey keyFromPassword = keyGenerator.deriveSecretKey(password, mk.keySalt);
                    byte[] masterKey = CryptoHelper.decrypt(mk.encryptedKey, keyFromPassword);
                    Arrays.fill(keyFromPassword.getEncoded(), (byte)0);
                    return Single.just(masterKey);
                })
                .flatMapCompletable(bytes -> Completable.fromAction(() -> cacheMasterKey(bytes)))
                .doFinally(() -> Arrays.fill(password, '\0'))
                .subscribeOn(Schedulers.io());
    }

    private void authorize(char[] password) {
        SharedPreferences prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
        byte[] storedHash = Base64.getDecoder().decode(prefs.getString(PREFS_PASSWORD_HASH, ""));
        byte[] authSalt = Base64.getDecoder().decode(prefs.getString(PREFS_PASSWORD_SALT, ""));
        byte[] hash;
        try {
            hash = keyGenerator.deriveKey(password, authSalt);
        } catch (CryptoException e) {
            throw new RuntimeException(e.getMessage());
        }

        if (!Arrays.equals(hash, storedHash))
            throw new SecurityException("Wrong password");
    }

    @Override
    public Completable registerUser(char[] password) {
        return isUserRegistered()
                .flatMapCompletable(isRegistered -> {
                    if (isRegistered) {
                        return Completable.error(new IllegalStateException("User already registered"));
                    }
                    return Completable.complete();
                })
                .doOnComplete(() -> Log.i(TAG, "Start registration"))
                .andThen(Completable.fromAction(() -> validatePassword(password)))
                .andThen(Single.fromCallable(() -> generateCredentials(password)))
                .doFinally(() -> Arrays.fill(password, '\0'))
                .flatMapCompletable(credentials -> saveEncodedKey(credentials.keySalt, credentials.passwordEncryptedKey)
                                .concatWith(Completable.fromAction( () -> saveAuthInfo(credentials.authSalt, credentials.passwordHash)))
                                .concatWith((Completable.fromAction(() -> cacheMasterKey(credentials.masterKey))))
                )
                .doOnComplete(() -> Log.i(TAG, "User registered successfully"))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public void logout() {

    }

    @Override
    public byte[] getMasterKey() throws AuthError {

        if (System.currentTimeMillis() - cacheTimestamp >= CACHE_TTL) {
            clearCache();
            throw new AuthError(AuthError.Reason.SESSION_EXPIRED);
        }

        if (cachedMasterKey == null)
            throw new AuthError(AuthError.Reason.NOT_AUTHENTICATED);

        return Arrays.copyOf(cachedMasterKey, cachedMasterKey.length);
    }

    private @NonNull Credentials generateCredentials(char[] password) throws Exception {
        byte[] authSalt = keyGenerator.generateSalt();
        byte[] passwordHash = keyGenerator.deriveKey(password, authSalt);

        byte[] keySalt = keyGenerator.generateSalt();
        byte[] masterKey = keyGenerator.generateMasterKey();

        SecretKey keyFromPassword = keyGenerator.deriveSecretKey(password, keySalt);
        byte[] passwordEncryptedKey = CryptoHelper.encrypt(masterKey, keyFromPassword);

        byte[] keyFromPasswordEncoded = keyFromPassword.getEncoded();
        if (keyFromPasswordEncoded != null) {
            Arrays.fill(keyFromPasswordEncoded, (byte) 0);
        }

        return new Credentials(authSalt, passwordHash, keySalt, masterKey, passwordEncryptedKey);
    }

    private void cacheMasterKey(byte[] masterKey) {
        cachedMasterKey = masterKey;
        cacheTimestamp = System.currentTimeMillis();
    }

    private void clearCache() {
        if (cachedMasterKey != null) {
            Arrays.fill(cachedMasterKey, (byte) 0);
            cachedMasterKey = null;
        }
        cacheTimestamp = System.currentTimeMillis();
    }

    @Override
    public void changePassword(char[] oldPassword, char[] newPassword) {

    }

    private void validatePassword(@NotNull char[] password) {
        if (password.length < 4)
            throw new SecurityException("Password length must be at least 4 symbols");
    }

    private void saveAuthInfo(byte[] authSalt, byte[] passwordHash) {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString(PREFS_PASSWORD_HASH, Base64.getEncoder().encodeToString(passwordHash))
                .putString(PREFS_PASSWORD_SALT, Base64.getEncoder().encodeToString(authSalt))
                .apply();
    }

    private Completable saveEncodedKey(byte[] keySalt, byte[] encryptedKey) {
        MasterKeyEntity masterKeyEntity = new MasterKeyEntity();
        masterKeyEntity.keySalt = keySalt;
        masterKeyEntity.encryptedKey = encryptedKey;
        masterKeyEntity.createdAt = System.currentTimeMillis();
        return masterKeyRepository.updateMasterKey(masterKeyEntity);
    }

    private static class Credentials {
        public final byte[] authSalt;
        public final byte[] passwordHash;
        public final byte[] keySalt;
        public final byte[] masterKey;
        public final byte[] passwordEncryptedKey;

        public Credentials(byte[] authSalt, byte[] passwordHash, byte[] keySalt, byte[] masterKey, byte[] passwordEncryptedKey) {
            this.authSalt = authSalt;
            this.passwordHash = passwordHash;
            this.keySalt = keySalt;
            this.masterKey = masterKey;
            this.passwordEncryptedKey = passwordEncryptedKey;
        }
    }
}
