package com.myapp.mindcache.datastorage;

import androidx.annotation.NonNull;

import java.io.File;

public interface ExportManager {
    void exportDatabase();
    void replaceDatabase(@NonNull File sourceUri);
}
