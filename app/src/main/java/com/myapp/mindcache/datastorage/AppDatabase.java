package com.myapp.mindcache.datastorage;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.myapp.mindcache.dao.MasterKeyDao;
import com.myapp.mindcache.dao.NoteDao;
import com.myapp.mindcache.model.MasterKeyEntity;
import com.myapp.mindcache.model.Note;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.Executors;

@Database(
        entities = { Note.class, MasterKeyEntity.class },
        version = 4)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    private static final String TAG = AppDatabase.class.getSimpleName();
    private static final String DB_NAME = "secure_notes_db";

    public abstract NoteDao noteDao();
    public abstract MasterKeyDao masterKeyDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = buildDatabase(context);
                }
            }
        }
        return INSTANCE;
    }

    private static AppDatabase buildDatabase(Context appContext) {
        return Room.databaseBuilder(
                        appContext,
                        AppDatabase.class,
                        DB_NAME
                )
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        // Действия при первом создании БД
                    }
                })
                .addMigrations(MIGRATION_1_2) // Добавляем все миграции
                .addMigrations(MIGRATION_2_3) // Добавляем все миграции
                .addMigrations(MIGRATION_3_4) // Добавляем все миграции
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

    // Миграции (пример для будущих версий)
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Реализация миграции при необходимости
            database.execSQL("ALTER TABLE notes ADD COLUMN preview TEXT DEFAULT ''");
        }
    };

    // Миграции (пример для будущих версий)
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Реализация миграции при необходимости
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `master_key` (" +
                            "`id` INTEGER PRIMARY KEY NOT NULL UNIQUE CHECK (id = 1) , " +  // CHECK гарантирует id = 1
                            "`key_salt` BLOB, " +
                            "`encrypted_key` BLOB, " +
                            "`iterations` INTEGER NOT NULL DEFAULT 100000, " +
                            "`algorithm` TEXT DEFAULT 'PBKDF2WithHmacSHA256', " +
                            "`created_at` INTEGER NOT NULL" +
                            ")"
            );
        }
    };

    // Миграции (пример для будущих версий)
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Реализация миграции при необходимости
            database.execSQL("ALTER TABLE notes DROP COLUMN salt");
        }
    };


    public static boolean exportDatabase(Context context) {
        File databaseFile = context.getDatabasePath(DB_NAME);
        File exportDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "MindCache_Backup");

        if (!exportDir.exists() && !exportDir.mkdirs()) {
            Log.e(TAG, "Не удалось создать директорию для экспорта");
            return false;
        }

        File exportFile = new File(exportDir, "secure_notes_backup.db");

        try {
            copyFile(databaseFile, exportFile);
            Log.i(TAG, "База данных успешно экспортирована: " + exportFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Ошибка экспорта базы данных", e);
            return false;
        }
    }

    private static void copyFile(File source, File dest) throws IOException {
        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }
}