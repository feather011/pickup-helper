package com.pickuphelper.domain.model

enum class Status(val value: Int, val displayName: String) {
    NO_TRACE(0, "无轨迹"),
    COLLECTED(1, "已揽收"),
    IN_TRANSIT(2, "运输中"),
    ARRIVED(3, "已到达"),
    SIGNED(4, "已签收"),
    EXCEPTION(5, "异常");

    companion object {
        fun fromValue(value: Int): Status = entries.firstOrNull { it.value == value } ?: NO_TRACE
    }
}
