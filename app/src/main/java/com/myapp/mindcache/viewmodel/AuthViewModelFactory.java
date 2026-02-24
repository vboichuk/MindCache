package com.myapp.mindcache.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.mindcache.security.KeyManager;

public class AuthViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final KeyManager keyManager;

    public AuthViewModelFactory(Application application, KeyManager keyManager) {
        this.application = application;
        this.keyManager = keyManager;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            return (T) new AuthViewModel(application, keyManager);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
