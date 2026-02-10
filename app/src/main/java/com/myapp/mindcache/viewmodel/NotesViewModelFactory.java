package com.myapp.mindcache.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.mindcache.security.PasswordManager;

public class NotesViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;
    private final PasswordManager passwordManager;

    public NotesViewModelFactory(Application application, PasswordManager passwordManager) {
        this.application = application;
        this.passwordManager = passwordManager;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new NotesViewModel(application, passwordManager);
    }
}