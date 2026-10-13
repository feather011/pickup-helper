package com.pickuphelper.domain.model

data class PickupResult(
    val parcel: Parcel? = null,
    val traces: List<Trace> = emptyList(),
    val failure: Failure? = null
) {
    init {
        require(parcel != null || failure != null) {
            "拿货结果必须在“包裹”与“失败”中二选一，不能两者都为空"
        }
        require(parcel == null || failure == null) {
            "拿货结果不能同时包含“包裹”和“失败”"
        }
    }
}
