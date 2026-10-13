package com.pickuphelper.domain.model

enum class Status(val value: Int) {
    NO_TRACE(0),
    COLLECTED(1),
    IN_TRANSIT(2),
    ARRIVED(3),
    SIGNED(4),
    EXCEPTION(5);

    companion object {
        fun fromValue(value: Int): Status = entries.firstOrNull { it.value == value } ?: NO_TRACE
    }
}
