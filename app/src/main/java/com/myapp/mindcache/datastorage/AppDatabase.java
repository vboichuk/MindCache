package com.myapp.mindcache.datastorage;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.myapp.mindcache.dao.MasterKeyDao;
import com.myapp.mindcache.dao.NoteDao;
import com.myapp.mindcache.model.EncryptedNote;
import com.myapp.mindcache.model.MasterKeyEntity;

import java.io.File;
import java.io.IOException;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

@Database(
        entities = { EncryptedNote.class, MasterKeyEntity.class },
        version = 2)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    private static ExportManager exportManager;

    private static final String TAG = AppDatabase.class.getSimpleName();
    private static final Object LOCK = new Object();

    static final String DB_NAME = "secure_notes_db";

    public abstract NoteDao noteDao();

    public abstract MasterKeyDao masterKeyDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Log.i(TAG, "create db instance");
                    INSTANCE = buildDatabase(context, DB_NAME);
                    initExportManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public static void createTemporaryFile(Application application, Uri uri, File file) throws IOException {
        ExportManagerImpl.copyToTemporary(application, uri, file);
    }

    public static Single<MasterKeyEntity> readMasterKeyFromFile(Context context, File dbFile) {
        return Single.using(
                () -> buildDatabase(context, dbFile.getAbsolutePath()),     // Resource factory
                tempDb -> tempDb.masterKeyDao().getMasterKeySingle(),       // Observable factory
                tempDb -> {                                               // Disposer
                    if (tempDb != null && tempDb.isOpen()) {
                        tempDb.close();
                        Log.d(TAG, "Temporary database closed");
                    }
                }
        ).subscribeOn(Schedulers.io());
    }

    public static String exportDatabase(Context context) throws IOException {
        synchronized (LOCK) {
            Log.d(TAG, "start export");
            initExportManager(context);
            String path = exportManager.exportDatabase();
            Log.d(TAG, "export done");
            return path;
        }
    }

    public static void importDatabase(Application context, File file) {
        synchronized (LOCK) {
            resetInstance();
            initExportManager(context);
            exportManager.replaceDatabase(file);
            getInstance(context);
        }
    }

    private static void resetInstance() {
        synchronized (AppDatabase.class) {
            if (INSTANCE != null) {
                Log.i(TAG, "reset db instance");
                INSTANCE.close();
                INSTANCE = null;
            }
        }
    }

    private static void initExportManager(Context context) {
        if (exportManager == null) {
            exportManager = new ExportManagerImpl(context);
        }
    }

    private static AppDatabase buildDatabase(Context appContext, String dbName) {
        Log.i(TAG, "buildDatabase");
        return Room.databaseBuilder(appContext, AppDatabase.class, dbName)
                .addMigrations(MIGRATION_1_2)
                .setJournalMode(JournalMode.TRUNCATE)
                // .fallbackToDestructiveMigration() // Только для разработки!
                .build();

        /*
        В production коде:
        Убрать деструктивную миграцию
            .fallbackToDestructiveMigration()
        Заменить на:
            .fallbackToDestructiveMigrationOnDowngrade()
        */
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.d(TAG, "MIGRATION " + startVersion + " -> " + endVersion);
            database.execSQL("ALTER TABLE master_key ADD COLUMN validation_text TEXT DEFAULT '';");
        }
    };

}