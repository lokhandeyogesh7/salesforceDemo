package com.salesforce.samples.smartsyncexplorer;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface DaoAccess {

    @Insert
    void insertMultipleRecord(ApiObjects... universities);

    @Insert
    void insertOnlySingleRecord(ApiObjects university);

    @Query("SELECT * FROM ApiObjects")
    List<ApiObjects> fetchAllData();

    @Delete
    void deleteRecord(ApiObjects university);
}
