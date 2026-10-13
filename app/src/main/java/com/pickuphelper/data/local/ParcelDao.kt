package com.pickuphelper.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Data classes for GROUP BY query results — field names must match SQL column aliases
data class ShipperCount(
    val shipperName: String,
    @ColumnInfo(name = "cnt") val count: Int
)

data class StatusCount(
    val status: Int,
    @ColumnInfo(name = "cnt") val count: Int
)

data class PlatformCount(
    val platform: String?,
    @ColumnInfo(name = "cnt") val count: Int
)

data class DailyCount(
    @ColumnInfo(name = "d") val date: String,
    @ColumnInfo(name = "cnt") val count: Int
)

data class StationCount(
    val stationName: String,
    @ColumnInfo(name = "cnt") val count: Int
)

@Dao
interface ParcelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ParcelEntity)

    @Query("SELECT * FROM parcels WHERE waybillNo = :waybillNo LIMIT 1")
    suspend fun getByWaybillNo(waybillNo: String): ParcelEntity?

    @Query("SELECT * FROM parcels ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE status = :status ORDER BY updatedAt DESC")
    fun getByStatus(status: Int): Flow<List<ParcelEntity>>

    @Query("UPDATE parcels SET status = :status, updatedAt = :updatedAt WHERE waybillNo = :waybillNo")
    suspend fun updateStatus(waybillNo: String, status: Int, updatedAt: Long)

    @Query("UPDATE parcels SET signedAt = :pickedAt, updatedAt = :pickedAt WHERE waybillNo = :waybillNo")
    suspend fun markPicked(waybillNo: String, pickedAt: Long)

    @Query("DELETE FROM parcels WHERE waybillNo = :waybillNo")
    suspend fun deleteByWaybillNo(waybillNo: String)

    @Query("SELECT COUNT(*) FROM parcels")
    suspend fun getCountTotal(): Int

    @Query("SELECT COUNT(*) FROM parcels WHERE createdAt > :since")
    suspend fun getCountThisMonth(since: Long): Int

    @Query("SELECT COUNT(*) FROM parcels WHERE status = 3 AND signedAt IS NULL")
    suspend fun getCountArrivedNotPicked(): Int

    @Query("SELECT COUNT(*) FROM parcels WHERE status = 4")
    suspend fun getCountSigned(): Int

    @Query("SELECT shipperName, COUNT(*) as cnt FROM parcels GROUP BY shipperName ORDER BY cnt DESC")
    suspend fun getGroupByShipper(): List<ShipperCount>

    @Query("SELECT status, COUNT(*) as cnt FROM parcels GROUP BY status")
    suspend fun getGroupByStatus(): List<StatusCount>

    @Query("SELECT platform, COUNT(*) as cnt FROM parcels GROUP BY platform")
    suspend fun getGroupByPlatform(): List<PlatformCount>

    @Query("""
        SELECT date(createdAt/1000, 'unixepoch') as d, COUNT(*) as cnt
        FROM parcels
        WHERE createdAt > :since
        GROUP BY d
        ORDER BY d ASC
    """)
    suspend fun getDailyTrendSince(since: Long): List<DailyCount>

    @Query("""
        SELECT stationName, COUNT(*) as cnt
        FROM parcels
        WHERE stationName IS NOT NULL
        GROUP BY stationName
        ORDER BY cnt DESC
        LIMIT :limit
    """)
    suspend fun getTopStations(limit: Int): List<StationCount>
}
