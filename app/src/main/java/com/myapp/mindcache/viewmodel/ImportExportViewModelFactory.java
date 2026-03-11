package com.myapp.mindcache.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.mindcache.security.KeyManager;

public class ImportExportViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final KeyManager keyManager;

    public ImportExportViewModelFactory(Application application, KeyManager keyManager) {
        this.application = application;
        this.keyManager = keyManager;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ImportExportViewModel(application, keyManager);
    }
}
