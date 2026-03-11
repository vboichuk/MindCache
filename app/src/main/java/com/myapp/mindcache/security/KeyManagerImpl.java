package com.myapp.mindcache.security;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.myapp.mindcache.model.MasterKeyEntity;
import com.myapp.mindcache.repositories.MasterKeyRepository;

import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
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
    private static final int VALIDATION_STRING_LENGTH = 32;

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
    public Completable authorize(char[] password) {
        return Completable.fromAction(() -> {
                    Log.d(TAG, "authorize");
                    SharedPreferences prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
                    byte[] storedHash = Base64.getDecoder().decode(prefs.getString(PREFS_PASSWORD_HASH, ""));
                    byte[] authSalt = Base64.getDecoder().decode(prefs.getString(PREFS_PASSWORD_SALT, ""));
                    byte[] hash = keyGenerator.generatePBKDF2Key(password, authSalt);
                    if (!Arrays.equals(hash, storedHash))
                        throw new SecurityException("Wrong password");
                })
//                .andThen(obtainRawMasterKey(passCopy2))
//                .flatMap(masterKey -> Single.fromCallable(() -> generateCredentials(password, masterKey)))
//                .flatMapCompletable(this::saveEncodedKey)
                .subscribeOn(Schedulers.io());
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
                .flatMapCompletable(credentials -> Completable.complete()
                                .concatWith(saveEncodedKeyToDb(credentials))
                                .concatWith(saveAuthInfo(credentials.authSalt, credentials.passwordHash))
                                .concatWith(putToKeystore(credentials.masterKey))
                                .doFinally(credentials::clear)
                )
                .doOnComplete(() -> Log.i(TAG, "User registered successfully"));
    }

    @Override
    public Completable changePassword(char[] password, char[] newPassword) {
        // TODO: rollback!
        return Completable.fromAction(() -> validatePassword(newPassword))
                .andThen(authorize(password))
                .doOnComplete(() -> Log.i(TAG, "Changing password started"))
                .andThen(obtainRawMasterKey(password))
                .flatMap(masterKey -> Single.fromCallable(() -> generateCredentials(newPassword, masterKey)))

                .flatMapCompletable(credentials -> Completable.complete()
                        .concatWith(saveEncodedKeyToDb(credentials))
                        .concatWith(saveAuthInfo(credentials.authSalt, credentials.passwordHash))
                        .doFinally(credentials::clear)
                )
                .doFinally(() -> Arrays.fill(password, '\0'))
                .doOnComplete(() -> Log.i(TAG, "Password changed successfully"))
                .doOnError(error -> Log.e(TAG, "Password changing failed", error))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Completable updatePassword(char[] password) {
        return obtainRawMasterKey(password)
                .flatMap(masterKey -> Single.fromCallable(() -> generateCredentials(password, masterKey)))
                .flatMapCompletable(credentials -> Completable.complete()
                        .concatWith(saveAuthInfo(credentials.authSalt, credentials.passwordHash))
                        .concatWith(putToKeystore(credentials.masterKey))
                        .doFinally(credentials::clear)

                )
                .doOnComplete(() -> Log.i(TAG, "Password changed successfully"))
                .doOnError(error -> Log.e(TAG, "Password changing failed", error));
    }

    @Override
    public Completable checkAccessToDatabase(char[] password, MasterKeyEntity entity) {
        return decryptMasterKey(password, entity.encryptedKey, entity.keySalt)
                .flatMapCompletable(masterKey -> {
                    Log.d(TAG, "checkAccessToDatabase");
                    SecretKey aes = new SecretKeySpec(masterKey, "AES");
                    CryptoHelper.decrypt(entity.validationText, aes);
                    return Completable.complete();
                });
    }

    private Single<byte[]> obtainRawMasterKey(char[] password) {
        return masterKeyRepository.getMasterKeySingle()
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .flatMap(mk -> decryptMasterKey(password, mk.encryptedKey, mk.keySalt));
    }

    private Single<byte[]> decryptMasterKey(char[] password, byte[] encryptedKey, byte[] salt) {
        return Single.fromCallable(() -> {
            byte[] keyFromPassword = keyGenerator.generatePBKDF2Key(password, salt);
            SecretKey aes = new SecretKeySpec(keyFromPassword, "AES");
            return CryptoHelper.decrypt(encryptedKey, aes);
        });
    }

    @Override
    public SecretKey getMasterKey() throws Exception {
        return loadFromKeystore();
    }

    private @NonNull Credentials generateCredentials(char[] password) throws Exception {
        byte[] masterKey = keyGenerator.generateMasterKey();
        return generateCredentials(password, masterKey);
    }



    private String generateRandomValidationString() {
        byte[] randomBytes = new byte[VALIDATION_STRING_LENGTH];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    private @NonNull Credentials generateCredentials(@NonNull char[] password, @NonNull byte[] masterKey) throws Exception {
        Log.d(TAG, "generateCredentials with existing masterKey");
        byte[] authSalt = keyGenerator.generateSalt();
        byte[] passwordHash = keyGenerator.generatePBKDF2Key(password, authSalt);
        byte[] keySalt = keyGenerator.generateSalt();

        byte[] keyFromPassword = keyGenerator.generatePBKDF2Key(password, keySalt);
        byte[] passwordEncryptedKey = CryptoHelper.encrypt(masterKey, new SecretKeySpec(keyFromPassword, "AES"));

        String validationText = CryptoHelper.encrypt(generateRandomValidationString(), new SecretKeySpec(masterKey, "AES"));

        return new Credentials(authSalt, passwordHash, keySalt, masterKey, passwordEncryptedKey, validationText);
    }

    private void validatePassword(@NotNull char[] password) {
        Log.d(TAG, "validatePassword");
        if (password.length < 4)
            throw new IllegalArgumentException("Password length must be at least 4 symbols");
    }

    private Completable saveAuthInfo(byte[] authSalt, byte[] passwordHash) {
        return Completable.fromAction(() -> {
                    SharedPreferences prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
                    prefs.edit()
                            .putString(PREFS_PASSWORD_HASH, Base64.getEncoder().encodeToString(passwordHash))
                            .putString(PREFS_PASSWORD_SALT, Base64.getEncoder().encodeToString(authSalt))
                            .apply();
                })
                .subscribeOn(Schedulers.io());
    }

    private Completable saveEncodedKeyToDb(Credentials credentials) {
        Log.d(TAG, "save EncodedKey to db");
        MasterKeyEntity masterKeyEntity = new MasterKeyEntity();
        masterKeyEntity.keySalt = credentials.keySalt;
        masterKeyEntity.encryptedKey = credentials.passwordEncryptedKey;
        masterKeyEntity.createdAt = System.currentTimeMillis();
        masterKeyEntity.validationText = credentials.validationText;
        return masterKeyRepository.updateMasterKey(masterKeyEntity);
    }

    private Completable putToKeystore(byte[] masterKey) {
        Log.d(TAG, "putToKeystore ...");
        SecretKey secretKey = new SecretKeySpec(masterKey, "AES");
        java.util.Arrays.fill(masterKey, (byte) 0);

        try {
            keystoreKeyManager.removeKey(alias);
        } catch (Exception ignored) {

        }
        try {
            keystoreKeyManager.addSecretKey(alias, secretKey);
            Log.d(TAG, "putToKeystore done");
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
        public final String validationText;

        public Credentials(byte[] authSalt, byte[] passwordHash, byte[] keySalt, byte[] masterKey, byte[] passwordEncryptedKey, String validationText) {
            this.authSalt = authSalt;
            this.passwordHash = passwordHash;
            this.keySalt = keySalt;
            this.masterKey = masterKey;
            this.passwordEncryptedKey = passwordEncryptedKey;
            this.validationText = validationText;
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