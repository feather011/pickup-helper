package com.pickuphelper.data.provider

import com.pickuphelper.domain.model.PickupResult

/**
 * 取件码查询契约（Contract-First）
 *
 * 所有 Provider 实现（Mock / Kuaidi100）均实现此接口。
 * App 业务层只依赖此接口，不直接调用具体实现。
 *
 * @param waybillNo 运单号
 * @param shipperCode 快递公司编码（可空，由 Provider 自行推断）
 * @param receiverPhone 收件人手机号（Kuaidi100 取件码接口硬要求，Mock 忽略）
 */
interface PickupProvider {
    suspend fun query(
        waybillNo: String,
        shipperCode: String?,
        receiverPhone: String? = null
    ): PickupResult
}
