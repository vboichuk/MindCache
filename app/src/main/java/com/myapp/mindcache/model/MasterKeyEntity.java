package com.myapp.mindcache.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "master_key")
public class MasterKeyEntity {

    @PrimaryKey
    public int id = 1;

    @ColumnInfo(name = "key_salt", typeAffinity = ColumnInfo.BLOB)
    public byte[] keySalt;

    @ColumnInfo(name = "encrypted_key", typeAffinity = ColumnInfo.BLOB)
    public byte[] encryptedKey;

    @ColumnInfo(name = "iterations")
    public int iterations = 100000;

    @ColumnInfo(name = "algorithm")
    public String algorithm = "PBKDF2WithHmacSHA256";

    @ColumnInfo(name = "validation_text")
    public String validationText = "";

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
