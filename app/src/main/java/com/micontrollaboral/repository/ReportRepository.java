package com.micontrollaboral.repository;

import android.os.Handler;
import android.os.Looper;

import com.micontrollaboral.database.AppDatabase;
import com.micontrollaboral.domain.ReportSummary;
import com.micontrollaboral.domain.EmploymentReportRow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportRepository {
    public interface ReportCallback {
        void onSuccess(ReportSummary summary);

        void onError(String message);
    }

    private final AppDatabase database;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ReportRepository(AppDatabase database) {
        this.database = database;
    }

    public void generate(String startDate, String endDate, ReportCallback callback) {
        if (startDate == null || endDate == null || startDate.isEmpty() || endDate.isEmpty() || startDate.compareTo(endDate) > 0) {
            callback.onError("El rango de fechas no es válido.");
            return;
        }
        databaseExecutor.execute(() -> {
            int generated = database.workSessionDao().sumGeneratedBetween(startDate, endDate);
            int allocated = database.paymentAllocationDao().sumAllocatedForWorkedPeriod(startDate, endDate);
            int received = database.paymentDao().sumReceivedBetween(startDate, endDate);
            Map<String, EmploymentReportRow> rowsByEmployment = new LinkedHashMap<>();
            for (EmploymentReportRow row : database.workSessionDao().sumGeneratedByEmploymentBetween(startDate, endDate)) {
                rowsByEmployment.put(row.employmentId, row);
            }
            for (EmploymentReportRow row : database.paymentAllocationDao().sumAllocatedByEmploymentBetween(startDate, endDate)) {
                EmploymentReportRow existing = rowsByEmployment.get(row.employmentId);
                if (existing == null) {
                    rowsByEmployment.put(row.employmentId, row);
                } else {
                    existing.allocatedAmountCents = row.allocatedAmountCents;
                }
            }
            ReportSummary summary = new ReportSummary(startDate, endDate, generated, allocated, received, new ArrayList<>(rowsByEmployment.values()));
            mainHandler.post(() -> callback.onSuccess(summary));
        });
    }
}