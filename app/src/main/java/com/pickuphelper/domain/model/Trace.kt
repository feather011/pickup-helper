package com.pickuphelper.domain.model

data class Trace(
    val waybillNo: String,
    val time: String,
    val station: String,
    val action: Status
)
