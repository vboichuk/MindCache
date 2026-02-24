package com.myapp.mindcache.repositories;

import android.content.Context;
import android.util.Log;

import com.myapp.mindcache.dao.MasterKeyDao;
import com.myapp.mindcache.datastorage.AppDatabase;
import com.myapp.mindcache.model.MasterKeyEntity;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MasterKeyRepository {
    private static final String TAG = MasterKeyRepository.class.getSimpleName();
    private final MasterKeyDao masterKeyDao;

    public MasterKeyRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.masterKeyDao = db.masterKeyDao();

    }

    public Single<MasterKeyEntity> getMasterKeySingle() {
        return Single.fromCallable(() -> {
                    MasterKeyEntity entity = masterKeyDao.getMasterKeyDirect();
                    if (entity == null) {
                        throw new IllegalStateException("Master key not found");
                    }
                    return entity;
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error getting master key", e));
    }

    public Single<Boolean> exists() {
        return masterKeyDao.exists();
    }

    public Completable deleteMasterKey(long id) {
        return Completable.fromAction(() -> {
                    masterKeyDao.deleteAll();
                    Log.i(TAG, "MasterKey was deleted from from database");
                })
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, "Error in MasterKey", e));
    }

    public Completable updateMasterKey(MasterKeyEntity masterKey) {
        return Completable.fromAction(() -> {
            Log.d(TAG, "perform 'updateMasterKey'");
                    masterKey.createdAt = System.currentTimeMillis();
                    masterKeyDao.insert(masterKey);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

    }
}