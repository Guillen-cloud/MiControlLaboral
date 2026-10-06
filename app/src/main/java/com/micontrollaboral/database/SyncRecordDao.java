package com.micontrollaboral.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SyncRecordDao {
    @Insert
    void insertOrReplace(SyncRecordEntity record);

    @Query("SELECT COUNT(*) FROM sync_record WHERE state IN ('PENDING', 'FAILED')")
    int countPending();
}