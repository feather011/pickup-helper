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
}
