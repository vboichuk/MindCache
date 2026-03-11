package com.myapp.mindcache.datastorage;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;

public interface ExportManager {
    void exportDatabase(Context context);
    void importDatabase(@NonNull Context context, @NonNull Uri sourceUri);
}
