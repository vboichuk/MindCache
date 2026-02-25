package com.myapp.mindcache.security;

import com.myapp.mindcache.exception.AuthError;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface KeyManager {

    Single<Boolean> isUserRegistered();

    Completable registerUser(char[] password);

    Completable login(char[] password);

    void logout();

    byte[] getMasterKey() throws Exception, AuthError;

    Completable changePassword(char[] oldPassword, char[] newPassword);
}
