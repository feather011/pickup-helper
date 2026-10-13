package com.pickuphelper.domain.model

enum class FailureType {
    NETWORK_ERROR,
    TIMEOUT,
    NO_TRACE,
    NO_PICKUP_CODE,
    PERMISSION_DENIED,
    INVALID_WAYBILL,
    EXCEPTION,
    UNKNOWN
}

data class Failure(
    val type: FailureType,
    val message: String,
    val rawResponse: String? = null
)
