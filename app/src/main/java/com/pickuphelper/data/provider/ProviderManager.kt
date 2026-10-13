package com.pickuphelper.data.provider

import android.content.Context
import com.pickuphelper.domain.model.Failure
import com.pickuphelper.domain.model.FailureType
import com.pickuphelper.domain.model.PickupResult

object ProviderManager {
    @Volatile
    private var cachedProvider: PickupProvider? = null

    fun get(context: Context): PickupProvider {
        cachedProvider?.let { return it }
        val active = ProviderConfig.getActive(context)
        val provider: PickupProvider = when (active) {
            ProviderConfig.ACTIVE_KDNIAO -> KuaidiNiaoProviderStub()
            else -> MockProvider()
        }
        cachedProvider = provider
        return provider
    }

    val currentProvider: PickupProvider?
        get() = cachedProvider

    fun resetCache() {
        cachedProvider = null
    }
}

class KuaidiNiaoProviderStub : PickupProvider {
    override suspend fun query(waybillNo: String, shipperCode: String?): PickupResult {
        return PickupResult(
            failure = Failure(
                type = FailureType.UNKNOWN,
                message = "快递鸟接口尚未配置，请先在设置中填写 API 密钥"
            )
        )
    }
}