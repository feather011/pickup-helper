package com.pickuphelper.data.provider

import com.pickuphelper.domain.model.FailureType
import com.pickuphelper.domain.model.Status
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MockProviderTest {

    private val provider = MockProvider()

    @Test
    fun query_YT001_returnsArrivedWithPickupCode() = runTest {
        val result = provider.query("YT001", null)
        assertNotNull(result.parcel)
        assertEquals(Status.ARRIVED, result.parcel!!.status)
        assertEquals("A-03-1234", result.parcel!!.pickupCode)
        assertEquals("YTO", result.parcel!!.shipperCode)
    }

    @Test
    fun query_YT002_returnsInTransit() = runTest {
        val result = provider.query("YT002", null)
        assertNotNull(result.parcel)
        assertEquals(Status.IN_TRANSIT, result.parcel!!.status)
    }

    @Test
    fun query_SF003_returnsSigned() = runTest {
        val result = provider.query("SF003", null)
        assertNotNull(result.parcel)
        assertEquals(Status.SIGNED, result.parcel!!.status)
    }

    @Test
    fun query_JD004_returnsArrivedNoPickupCode() = runTest {
        val result = provider.query("JD004", null)
        assertNotNull(result.parcel)
        assertEquals(Status.ARRIVED, result.parcel!!.status)
        assertNull(result.parcel!!.pickupCode)
    }

    @Test
    fun query_EMS005_returnsNoTrace() = runTest {
        val result = provider.query("EMS005", null)
        assertNotNull(result.failure)
        assertEquals(FailureType.NO_TRACE, result.failure!!.type)
    }

    @Test
    fun query_EMS006_returnsException() = runTest {
        val result = provider.query("EMS006", null)
        assertNotNull(result.failure)
    }

    @Test
    fun query_unknownWaybill_returnsNoTraceParcel() = runTest {
        val result = provider.query("UNKNOWN999", null)
        assertNotNull(result.parcel)
        assertEquals(Status.NO_TRACE, result.parcel!!.status)
    }

    @Test
    fun query_lowercase_normalizesToUppercase() = runTest {
        val result = provider.query("yt001", null)
        assertNotNull(result.parcel)
        assertEquals("YT001", result.parcel!!.waybillNo)
    }

    @Test
    fun query_withExplicitShipperCode_usesIt_forUnknownWaybill() = runTest {
        // For unknown waybillNos, explicit shipperCode takes effect
        val result = provider.query("NOTINSAMPLE1", "SF")
        assertNotNull(result.parcel)
        assertEquals("SF", result.parcel!!.shipperCode)
        assertEquals("顺丰速运", result.parcel!!.shipperName)
    }

    @Test
    fun query_withExplicitShipperCode_overridesDetection() = runTest {
        // Unknown waybill, explicit code overrides prefix detection
        val result = provider.query("NOTINSAMPLE2", "YTO")
        assertNotNull(result.parcel)
        assertEquals("YTO", result.parcel!!.shipperCode)
    }
}
