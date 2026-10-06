package com.micontrollaboral.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_record")
public class SyncRecordEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String entityType;
    @NonNull
    public String entityId;
    @NonNull
    public String operation;
    @NonNull
    public String state;
    public long updatedAt;
    public int retryCount;
    public String lastError;
}