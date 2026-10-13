package com.pickuphelper.domain.model

data class Parcel(
    val waybillNo: String,
    val shipperCode: String,
    val shipperName: String,
    val platform: String? = null,
    val pickupCode: String? = null,
    val stationName: String? = null,
    val stationAddress: String? = null,
    val status: Status = Status.NO_TRACE,
    val latestTrace: String? = null,
    val arrivedAt: Long? = null,
    val signedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
