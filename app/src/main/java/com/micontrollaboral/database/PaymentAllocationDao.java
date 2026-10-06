package com.micontrollaboral.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.micontrollaboral.domain.EmploymentReportRow;

import java.util.List;

@Dao
public interface PaymentAllocationDao {
    @Query("SELECT COALESCE(SUM(allocatedAmountCents), 0) FROM payment_allocation WHERE workSessionId = :workSessionId")
    int sumAllocatedForWorkSession(String workSessionId);

    @Query("SELECT COALESCE(SUM(allocatedAmountCents), 0) FROM payment_allocation")
    LiveData<Integer> observeAllocatedAmountCents();

    @Query("SELECT COALESCE(SUM(pa.allocatedAmountCents), 0) FROM payment_allocation pa INNER JOIN work_session ws ON ws.id = pa.workSessionId WHERE ws.workedDate BETWEEN :startDate AND :endDate AND ws.deletedAt IS NULL")
    int sumAllocatedForWorkedPeriod(String startDate, String endDate);

    @Query("SELECT e.id AS employmentId, e.name AS employmentName, 0 AS generatedAmountCents, COALESCE(SUM(pa.allocatedAmountCents), 0) AS allocatedAmountCents FROM payment_allocation pa INNER JOIN work_session ws ON ws.id = pa.workSessionId INNER JOIN employment e ON e.id = ws.employmentId WHERE ws.workedDate BETWEEN :startDate AND :endDate AND ws.deletedAt IS NULL GROUP BY e.id, e.name ORDER BY e.name")
    List<EmploymentReportRow> sumAllocatedByEmploymentBetween(String startDate, String endDate);

    @Insert
    void insert(PaymentAllocationEntity allocation);

    @Insert
    void insertAll(List<PaymentAllocationEntity> allocations);

    @Query("SELECT * FROM payment_allocation")
    List<PaymentAllocationEntity> getAll();

    @Query("DELETE FROM payment_allocation")
    void deleteAll();
}