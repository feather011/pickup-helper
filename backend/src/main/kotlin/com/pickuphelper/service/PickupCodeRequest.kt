package com.pickuphelper.service

/**
 * 从 App 收到的取件码查询请求
 *
 * @param waybillNo 运单号
 * @param shipperCode 快递公司编码（如 YTO/SF/JD/EMS，可空）
 * @param receiverPhone 收件人手机号（快递100 取件码接口硬要求）
 */
data class PickupCodeRequest(
    val waybillNo: String,
    val shipperCode: String?,
    val receiverPhone: String? = null
)
