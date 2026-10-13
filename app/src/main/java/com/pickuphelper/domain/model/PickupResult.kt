package com.pickuphelper.domain.model

data class PickupResult(
    val parcel: Parcel? = null,
    val traces: List<Trace> = emptyList(),
    val failure: Failure? = null
) {
    init {
        require(parcel != null || failure != null) {
            "PickupResult must have either parcel or failure, not both null"
        }
        require(parcel == null || failure == null) {
            "PickupResult cannot have both parcel and failure"
        }
    }
}
