package com.myapp.mindcache.viewmodel;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.ViewModel;

import com.myapp.mindcache.datastorage.AppDatabase;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

public class ImportExportViewModel extends ViewModel {

    public Completable exportDb(Context context) {
        return Completable.fromAction(() -> AppDatabase.exportDatabase(context))
                .subscribeOn(Schedulers.io());
    }

    public Completable importDb(Context context, Uri uri) {
        return Completable.fromAction(() -> AppDatabase.importDatabase(context, uri))
                .subscribeOn(Schedulers.io());
    }
}
