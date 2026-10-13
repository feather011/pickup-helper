package com.pickuphelper.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pickuphelper.domain.model.Parcel
import com.pickuphelper.domain.model.Status

@Entity(
    tableName = "parcels",
    indices = [Index(value = ["waybillNo"], unique = true)]
)
data class ParcelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val waybillNo: String,
    val shipperCode: String,
    val shipperName: String,
    val platform: String? = null,
    val pickupCode: String? = null,
    val stationName: String? = null,
    val stationAddress: String? = null,
    val status: Int = 0,
    val latestTrace: String? = null,
    val arrivedAt: Long? = null,
    val signedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Parcel = Parcel(
        waybillNo = waybillNo,
        shipperCode = shipperCode,
        shipperName = shipperName,
        platform = platform,
        pickupCode = pickupCode,
        stationName = stationName,
        stationAddress = stationAddress,
        status = Status.fromValue(status),
        latestTrace = latestTrace,
        arrivedAt = arrivedAt,
        signedAt = signedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(p: Parcel): ParcelEntity = ParcelEntity(
            waybillNo = p.waybillNo,
            shipperCode = p.shipperCode,
            shipperName = p.shipperName,
            platform = p.platform,
            pickupCode = p.pickupCode,
            stationName = p.stationName,
            stationAddress = p.stationAddress,
            status = p.status.value,
            latestTrace = p.latestTrace,
            arrivedAt = p.arrivedAt,
            signedAt = p.signedAt,
            createdAt = p.createdAt,
            updatedAt = p.updatedAt
        )
    }
}
