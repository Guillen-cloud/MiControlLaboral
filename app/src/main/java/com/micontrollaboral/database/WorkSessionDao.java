package com.micontrollaboral.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.micontrollaboral.domain.EmploymentReportRow;

import java.util.List;

@Dao
public interface WorkSessionDao {
    @Query("SELECT * FROM work_session WHERE deletedAt IS NULL ORDER BY workedDate DESC")
    LiveData<List<WorkSessionEntity>> observeActiveWorkSessions();

    @Query("SELECT * FROM work_session WHERE employmentId = :employmentId AND workedDate = :workedDate AND deletedAt IS NULL LIMIT 1")
    WorkSessionEntity findByEmploymentAndDate(String employmentId, String workedDate);

    @Query("SELECT * FROM work_session WHERE id = :workSessionId AND deletedAt IS NULL LIMIT 1")
    WorkSessionEntity findById(String workSessionId);

    @Query("SELECT * FROM work_session")
    List<WorkSessionEntity> getAll();

    @Query("DELETE FROM work_session")
    void deleteAll();

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM work_session WHERE deletedAt IS NULL")
    LiveData<Integer> observeGeneratedAmountCents();

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM work_session WHERE workedDate BETWEEN :startDate AND :endDate AND deletedAt IS NULL")
    int sumGeneratedBetween(String startDate, String endDate);

    @Query("SELECT e.id AS employmentId, e.name AS employmentName, COALESCE(SUM(ws.amountCents), 0) AS generatedAmountCents, 0 AS allocatedAmountCents FROM work_session ws INNER JOIN employment e ON e.id = ws.employmentId WHERE ws.workedDate BETWEEN :startDate AND :endDate AND ws.deletedAt IS NULL GROUP BY e.id, e.name ORDER BY e.name")
    List<EmploymentReportRow> sumGeneratedByEmploymentBetween(String startDate, String endDate);

    @Insert
    void insert(WorkSessionEntity workSession);

    @Insert
    void insertAll(List<WorkSessionEntity> workSessions);

    @Update
    void update(WorkSessionEntity workSession);
}