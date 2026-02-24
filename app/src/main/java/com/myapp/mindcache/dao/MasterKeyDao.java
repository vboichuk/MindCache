package com.myapp.mindcache.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.myapp.mindcache.model.MasterKeyEntity;

import io.reactivex.Single;

@Dao
public interface MasterKeyDao {
    @Query("SELECT * FROM master_key WHERE id = 1")
    LiveData<MasterKeyEntity> getMasterKey();

    @Query("SELECT * FROM master_key WHERE id = 1")
    MasterKeyEntity getMasterKeyDirect();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MasterKeyEntity masterKey);

    @Query("DELETE FROM master_key")
    void deleteAll();

    @Query("SELECT EXISTS (SELECT 1 FROM master_key WHERE id = 1 LIMIT 1)")
    Single<Boolean> exists();
}
