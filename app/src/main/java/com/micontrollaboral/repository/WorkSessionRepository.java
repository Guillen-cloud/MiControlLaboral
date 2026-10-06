package com.micontrollaboral.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;

import com.micontrollaboral.database.WorkSessionDao;
import com.micontrollaboral.database.WorkSessionEntity;
import com.micontrollaboral.database.SyncRecordDao;
import com.micontrollaboral.database.SyncRecordEntity;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkSessionRepository {
    public interface SaveCallback {
        void onSuccess();

        void onError(String message);
    }

    private final WorkSessionDao workSessionDao;
    private final SyncRecordDao syncRecordDao;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public WorkSessionRepository(WorkSessionDao workSessionDao, SyncRecordDao syncRecordDao) {
        this.workSessionDao = workSessionDao;
        this.syncRecordDao = syncRecordDao;
    }

    public LiveData<List<WorkSessionEntity>> observeActiveWorkSessions() {
        return workSessionDao.observeActiveWorkSessions();
    }

    public LiveData<Integer> observeGeneratedAmountCents() {
        return workSessionDao.observeGeneratedAmountCents();
    }

    public void register(String employmentId, String workedDate, String sessionType, int amountCents, String note, SaveCallback callback) {
        if (amountCents <= 0) {
            callback.onError("El importe debe ser mayor que cero.");
            return;
        }

        databaseExecutor.execute(() -> {
            WorkSessionEntity existing = workSessionDao.findByEmploymentAndDate(employmentId, workedDate);
            if (existing != null) {
                mainHandler.post(() -> callback.onError("Ya existe una jornada para ese empleo y fecha."));
                return;
            }

            long now = System.currentTimeMillis();
            WorkSessionEntity workSession = new WorkSessionEntity();
            workSession.id = UUID.randomUUID().toString();
            workSession.employmentId = employmentId;
            workSession.workedDate = workedDate;
            workSession.sessionType = sessionType;
            workSession.amountCents = amountCents;
            workSession.note = note;
            workSession.createdAt = now;
            workSession.updatedAt = now;
            workSessionDao.insert(workSession);
            markPending(workSession.id, "CREATE");
            mainHandler.post(callback::onSuccess);
        });
    }

    public void update(WorkSessionEntity workSession, SaveCallback callback) {
        if (workSession.amountCents <= 0) {
            callback.onError("El importe debe ser mayor que cero.");
            return;
        }

        databaseExecutor.execute(() -> {
            WorkSessionEntity existing = workSessionDao.findByEmploymentAndDate(workSession.employmentId, workSession.workedDate);
            if (existing != null && !existing.id.equals(workSession.id)) {
                mainHandler.post(() -> callback.onError("Ya existe otra jornada para ese empleo y fecha."));
                return;
            }
            workSession.updatedAt = System.currentTimeMillis();
            workSessionDao.update(workSession);
            markPending(workSession.id, "UPDATE");
            mainHandler.post(callback::onSuccess);
        });
    }

    private void markPending(String entityId, String operation) {
        SyncRecordEntity record = new SyncRecordEntity();
        record.id = "workSession:" + entityId;
        record.entityType = "workSession";
        record.entityId = entityId;
        record.operation = operation;
        record.state = "PENDING";
        record.updatedAt = System.currentTimeMillis();
        syncRecordDao.insertOrReplace(record);
    }
}