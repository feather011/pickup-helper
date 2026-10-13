package com.pickuphelper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TraceDao {
    @Insert(entity = TraceEntity::class)
    suspend fun insertAll(traces: List<TraceEntity>)

    @Query("SELECT * FROM traces WHERE waybillNo = :waybillNo ORDER BY traceTime ASC")
    suspend fun getByWaybillNo(waybillNo: String): List<TraceEntity>

    @Query("DELETE FROM traces WHERE waybillNo = :waybillNo")
    suspend fun deleteByWaybillNo(waybillNo: String)
}
