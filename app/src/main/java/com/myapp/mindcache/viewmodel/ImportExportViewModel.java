package com.myapp.mindcache.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.myapp.mindcache.datastorage.AppDatabase;
import com.myapp.mindcache.security.KeyManager;

import java.io.File;
import java.util.Arrays;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

public class ImportExportViewModel extends ViewModel {

    private static final String TAG = ImportExportViewModel.class.getSimpleName();
    private final Application application;
    private final KeyManager keyManager;

    public ImportExportViewModel(Application application, KeyManager keyManager) {
        this.application = application;
        this.keyManager = keyManager;
    }


    public Completable exportDb() {
        return Completable.fromAction(() -> AppDatabase.exportDatabase(application))
                .subscribeOn(Schedulers.io());
    }

    public Completable importDb(Uri uri, char[] password) {
        Log.d(TAG, "start import " + uri);
        File tmpFile = application.getDatabasePath("temp.db");
        return Completable.fromAction(() -> AppDatabase.createTemporaryFile(application, uri, tmpFile))
                .andThen(AppDatabase.readMasterKeyFromFile(application, tmpFile))
                .flatMapCompletable(entity -> keyManager.checkAccessToDatabase(password, entity))
                .andThen(Completable.fromAction(() -> AppDatabase.importDatabase(application, tmpFile)))
                .andThen(keyManager.updatePassword(password))
                .doFinally(() -> {
                    Arrays.fill(password, '\0');
                    if (tmpFile.exists()) {
                        boolean delete = tmpFile.delete();
                        Log.d(TAG, "delete tmpFile result: " + delete);
                    }
                })
                .subscribeOn(Schedulers.io());
    }
}
