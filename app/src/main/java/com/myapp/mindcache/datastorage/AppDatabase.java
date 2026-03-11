package com.myapp.mindcache.datastorage;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.myapp.mindcache.dao.MasterKeyDao;
import com.myapp.mindcache.dao.NoteDao;
import com.myapp.mindcache.model.EncryptedNote;
import com.myapp.mindcache.model.MasterKeyEntity;

import java.util.concurrent.Executors;

@Database(
        entities = { EncryptedNote.class, MasterKeyEntity.class },
        version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    private static ExportManager exportManager;

    private static final String TAG = AppDatabase.class.getSimpleName();
    private static final Object LOCK = new Object();

    static final String DB_NAME = "secure_notes_db";


    public static void importDatabase(Context context, Uri uri) {
        synchronized (LOCK) {
            closeDatabase();
            resetInstance();
            initExportManager(context);
            exportManager.importDatabase(uri);
            getInstance(context);
        }
    }

    public static void exportDatabase(Context context) {
        synchronized (LOCK) {
            closeDatabase();
            initExportManager(context);
            exportManager.exportDatabase();
            getInstance(context);
        }
    }


    public abstract NoteDao noteDao();

    public abstract MasterKeyDao masterKeyDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = buildDatabase(context);
                    initExportManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private static void initExportManager(Context context) {
        if (exportManager == null) {
            exportManager = new ExportManagerImpl(context);
        }
    }

    private static AppDatabase buildDatabase(Context appContext) {
        return Room.databaseBuilder(appContext, AppDatabase.class, DB_NAME)
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        // Действия при первом создании БД
                    }
                })
                // .addMigrations(MIGRATION_1_2)
                // .addMigrations(MIGRATION_2_3)
                // .addMigrations(MIGRATION_3_4)
                .setJournalMode(JournalMode.TRUNCATE) // Оптимизация для записи
                // .fallbackToDestructiveMigration() // Только для разработки!
                .setQueryCallback((sqlQuery, bindArgs) ->
                                // Log.d("ROOM_QUERY", "SQL: " + sqlQuery),
                                {},
                                Executors.newSingleThreadExecutor())
                .build();

        /*
        В production коде:
        Убрать деструктивную миграцию
            .fallbackToDestructiveMigration()
        Заменить на:
            .fallbackToDestructiveMigrationOnDowngrade()
        */
    }


    static void resetInstance() {
        // Сбрасываем INSTANCE
        synchronized (AppDatabase.class) {
            if (INSTANCE != null) {
                INSTANCE.close();
                INSTANCE = null;
            }
        }
    }

    static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
        }
    }
}