package com.micontrollaboral.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "work_session", foreignKeys = @ForeignKey(entity = EmploymentEntity.class, parentColumns = "id", childColumns = "employmentId", onDelete = ForeignKey.RESTRICT), indices = @Index(value = {
        "employmentId", "workedDate" }, unique = true))
public class WorkSessionEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String employmentId;
    @NonNull
    public String workedDate;
    @NonNull
    public String sessionType;
    public int amountCents;
    public String note;
    public long createdAt;
    public long updatedAt;
    public Long deletedAt;
}