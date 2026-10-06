package com.micontrollaboral.database;

import android.content.Context;

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

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "mi_control_laboral.db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build();
                }
            }
        }
        return instance;
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