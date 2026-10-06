package com.micontrollaboral.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PaymentDao {
    @Insert
    void insert(PaymentEntity payment);

    @Insert
    void insertAll(List<PaymentEntity> payments);

    @Query("SELECT * FROM payment")
    List<PaymentEntity> getAll();

    @Query("DELETE FROM payment")
    void deleteAll();

    @Query("SELECT COALESCE(SUM(totalAmountCents), 0) FROM payment WHERE receivedAt BETWEEN :startDate AND :endDate AND deletedAt IS NULL")
    int sumReceivedBetween(String startDate, String endDate);
}