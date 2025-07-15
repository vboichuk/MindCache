package com.myapp.mindcache.datastorage;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.mindcache.security.PasswordManager;

public class DiaryViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;
    private final PasswordManager passwordManager;

    public DiaryViewModelFactory(Application repository, PasswordManager secondArg) {
        this.application = repository;
        this.passwordManager = secondArg;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new DiaryViewModel(application, passwordManager);
    }
}