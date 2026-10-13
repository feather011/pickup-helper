package com.pickuphelper.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusMapperTest {

    @Test
    fun state211_mapsToArrived() {
        assertEquals(Status.ARRIVED, StatusMapper.fromKuaidiNiao(2, 211))
    }

    @Test
    fun stateEx3xx_mapsToSigned() {
        assertEquals(Status.SIGNED, StatusMapper.fromKuaidiNiao(3, 301))
        assertEquals(Status.SIGNED, StatusMapper.fromKuaidiNiao(3, 399))
    }

    @Test
    fun stateEx4xx_mapsToException() {
        assertEquals(Status.EXCEPTION, StatusMapper.fromKuaidiNiao(2, 401))
        assertEquals(Status.EXCEPTION, StatusMapper.fromKuaidiNiao(2, 499))
    }

    @Test
    fun state1_mapsToCollected() {
        assertEquals(Status.COLLECTED, StatusMapper.fromKuaidiNiao(1, null))
    }

    @Test
    fun state2_mapsToInTransit() {
        assertEquals(Status.IN_TRANSIT, StatusMapper.fromKuaidiNiao(2, null))
    }

    @Test
    fun state0_mapsToNoTrace() {
        assertEquals(Status.NO_TRACE, StatusMapper.fromKuaidiNiao(0, null))
    }

    @Test
    fun fromState_withState3_returnsSigned() {
        assertEquals(Status.SIGNED, StatusMapper.fromState(3))
    }
}
