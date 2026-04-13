package com.myapp.mindcache.datastorage;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

public interface ExportManager {
    String exportDatabase() throws IOException;
    void replaceDatabase(@NonNull File sourceUri);
}
