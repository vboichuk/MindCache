package com.myapp.mindcache.security;

import com.myapp.mindcache.exception.AuthError;
import com.myapp.mindcache.model.MasterKeyEntity;

import javax.crypto.SecretKey;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface KeyManager {

    Single<Boolean> isUserRegistered();

    Completable registerUser(char[] password);

    Completable authorize(char[] password);

    SecretKey getMasterKey() throws Exception, AuthError;

    Completable changePassword(char[] oldPassword, char[] newPassword);

    Completable updatePassword(char[] password);

    Completable checkAccessToDatabase(char[] password, MasterKeyEntity masterKey);
}
