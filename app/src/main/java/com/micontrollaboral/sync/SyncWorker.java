package com.micontrollaboral.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.micontrollaboral.MiControlLaboralApplication;
import com.micontrollaboral.database.AppDatabase;
import com.micontrollaboral.database.EmploymentEntity;
import com.micontrollaboral.database.PaymentAllocationEntity;
import com.micontrollaboral.database.PaymentEntity;
import com.micontrollaboral.database.WorkSessionEntity;
import com.micontrollaboral.database.SyncRecordEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Result.failure();
        }

        try {
            AppDatabase database = ((MiControlLaboralApplication) getApplicationContext()).getDatabase(user.getUid());
            List<SyncRecordEntity> pendingRecords = database.syncRecordDao().getPending();
            if (pendingRecords.isEmpty()) {
                return Result.success();
            }
            String backupId = String.valueOf(System.currentTimeMillis());
            String root = "users/" + user.getUid() + "/backups/" + backupId;
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            WriteBatch batch = firestore.batch();
            for (EmploymentEntity value : database.employmentDao().getAll()) {
                batch.set(firestore.document(root + "/employments/" + value.id), employmentMap(value), SetOptions.merge());
            }
            for (WorkSessionEntity value : database.workSessionDao().getAll()) {
                batch.set(firestore.document(root + "/workSessions/" + value.id), workSessionMap(value), SetOptions.merge());
            }
            for (PaymentEntity value : database.paymentDao().getAll()) {
                batch.set(firestore.document(root + "/payments/" + value.id), paymentMap(value), SetOptions.merge());
            }
            for (PaymentAllocationEntity value : database.paymentAllocationDao().getAll()) {
                batch.set(firestore.document(root + "/paymentAllocations/" + value.id), allocationMap(value), SetOptions.merge());
            }
            Tasks.await(batch.commit());
            long syncedAt = System.currentTimeMillis();
            database.runInTransaction(() -> {
                for (SyncRecordEntity record : pendingRecords) {
                    database.syncRecordDao().updateState(record.id, "SYNCED", 0, null, syncedAt);
                }
            });
            return Result.success();
        } catch (Exception exception) {
            try {
                AppDatabase database = ((MiControlLaboralApplication) getApplicationContext()).getDatabase(user.getUid());
                long failedAt = System.currentTimeMillis();
                for (SyncRecordEntity record : database.syncRecordDao().getPending()) {
                    database.syncRecordDao().updateState(record.id, "FAILED", 1, exception.getMessage(), failedAt);
                }
            } catch (Exception ignored) {
            }
            return Result.retry();
        }
    }

    private Map<String, Object> employmentMap(EmploymentEntity value) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", value.id); map.put("name", value.name); map.put("fullRateCents", value.fullRateCents); map.put("usualDays", value.usualDays); map.put("active", value.active); map.put("createdAt", value.createdAt); map.put("updatedAt", value.updatedAt); map.put("deletedAt", value.deletedAt);
        return map;
    }

    private Map<String, Object> workSessionMap(WorkSessionEntity value) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", value.id); map.put("employmentId", value.employmentId); map.put("workedDate", value.workedDate); map.put("sessionType", value.sessionType); map.put("amountCents", value.amountCents); map.put("note", value.note); map.put("createdAt", value.createdAt); map.put("updatedAt", value.updatedAt); map.put("deletedAt", value.deletedAt);
        return map;
    }

    private Map<String, Object> paymentMap(PaymentEntity value) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", value.id); map.put("employmentId", value.employmentId); map.put("receivedAt", value.receivedAt); map.put("totalAmountCents", value.totalAmountCents); map.put("note", value.note); map.put("createdAt", value.createdAt); map.put("updatedAt", value.updatedAt); map.put("deletedAt", value.deletedAt);
        return map;
    }

    private Map<String, Object> allocationMap(PaymentAllocationEntity value) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", value.id); map.put("paymentId", value.paymentId); map.put("workSessionId", value.workSessionId); map.put("allocatedAmountCents", value.allocatedAmountCents);
        return map;
    }
}