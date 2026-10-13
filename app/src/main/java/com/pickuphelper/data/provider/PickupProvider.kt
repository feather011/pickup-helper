package com.pickuphelper.data.provider

import com.pickuphelper.domain.model.PickupResult

interface PickupProvider {
    suspend fun query(waybillNo: String, shipperCode: String?): PickupResult
}
