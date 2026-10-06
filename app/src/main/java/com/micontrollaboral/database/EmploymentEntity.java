package com.micontrollaboral.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "employment")
public class EmploymentEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public int fullRateCents;
    public String usualDays;
    public boolean active;
    public long createdAt;
    public long updatedAt;
    public Long deletedAt;
}