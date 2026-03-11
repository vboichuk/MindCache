package com.myapp.mindcache.datastorage;

import android.net.Uri;

import androidx.annotation.NonNull;

public interface ExportManager {
    void exportDatabase();
    void importDatabase(@NonNull Uri sourceUri);
}
