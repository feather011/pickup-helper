package com.pickuphelper.service

object WaybillExtractor {
    private val PATTERN = Regex("[A-Za-z0-9]{4,30}")
    fun extract(text: String): String? = if (text.isBlank()) null else PATTERN.find(text)?.value?.uppercase()
    fun containsWaybill(text: String): Boolean = extract(text) != null
}
