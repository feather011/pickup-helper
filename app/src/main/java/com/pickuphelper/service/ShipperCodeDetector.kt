package com.pickuphelper.service

import com.pickuphelper.data.provider.ShipperCodeMapper

object ShipperCodeDetector {
    fun detect(waybillNo: String): String? = ShipperCodeMapper.detect(waybillNo)
    fun detectWithName(waybillNo: String): Pair<String, String>? = ShipperCodeMapper.detectWithName(waybillNo)
}