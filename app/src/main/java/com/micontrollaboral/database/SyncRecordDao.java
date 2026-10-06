package com.micontrollaboral.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SyncRecordDao {
    @Insert
    void insertOrReplace(SyncRecordEntity record);

    @Query("SELECT COUNT(*) FROM sync_record WHERE state IN ('PENDING', 'FAILED')")
    int countPending();

    @Query("SELECT * FROM sync_record WHERE state IN ('PENDING', 'FAILED')")
    List<SyncRecordEntity> getPending();

    @Query("UPDATE sync_record SET state = :state, retryCount = retryCount + :retryIncrement, lastError = :lastError, updatedAt = :updatedAt WHERE id = :id")
    void updateState(String id, String state, int retryIncrement, String lastError, long updatedAt);
}