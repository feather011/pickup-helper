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
            ProviderConfig.ACTIVE_KDNIAO -> Kuaidi100Provider(context)
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
