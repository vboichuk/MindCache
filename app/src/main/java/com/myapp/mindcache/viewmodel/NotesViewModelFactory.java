package com.myapp.mindcache.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.mindcache.repositories.NoteRepository;
import com.myapp.mindcache.security.KeyManager;

public class NotesViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;
    private final KeyManager keyManager;
    private final NoteRepository noteRepository;

    public NotesViewModelFactory(Application application, KeyManager keyManager, NoteRepository noteRepository) {
        this.application = application;
        this.keyManager = keyManager;
        this.noteRepository = noteRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new NotesViewModel(application, keyManager, noteRepository);
    }
}