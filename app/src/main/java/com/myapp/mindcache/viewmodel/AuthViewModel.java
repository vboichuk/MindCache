package com.myapp.mindcache.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.myapp.mindcache.security.KeyManager;

import java.util.Arrays;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AuthViewModel extends AndroidViewModel {

    private static final String TAG = AuthViewModel.class.getSimpleName();

    private final KeyManager keyManager;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application application, @NonNull KeyManager keyManager) {
        super(application);
        this.keyManager = keyManager;
    }


    public MutableLiveData<AuthState> getAuthState() {
        return authState;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void checkRegistration() {
        isLoading.postValue(true);
        Disposable disposable = keyManager.isUserRegistered()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        isRegistered -> authState.postValue(isRegistered ? AuthState.LOGIN : AuthState.REGISTER),
                        error -> {
                            isLoading.postValue(false);
                            errorMessage.postValue(error.getMessage());
                            Log.e(TAG, "Error checking registration", error);
                        }
                );
        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }

    public void login(char[] password) {

        Disposable disposable = validatePasswordCompletable(password)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(s -> isLoading.postValue(true))
                .observeOn(AndroidSchedulers.mainThread())
                .andThen(keyManager.authorize(password))
                .doFinally(() -> Arrays.fill(password, '\0'))
                .subscribe(
                        () -> {
                            isLoading.postValue(false);
                            authState.postValue(AuthState.AUTHENTICATED);
                        },
                        error -> {
                            isLoading.postValue(false);
                            errorMessage.postValue("Login error: " + error.getMessage());
                            Log.e(TAG, "Login error", error);
                        }
                );
        disposables.add(disposable);
    }

    public void register(char[] password) {
        if (password == null || password.length == 0) {
            Log.e(TAG, "Password cannot be null or empty");
            errorMessage.postValue("Password cannot be null or empty");
            return;
        }

        isLoading.postValue(true);

        Disposable disposable = keyManager.registerUser(password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            isLoading.postValue(false);
                            authState.postValue(AuthState.AUTHENTICATED);
                        },
                        error -> {
                            isLoading.postValue(false);
                            errorMessage.postValue("Registration error: " + error.getMessage());
                            Log.e(TAG, "Registration error", error);
                        }
                );
        disposables.add(disposable);
    }

    public Completable changePassword(char[] oldPassword, char[] newPassword) {

        return validatePasswordCompletable(newPassword)
                .andThen(keyManager.changePassword(oldPassword, newPassword));
    }

    protected Completable validatePasswordCompletable(char[] password) {
        if (password == null || password.length == 0) {
            return Completable.error(new IllegalArgumentException("Password cannot be null or empty"));
        }
        if (password.length < 4) {
            return Completable.error(new IllegalArgumentException("Password must be at least 4 characters"));
        }
        return Completable.complete();
    }

    public enum AuthState {
        REGISTER,
        LOGIN,
        AUTHENTICATED
    }
}
