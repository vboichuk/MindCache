package com.myapp.mindcache.security;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.myapp.mindcache.exception.CryptoException;
import com.myapp.mindcache.model.MasterKeyEntity;
import com.myapp.mindcache.repositories.MasterKeyRepository;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class KeyManagerImpl implements KeyManager {

    private static final String TAG = KeyManagerImpl.class.getSimpleName();
    private static final String PREFS_PASSWORD_HASH = "password_hash";
    private static final String PREFS_PASSWORD_SALT = "password_salt";
    private static final String AUTH_PREFS = "auth_prefs";
    private static final String alias = "my_secret_key_alias";

    private final Context context;
    private final KeyGenerator keyGenerator;
    private final MasterKeyRepository masterKeyRepository;
    private final AndroidKeystoreKeyManager keystoreKeyManager;

    public KeyManagerImpl(Context context,
                          KeyGenerator keyGenerator,
                          AndroidKeystoreKeyManager keystoreKeyManager) {
        this.context = context;
        this.keyGenerator = keyGenerator;
        this.masterKeyRepository = new MasterKeyRepository(context);
        this.keystoreKeyManager = keystoreKeyManager;
    }

    @Override
    public Single<Boolean> isUserRegistered() {
        return masterKeyRepository.exists();
    }

    @Override
    public Completable login(char[] password) {
        char[] passCopy = password.clone();
        return Completable.fromAction(() -> authorize(passCopy));
    }

    private void authorize(@NonNull char[] password) {
        SharedPreferences prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
        byte[] storedHash = Base64.getDecoder().decode(prefs.getString(PREFS_PASSWORD_HASH, ""));
        byte[] authSalt = Base64.getDecoder().decode(prefs.getString(PREFS_PASSWORD_SALT, ""));
        byte[] hash;
        try {
            hash = keyGenerator.generatePBKDF2Key(password, authSalt);
        } catch (CryptoException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
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
                .flatMapCompletable(credentials ->
                        saveEncodedKey(credentials.keySalt, credentials.passwordEncryptedKey)
                                .concatWith(Completable.fromAction(() -> saveAuthInfo(credentials.authSalt, credentials.passwordHash)))
                                .concatWith(putToKeystore(credentials.masterKey))
                                .doFinally(credentials::clear)
                )
                .doOnComplete(() -> Log.i(TAG, "User registered successfully"));
    }

    @Override
    public Completable changePassword(char[] password, char[] newPassword) {
        // TODO: rollback!
        return Completable.fromAction(() -> validatePassword(newPassword))
                .andThen(login(password))
                .doOnComplete(() -> Log.i(TAG, "Changing password started"))
                .andThen(obtainRawMasterKey(password))
                .flatMap(masterKey -> Single.fromCallable(() -> generateCredentials(newPassword, masterKey)))
                .flatMapCompletable(credentials -> saveEncodedKey(credentials.keySalt, credentials.passwordEncryptedKey)
                        .concatWith(Completable.fromAction(() -> saveAuthInfo(credentials.authSalt, credentials.passwordHash)))
                )
                .doOnComplete(() -> Log.i(TAG, "Password changed successfully"))
                .doOnError(error -> Log.e(TAG, "Password changing failed", error))
                .subscribeOn(Schedulers.io());
    }

    private Single<byte[]> obtainRawMasterKey(char[] password) {
        return masterKeyRepository.getMasterKeySingle()
                .timeout(5, TimeUnit.SECONDS)
                .flatMap(mk -> decryptMasterKey(password, mk));
    }

    @Override
    public SecretKey getMasterKey() throws Exception {
        return loadFromKeystore();
    }

    private Single<byte[]> decryptMasterKey(char[] password, MasterKeyEntity mk) throws Exception {
        byte[] keyFromPassword = keyGenerator.generatePBKDF2Key(password, mk.keySalt);
        SecretKey aes = keyGenerator.generateAESKey(keyFromPassword);
        byte[] masterKey = CryptoHelper.decrypt(mk.encryptedKey, aes);
        return Single.just(masterKey);
    }

    private @NonNull Credentials generateCredentials(char[] password) throws Exception {
        byte[] masterKey = keyGenerator.generateMasterKey();
        return generateCredentials(password, masterKey);
    }

    private @NonNull Credentials generateCredentials(@NonNull char[] password, @NonNull byte[] masterKey) throws Exception {
        byte[] authSalt = keyGenerator.generateSalt();
        byte[] passwordHash = keyGenerator.generatePBKDF2Key(password, authSalt);
        byte[] keySalt = keyGenerator.generateSalt();

        byte[] keyFromPassword = keyGenerator.generatePBKDF2Key(password, keySalt);
        SecretKey aes = keyGenerator.generateAESKey(keyFromPassword);
        byte[] passwordEncryptedKey = CryptoHelper.encrypt(masterKey, aes);
        return new Credentials(authSalt, passwordHash, keySalt, masterKey, passwordEncryptedKey);
    }

    private void validatePassword(@NotNull char[] password) {
        Log.d(TAG, "validatePassword");
        if (password.length < 4)
            throw new IllegalArgumentException("Password length must be at least 4 symbols");
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

    private Completable putToKeystore(byte[] masterKey) {
        SecretKey secretKey = new SecretKeySpec(masterKey, "AES");
        java.util.Arrays.fill(masterKey, (byte) 0);

        try {
            keystoreKeyManager.addSecretKey(alias, secretKey);
            return Completable.complete();
        } catch (Exception e) {
            return Completable.error(e);
        }
    }

    private SecretKey loadFromKeystore() throws Exception {
        return keystoreKeyManager.getSecretKey(alias);
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

        public void clear() {
            if (authSalt != null)
                Arrays.fill(authSalt, (byte)0);
            if (passwordHash != null)
                Arrays.fill(passwordHash, (byte)0);
            if (keySalt != null)
                Arrays.fill(keySalt, (byte)0);
            if (masterKey != null)
                Arrays.fill(masterKey, (byte)0);
            if (passwordEncryptedKey != null)
                Arrays.fill(passwordEncryptedKey, (byte)0);
        }
    }
}