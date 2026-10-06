package com.micontrollaboral.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EmploymentDao {
    @Query("SELECT * FROM employment WHERE deletedAt IS NULL ORDER BY name")
    LiveData<List<EmploymentEntity>> observeActiveEmployments();

    @Query("SELECT COUNT(*) FROM employment WHERE deletedAt IS NULL")
    int countActiveEmployments();

    @Query("SELECT * FROM employment")
    List<EmploymentEntity> getAll();

    @Query("DELETE FROM employment")
    void deleteAll();

    @Insert
    void insertAll(List<EmploymentEntity> employments);

    @Update
    void update(EmploymentEntity employment);
}