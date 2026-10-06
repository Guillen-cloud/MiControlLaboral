package com.micontrollaboral.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.micontrollaboral.database.AppDatabase;
import com.micontrollaboral.database.PaymentAllocationEntity;
import com.micontrollaboral.database.PaymentEntity;
import com.micontrollaboral.database.WorkSessionDao;
import com.micontrollaboral.database.WorkSessionEntity;
import com.micontrollaboral.database.SyncRecordDao;
import com.micontrollaboral.database.SyncRecordEntity;

import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentRepository {
    public static class AllocationInput {
        public final String workSessionId;
        public final int amountCents;

        public AllocationInput(String workSessionId, int amountCents) {
            this.workSessionId = workSessionId;
            this.amountCents = amountCents;
        }
    }

    public interface SaveCallback {
        void onSuccess();

        void onError(String message);
    }

    private final AppDatabase database;
    private final WorkSessionDao workSessionDao;
    private final SyncRecordDao syncRecordDao;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PaymentRepository(AppDatabase database, SyncRecordDao syncRecordDao) {
        this.database = database;
        workSessionDao = database.workSessionDao();
        this.syncRecordDao = syncRecordDao;
    }

    public LiveData<Integer> observeAllocatedAmountCents() {
        return database.paymentAllocationDao().observeAllocatedAmountCents();
    }

    public void register(String workSessionId, String receivedAt, int amountCents, String note, SaveCallback callback) {
        if (amountCents <= 0) {
            callback.onError("El cobro debe ser mayor que cero.");
            return;
        }

        databaseExecutor.execute(() -> {
            WorkSessionEntity workSession = workSessionDao.findById(workSessionId);
            if (workSession == null) {
                postError(callback, "La jornada seleccionada ya no existe.");
                return;
            }
            int alreadyAllocated = database.paymentAllocationDao().sumAllocatedForWorkSession(workSessionId);
            if (alreadyAllocated + amountCents > workSession.amountCents) {
                postError(callback, "El cobro supera el saldo pendiente de la jornada.");
                return;
            }

            long now = System.currentTimeMillis();
            PaymentEntity payment = new PaymentEntity();
            payment.id = UUID.randomUUID().toString();
            payment.employmentId = workSession.employmentId;
            payment.receivedAt = receivedAt;
            payment.totalAmountCents = amountCents;
            payment.note = note;
            payment.createdAt = now;
            payment.updatedAt = now;

            PaymentAllocationEntity allocation = new PaymentAllocationEntity();
            allocation.id = UUID.randomUUID().toString();
            allocation.paymentId = payment.id;
            allocation.workSessionId = workSessionId;
            allocation.allocatedAmountCents = amountCents;

            database.runInTransaction(() -> {
                database.paymentDao().insert(payment);
                database.paymentAllocationDao().insert(allocation);
                markPending(payment.id, "payment", "CREATE");
                markPending(allocation.id, "paymentAllocation", "CREATE");
            });
            mainHandler.post(callback::onSuccess);
        });
    }

    public void registerGrouped(String receivedAt, String note, List<AllocationInput> inputs, SaveCallback callback) {
        if (inputs == null || inputs.isEmpty()) {
            callback.onError("Selecciona al menos una jornada y asigna un importe.");
            return;
        }

        databaseExecutor.execute(() -> {
            Set<String> sessionIds = new HashSet<>();
            List<WorkSessionEntity> sessions = new ArrayList<>();
            String employmentId = null;
            int totalAmountCents = 0;
            for (AllocationInput input : inputs) {
                if (input.amountCents <= 0 || !sessionIds.add(input.workSessionId)) {
                    postError(callback, "Cada asignación debe tener un importe positivo y una jornada única.");
                    return;
                }
                WorkSessionEntity session = workSessionDao.findById(input.workSessionId);
                if (session == null) {
                    postError(callback, "Una de las jornadas seleccionadas ya no existe.");
                    return;
                }
                if (employmentId == null) {
                    employmentId = session.employmentId;
                } else if (!employmentId.equals(session.employmentId)) {
                    postError(callback, "Un pago agrupado solo puede pertenecer a un empleo.");
                    return;
                }
                int alreadyAllocated = database.paymentAllocationDao().sumAllocatedForWorkSession(session.id);
                if (alreadyAllocated + input.amountCents > session.amountCents) {
                    postError(callback, "Una asignación supera el saldo pendiente de su jornada.");
                    return;
                }
                sessions.add(session);
                totalAmountCents += input.amountCents;
            }

            long now = System.currentTimeMillis();
            PaymentEntity payment = new PaymentEntity();
            payment.id = UUID.randomUUID().toString();
            payment.employmentId = employmentId;
            payment.receivedAt = receivedAt;
            payment.totalAmountCents = totalAmountCents;
            payment.note = note;
            payment.createdAt = now;
            payment.updatedAt = now;
            List<PaymentAllocationEntity> allocations = new ArrayList<>();
            for (int index = 0; index < inputs.size(); index++) {
                PaymentAllocationEntity allocation = new PaymentAllocationEntity();
                allocation.id = UUID.randomUUID().toString();
                allocation.paymentId = payment.id;
                allocation.workSessionId = sessions.get(index).id;
                allocation.allocatedAmountCents = inputs.get(index).amountCents;
                allocations.add(allocation);
            }
            database.runInTransaction(() -> {
                database.paymentDao().insert(payment);
                for (PaymentAllocationEntity allocation : allocations) {
                    database.paymentAllocationDao().insert(allocation);
                    markPending(allocation.id, "paymentAllocation", "CREATE");
                }
                markPending(payment.id, "payment", "CREATE");
            });
            mainHandler.post(callback::onSuccess);
        });
    }

    private void postError(SaveCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private void markPending(String entityId, String entityType, String operation) {
        SyncRecordEntity record = new SyncRecordEntity();
        record.id = entityType + ":" + entityId;
        record.entityType = entityType;
        record.entityId = entityId;
        record.operation = operation;
        record.state = "PENDING";
        record.updatedAt = System.currentTimeMillis();
        syncRecordDao.insertOrReplace(record);
    }
}