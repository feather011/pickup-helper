package com.pickuphelper.data.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShipperCodeMapperTest {

    @Test
    fun detectYTprefixReturnsYTO() {
        assertEquals("YTO", ShipperCodeMapper.detect("YT12345678"))
    }

    @Test
    fun detectSFPrefixReturnsSF() {
        assertEquals("SF", ShipperCodeMapper.detect("SF1234567890"))
    }

    @Test
    fun detectJDPrefixReturnsJD() {
        assertEquals("JD", ShipperCodeMapper.detect("JD12345678"))
    }

    @Test
    fun detectLowercaseReturnsYTO() {
        assertEquals("YTO", ShipperCodeMapper.detect("yt12345678"))
    }

    @Test
    fun detectEmptyStringReturnsNull() {
        assertNull(ShipperCodeMapper.detect(""))
    }

    @Test
    fun detectUnknownPrefixReturnsNull() {
        assertNull(ShipperCodeMapper.detect("UNKNOWN12345"))
    }

    @Test
    fun toChineseNameYTO() {
        assertEquals("圆通速递", ShipperCodeMapper.toChineseName("YTO"))
    }

    @Test
    fun toChineseNameSF() {
        assertEquals("顺丰速运", ShipperCodeMapper.toChineseName("SF"))
    }

    @Test
    fun toChineseNameJD() {
        assertEquals("京东物流", ShipperCodeMapper.toChineseName("JD"))
    }

    @Test
    fun toChineseNameUNKNOWN() {
        assertEquals("未知快递", ShipperCodeMapper.toChineseName("UNKNOWN"))
    }

    @Test
    fun detectWithNameReturnsCodeAndName() {
        val result = ShipperCodeMapper.detectWithName("YT12345678")
        assertNotNull(result)
        assertEquals("YTO", result!!.first)
    }

    @Test
    fun allKnownCodesReturnsNonEmptyList() {
        val codes = ShipperCodeMapper.allKnownCodes()
        assertTrue(codes.isNotEmpty())
        assertTrue(codes.any { it.first == "YTO" })
        assertTrue(codes.any { it.first == "SF" })
    }

    // ---- fromChineseName: reverse mapping for dropdown fallback ----

    @Test
    fun fromChineseNameZTO() {
        assertEquals("ZTO", ShipperCodeMapper.fromChineseName("中通快递"))
    }

    @Test
    fun fromChineseNameYunda() {
        assertEquals("Yunda", ShipperCodeMapper.fromChineseName("韵达快递"))
    }

    @Test
    fun fromChineseNameSTO() {
        assertEquals("STO", ShipperCodeMapper.fromChineseName("申通快递"))
    }

    @Test
    fun fromChineseNameYTO() {
        assertEquals("YTO", ShipperCodeMapper.fromChineseName("圆通速递"))
    }

    @Test
    fun fromChineseNameSF() {
        assertEquals("SF", ShipperCodeMapper.fromChineseName("顺丰速运"))
    }

    @Test
    fun fromChineseNameEMS() {
        assertEquals("EMS", ShipperCodeMapper.fromChineseName("中国邮政EMS"))
    }

    @Test
    fun fromChineseNameJT() {
        assertEquals("JT", ShipperCodeMapper.fromChineseName("极兔速递"))
    }

    @Test
    fun fromChineseNameUnknownReturnsNull() {
        assertNull(ShipperCodeMapper.fromChineseName("菜鸟驿站"))
    }

    @Test
    fun fromChineseNameNullReturnsNull() {
        assertNull(ShipperCodeMapper.fromChineseName(null))
    }

    @Test
    fun fromChineseNameEmptyReturnsNull() {
        assertNull(ShipperCodeMapper.fromChineseName(""))
    }

    @Test
    fun fromChineseNameTrimsWhitespace() {
        assertEquals("ZTO", ShipperCodeMapper.fromChineseName("  中通快递  "))
    }

    // ---- end-to-end: pure numeric waybill + dropdown choice ----

    @Test
    fun pureNumericPlusDropdownFallback_zhongtong() {
        val waybill = "1234567890"   // 纯数字，前缀推断必返回 null
        val detected = ShipperCodeMapper.detect(waybill)
        assertNull(detected)  // 确认前缀推断为 null
        val userPicked = ShipperCodeMapper.fromChineseName("中通快递")
        assertEquals("ZTO", userPicked)
    }

    @Test
    fun pureNumericPlusDropdownFallback_yunda() {
        val userPicked = ShipperCodeMapper.fromChineseName("韵达快递")
        assertEquals("Yunda", userPicked)
    }

    @Test
    fun pureNumericPlusDropdownFallback_shentong() {
        val userPicked = ShipperCodeMapper.fromChineseName("申通快递")
        assertEquals("STO", userPicked)
    }
}
