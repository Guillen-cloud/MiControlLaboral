package com.micontrollaboral.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "payment_allocation",
        foreignKeys = {
                @ForeignKey(entity = PaymentEntity.class, parentColumns = "id", childColumns = "paymentId", onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = WorkSessionEntity.class, parentColumns = "id", childColumns = "workSessionId", onDelete = ForeignKey.RESTRICT)
        },
        indices = {
                @Index("paymentId"),
                @Index("workSessionId"),
                @Index(value = {"paymentId", "workSessionId"}, unique = true)
        }
)
public class PaymentAllocationEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String paymentId;
    @NonNull
    public String workSessionId;
    public int allocatedAmountCents;
}