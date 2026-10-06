package com.micontrollaboral.repository;

import androidx.lifecycle.LiveData;

import com.micontrollaboral.database.EmploymentDao;
import com.micontrollaboral.database.EmploymentEntity;
import com.micontrollaboral.database.SyncRecordDao;
import com.micontrollaboral.database.SyncRecordEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmploymentRepository {
    private final EmploymentDao employmentDao;
    private final SyncRecordDao syncRecordDao;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    public EmploymentRepository(EmploymentDao employmentDao, SyncRecordDao syncRecordDao) {
        this.employmentDao = employmentDao;
        this.syncRecordDao = syncRecordDao;
    }

    public LiveData<List<EmploymentEntity>> observeActiveEmployments() {
        return employmentDao.observeActiveEmployments();
    }

    public void ensureDefaultEmployments() {
        databaseExecutor.execute(() -> {
            if (employmentDao.countActiveEmployments() > 0) {
                return;
            }

            long now = System.currentTimeMillis();
            EmploymentEntity santillana = createDefault("Santillana", 10000, "LUNES,MARTES,MIERCOLES,JUEVES,VIERNES", now);
            EmploymentEntity campo = createDefault("Trabajo en el campo", 12000, "SABADO", now);
            employmentDao.insertAll(List.of(santillana, campo));
            markPending(santillana.id, "CREATE");
            markPending(campo.id, "CREATE");
        });
    }

    private void markPending(String entityId, String operation) {
        SyncRecordEntity record = new SyncRecordEntity();
        record.id = "employment:" + entityId;
        record.entityType = "employment";
        record.entityId = entityId;
        record.operation = operation;
        record.state = "PENDING";
        record.updatedAt = System.currentTimeMillis();
        syncRecordDao.insertOrReplace(record);
    }

    private EmploymentEntity createDefault(String name, int fullRateCents, String usualDays, long now) {
        EmploymentEntity employment = new EmploymentEntity();
        employment.id = UUID.randomUUID().toString();
        employment.name = name;
        employment.fullRateCents = fullRateCents;
        employment.usualDays = usualDays;
        employment.active = true;
        employment.createdAt = now;
        employment.updatedAt = now;
        return employment;
    }
}