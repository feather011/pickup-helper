package com.pickuphelper.domain.model

/**
 * Maps KuaidiNiao API State / StateEx integers to our canonical Status enum.
 * Reference: architecture.md Section 3.
 */
object StatusMapper {

    /**
     * Map KuaidiNiao State + StateEx to canonical Status (String version).
     *
     * @param state KuaidiNiao State as string (0=none, 1=collected, 2=in_transit, 3=signed)
     * @param stateEx KuaidiNiao StateEx as string (e.g. "211" = arrived at station)
     */
    fun mapFromKuaidiNiao(state: String?, stateEx: String?): Status {
        val s = state?.toIntOrNull()
        val ex = stateEx?.toIntOrNull()
        return fromKuaidiNiao(s, ex)
    }

    /**
     * Map KuaidiNiao State + StateEx to canonical Status (Int version).
     *
     * @param state KuaidiNiao State (0=none, 1=collected, 2=in_transit, 3=signed)
     * @param stateEx KuaidiNiao StateEx extended code (e.g. 211 = arrived at station)
     */
    fun fromKuaidiNiao(state: Int?, stateEx: Int?): Status {
        // Signed / delivered (StateEx 3xx)
        if (stateEx != null && stateEx in 300..399) return Status.SIGNED

        // Exception / problem (StateEx 4xx)
        if (stateEx != null && stateEx in 400..499) return Status.EXCEPTION

        // ARRIVED — StateEx 211 indicates arrived at Cainiao station/cabinet
        if (stateEx == 211) return Status.ARRIVED

        // Standard state mapping
        return when (state) {
            1 -> Status.COLLECTED
            2 -> Status.IN_TRANSIT
            3 -> Status.SIGNED
            else -> Status.NO_TRACE
        }
    }

    /**
     * Map from State alone (when StateEx is not available).
     */
    fun fromState(state: Int): Status = when (state) {
        1 -> Status.COLLECTED
        2 -> Status.IN_TRANSIT
        3 -> Status.SIGNED
        else -> Status.NO_TRACE
    }
}
