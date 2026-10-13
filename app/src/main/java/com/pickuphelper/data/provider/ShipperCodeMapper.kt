package com.pickuphelper.data.provider

import com.pickuphelper.domain.model.Status

/**
 * Shipper code detection from waybill number prefix.
 * Reference: architecture.md Section 7.2.
 *
 * Prefix inference table (partial — covers common carriers):
 *   YT  -> YTO (圆通)
 *   SF  -> SF   (顺丰)
 *   ZT  -> ZTO  (中通)
 *   YD  -> Yunda(韵达)
 *   JD  -> JD   (京东)
 *   EMS -> EMS  (中国邮政EMS)
 *   ST  -> ST   (申通)
 *   ZJ  -> ZJS  (宅急送)
 *   TT  -> TT   (天天快递)
 *   XL  -> XLA  (新锐快递)
 */
object ShipperCodeMapper {

    private val prefixMap = listOf(
        PrefixEntry("YT", "YTO", "圆通速递"),
        PrefixEntry("SF", "SF", "顺丰速运"),
        PrefixEntry("ZT", "ZTO", "中通快递"),
        PrefixEntry("YD", "Yunda", "韵达快递"),
        PrefixEntry("JD", "JD", "京东物流"),
        PrefixEntry("EMS", "EMS", "中国邮政EMS"),
        PrefixEntry("ST", "STO", "申通快递"),
        PrefixEntry("ZJ", "ZJS", "宅急送"),
        PrefixEntry("TT", "TT", "天天快递"),
        PrefixEntry("XL", "XLA", "新锐快递"),
        // Fallback — YTO as the default for YT-prefixed unknowns
        PrefixEntry("YT", "YTO", "圆通速递"),
    )

    private data class PrefixEntry(
        val prefix: String,
        val code: String,
        val chineseName: String
    )

    /**
     * Detect shipper code from a waybill number.
     * Returns the best-match shipper code, or null if unknown.
     */
    fun detect(waybillNo: String): String? {
        if (waybillNo.isBlank()) return null
        val upper = waybillNo.uppercase()

        // Try longest prefix first (e.g. "EMS" before "YT")
        val sorted = prefixMap.sortedByDescending { it.prefix.length }
        for (entry in sorted) {
            if (upper.startsWith(entry.prefix)) {
                return entry.code
            }
        }
        return null
    }

    /**
     * Convert a shipper code to its Chinese display name.
     */
    fun toChineseName(shipperCode: String): String {
        return prefixMap.find { it.code == shipperCode }?.chineseName
            ?: "未知快递"
    }

    /**
     * Detect shipper code AND return Chinese name in one call.
     */
    fun detectWithName(waybillNo: String): Pair<String, String>? {
        val code = detect(waybillNo) ?: return null
        return code to toChineseName(code)
    }

    /**
     * All known shipper codes (for UI display).
     */
    fun allKnownCodes(): List<Pair<String, String>> {
        return prefixMap
            .distinctBy { it.code }
            .map { it.code to it.chineseName }
    }
}
