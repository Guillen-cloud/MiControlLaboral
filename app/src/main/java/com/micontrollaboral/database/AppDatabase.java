package com.micontrollaboral.database;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {EmploymentEntity.class, WorkSessionEntity.class, PaymentEntity.class, PaymentAllocationEntity.class, SyncRecordEntity.class},
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract EmploymentDao employmentDao();

    public abstract WorkSessionDao workSessionDao();

    public abstract PaymentDao paymentDao();

    public abstract PaymentAllocationDao paymentAllocationDao();

    public abstract SyncRecordDao syncRecordDao();

    public static AppDatabase getInstance(Context context, String ownerUid) {
        if (ownerUid == null || ownerUid.trim().isEmpty()) {
            throw new IllegalArgumentException("Se requiere un uid autenticado para abrir la base local.");
        }
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    String safeUid = ownerUid.replaceAll("[^A-Za-z0-9_-]", "_");
                    String databaseName = "mi_control_laboral_" + safeUid + ".db";
                    migrateLegacyDatabaseIfNeeded(context, databaseName);
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            databaseName
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build();
                }
            }
        }
        return instance;
    }

    private static void migrateLegacyDatabaseIfNeeded(Context context, String databaseName) {
        File legacy = context.getDatabasePath("mi_control_laboral.db");
        File target = context.getDatabasePath(databaseName);
        if (!legacy.exists() || target.exists()) {
            return;
        }
        try {
            Files.copy(legacy.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            File legacyWal = new File(legacy.getPath() + "-wal");
            File targetWal = new File(target.getPath() + "-wal");
            if (legacyWal.exists()) Files.copy(legacyWal.toPath(), targetWal.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            File legacyShm = new File(legacy.getPath() + "-shm");
            File targetShm = new File(target.getPath() + "-shm");
            if (legacyShm.exists()) Files.copy(legacyShm.toPath(), targetShm.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            legacy.delete();
            legacyWal.delete();
            legacyShm.delete();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo migrar la base local existente.", exception);
        }
    }

    private static final androidx.room.migration.Migration MIGRATION_1_2 = new androidx.room.migration.Migration(1, 2) {
        @Override
        public void migrate(androidx.sqlite.db.SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS payment (id TEXT NOT NULL, employmentId TEXT NOT NULL, receivedAt TEXT NOT NULL, totalAmountCents INTEGER NOT NULL, note TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, deletedAt INTEGER, PRIMARY KEY(id), FOREIGN KEY(employmentId) REFERENCES employment(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_payment_employmentId ON payment (employmentId)");
            database.execSQL("CREATE TABLE IF NOT EXISTS payment_allocation (id TEXT NOT NULL, paymentId TEXT NOT NULL, workSessionId TEXT NOT NULL, allocatedAmountCents INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(paymentId) REFERENCES payment(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(workSessionId) REFERENCES work_session(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocation_paymentId ON payment_allocation (paymentId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocation_workSessionId ON payment_allocation (workSessionId)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payment_allocation_paymentId_workSessionId ON payment_allocation (paymentId, workSessionId)");
        }
    };

    private static final androidx.room.migration.Migration MIGRATION_2_3 = new androidx.room.migration.Migration(2, 3) {
        @Override
        public void migrate(androidx.sqlite.db.SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS sync_record (id TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, operation TEXT NOT NULL, state TEXT NOT NULL, updatedAt INTEGER NOT NULL, retryCount INTEGER NOT NULL, lastError TEXT, PRIMARY KEY(id))");
        }
    };
}