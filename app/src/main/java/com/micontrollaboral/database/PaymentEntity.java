package com.micontrollaboral.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "payment",
        foreignKeys = @ForeignKey(
                entity = EmploymentEntity.class,
                parentColumns = "id",
                childColumns = "employmentId",
                onDelete = ForeignKey.RESTRICT
        ),
        indices = @Index("employmentId")
)
public class PaymentEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String employmentId;
    @NonNull
    public String receivedAt;
    public int totalAmountCents;
    public String note;
    public long createdAt;
    public long updatedAt;
    public Long deletedAt;
}