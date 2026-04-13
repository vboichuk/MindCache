package com.myapp.mindcache.datastorage;

import static com.myapp.mindcache.datastorage.AppDatabase.DB_NAME;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.myapp.mindcache.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ExportManagerImpl implements ExportManager {

    private static final String TAG = ExportManager.class.getSimpleName();
    private static final String BACKUP_FILE_NAME = "MindCache_backup";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private final Context context;

    ExportManagerImpl(Context context) {
        this.context = context;
    }

    @Override
    public String exportDatabase() throws IOException {
        File databaseFile = context.getDatabasePath(DB_NAME);
        String filename = obtainFilenameForExport();
        String path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10+
            path = exportViaMediaStore(context, databaseFile, filename);
        } else {
            // Use directory access for Android 9-
            path = exportDirect(context, databaseFile, filename);
        }
        Log.i(TAG, "Exported to: " + path);
        return path;
    }

    @Override
    public void replaceDatabase(@NonNull File source) {
        File dest = context.getDatabasePath(DB_NAME);
        if (source.renameTo(dest))
            Log.i(TAG, "MOVED " + source.getName() + " => " + dest.getName());
        else {
            try {
                copyFile(source, dest);
                tryToRemoveFile(source);
                Log.i(TAG, "File imported successfully: " + source.getPath());
            } catch (IOException e) {
                Log.e(TAG, "Import failed: " + e.getMessage(), e);
            }
        }
    }

    public static void copyToTemporary(@NonNull Context context, @NonNull Uri sourceUri, @NonNull File file) throws IOException {
        Log.d(TAG, "copyToTemporary " + sourceUri.getPath() + " -> " + file.getAbsolutePath());
        try (
                InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                FileOutputStream outputStream = new FileOutputStream(file)
        ) {
            if (inputStream == null) {
                Log.e(TAG, "Не удалось открыть InputStream из Uri");
                return;
            }
            copyData(inputStream, outputStream);
            Log.i(TAG, "The file was successfully imported to " + file.getAbsolutePath());
        }
    }

    private static @NonNull String obtainFilenameForExport() {
        LocalDateTime dateTime = LocalDateTime.now();
        return BACKUP_FILE_NAME + "-" + DATE_TIME_FORMATTER.format(dateTime) + ".db";
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static String exportViaMediaStore(Context context, File databaseFile, String filename) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS
                + "/" + context.getString(R.string.app_name));

        Uri collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri destinationUri = context.getContentResolver().insert(collection, values);

        if (destinationUri == null) {
            return "";
        }

        try (
                FileInputStream fileInputStream = new FileInputStream(databaseFile);
                OutputStream outputStream = context.getContentResolver().openOutputStream(destinationUri)
        ) {
            copyData(fileInputStream, outputStream);

            // Сообщаем пользователю понятный путь
            return Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                    + "/" + context.getString(R.string.app_name) + "/" + filename;

        } catch (IOException e) {
            tryToRemoveFile(context, destinationUri);
            throw e;
        }
    }

    private static String exportDirect(Context context, File databaseFile, String filename) throws IOException {
        File exportDir = new File(Environment.getExternalStorageDirectory(),
                context.getString(R.string.app_name));

        if (!exportDir.exists() && !exportDir.mkdirs()) {
            Log.e(TAG, "Failed to create export directory");
            throw new IOException("Failed to create export directory");
        }

        File exportFile = new File(exportDir, filename);
        copyFile(databaseFile, exportFile);
        return exportFile.getAbsolutePath();
    }

    private static void copyFile(File source, File dest) throws IOException {
        try (
                FileInputStream fileInputStream = new FileInputStream(source);
                OutputStream outputStream = Files.newOutputStream(dest.toPath())
        ) {
            copyData(fileInputStream, outputStream);
        } catch (IOException e) {
            Log.e(TAG, "Copying failed: " + e.getMessage());
            tryToRemoveFile(dest);
            throw e;
        }
    }

    private static void copyData(InputStream inputStream,
                                 OutputStream outputStream) throws IOException {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(outputStream);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
    }

    private static void tryToRemoveFile(Context context, Uri fileUri) {
        try {
            Log.d(TAG, "try to remove created file...");
            context.getContentResolver().delete(fileUri, null, null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private static void tryToRemoveFile(File dest) {
        try {
            //noinspection ResultOfMethodCallIgnored
            dest.delete();
        } catch (SecurityException ignored) {
        }
    }
}
