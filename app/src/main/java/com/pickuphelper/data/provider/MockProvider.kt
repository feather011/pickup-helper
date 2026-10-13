package com.pickuphelper.data.provider

import com.pickuphelper.domain.model.Failure
import com.pickuphelper.domain.model.FailureType
import com.pickuphelper.domain.model.Parcel
import com.pickuphelper.domain.model.PickupResult
import com.pickuphelper.domain.model.Status
import com.pickuphelper.domain.model.Trace

/**
 * Mock implementation of PickupProvider — used during MVP when ACTIVE=mock.
 * Returns 6 predefined scenarios for end-to-end testing.
 *
 * Sample waybill numbers:
 *   YT001 — Normal Cainiao ARRIVED with pickupCode A-03-1234
 *   YT002 — IN_TRANSIT (on the way)
 *   SF003 — SIGNED (already delivered)
 *   JD004 — ARRIVED but NO pickup code (non-Cainiao platform)
 *   EMS005 — NO_TRACE (no logistics info found)
 *   EMS006 — EXCEPTION (package problem)
 */
class MockProvider : PickupProvider {

    private val samples = mapOf(
        "YT001" to makeNormalCainiao(),
        "YT002" to makeInTransit(),
        "SF003" to makeSigned(),
        "JD004" to makeNonCainiao(),
        "EMS005" to makeNoTrace(),
        "EMS006" to makeException()
    )

    override suspend fun query(waybillNo: String, shipperCode: String?): PickupResult {
        val sample = samples[waybillNo.uppercase()]
        return if (sample != null) {
            sample
        } else {
            // 未知运单号——返回 NOT_FOUND 失败，由 MainViewModel 弹错误提示（不落库）
            PickupResult(
                parcel = null,
                failure = Failure(
                    type = FailureType.UNKNOWN,
                    message = "未找到该单号的取件信息，请确认单号是否正确"
                )
            )
        }
    }

    // ─── 样本构建器 ──────────────────────────────────────────────────

    private fun makeNormalCainiao(): PickupResult {
        val waybillNo = "YT001"
        val shipperCode = "YTO"
        val now = System.currentTimeMillis()
        val arrivedAt = now - 3600_000 // 一小时前

        val traces = listOf(
            Trace(waybillNo, "2026-10-13 08:00:00", "【北京市】快件已发货", Status.COLLECTED),
            Trace(waybillNo, "2026-10-13 12:00:00", "【北京市】快件在途中", Status.IN_TRANSIT),
            Trace(waybillNo, "2026-10-13 14:30:00", "【北京市望京SOHO店】快件已到站", Status.ARRIVED),
        )

        val parcel = Parcel(
            waybillNo = waybillNo,
            shipperCode = shipperCode,
            shipperName = "圆通速递",
            platform = "taobao",
            pickupCode = "A-03-1234",
            stationName = "菜鸟驿站(望京SOHO店)",
            stationAddress = "北京市朝阳区望京SOHO T3",
            status = Status.ARRIVED,
            latestTrace = "【北京市望京SOHO店】快件已到站",
            arrivedAt = arrivedAt,
            createdAt = now,
            updatedAt = now
        )
        return PickupResult(parcel = parcel, traces = traces)
    }

    private fun makeInTransit(): PickupResult {
        val waybillNo = "YT002"
        val now = System.currentTimeMillis()

        val traces = listOf(
            Trace(waybillNo, "2026-10-11 10:00:00", "【上海市】快件已揽收", Status.COLLECTED),
            Trace(waybillNo, "2026-10-12 08:00:00", "【杭州市】快件转运中", Status.IN_TRANSIT),
        )

        val parcel = Parcel(
            waybillNo = waybillNo,
            shipperCode = "SF",
            shipperName = "顺丰速运",
            platform = "pdd",
            status = Status.IN_TRANSIT,
            latestTrace = "【杭州市】快件转运中",
            createdAt = now,
            updatedAt = now
        )
        return PickupResult(parcel = parcel, traces = traces)
    }

    private fun makeSigned(): PickupResult {
        val waybillNo = "SF003"
        val now = System.currentTimeMillis()
        val signedAt = now - 86400_000 // 1 day ago

        val traces = listOf(
            Trace(waybillNo, "2026-10-10 09:00:00", "【深圳市】已揽收", Status.COLLECTED),
            Trace(waybillNo, "2026-10-11 15:00:00", "【深圳市】快件派送中", Status.IN_TRANSIT),
            Trace(waybillNo, "2026-10-12 10:30:00", "【深圳市南山区】已签收", Status.SIGNED),
        )

        val parcel = Parcel(
            waybillNo = waybillNo,
            shipperCode = "SF",
            shipperName = "顺丰速运",
            platform = "jd",
            status = Status.SIGNED,
            latestTrace = "【深圳市南山区】已签收，签收人：本人",
            signedAt = signedAt,
            createdAt = now,
            updatedAt = now
        )
        return PickupResult(parcel = parcel, traces = traces)
    }

    private fun makeNonCainiao(): PickupResult {
        val waybillNo = "JD004"
        val now = System.currentTimeMillis()

        // Non-Cainiao — ARRIVED but no pickup code (no Cainiao station)
        val parcel = Parcel(
            waybillNo = waybillNo,
            shipperCode = "JD",
            shipperName = "京东物流",
            platform = "jd",
            pickupCode = null,          // No Cainiao pickup code
            stationName = null,          // No Cainiao station
            status = Status.ARRIVED,
            latestTrace = "【北京市】快件已到站（京东直营站点）",
            arrivedAt = now,
            createdAt = now,
            updatedAt = now
        )
        return PickupResult(
            parcel = parcel,
            traces = listOf(
                Trace(waybillNo, "2026-10-13 09:00:00", "【北京市】已发货", Status.COLLECTED),
                Trace(waybillNo, "2026-10-13 14:00:00", "【北京市】快件已到站（京东直营站点）", Status.ARRIVED),
            )
        )
    }

    private fun makeNoTrace(): PickupResult {
        return PickupResult(
            failure = Failure(
                type = FailureType.NO_TRACE,
                message = "暂无物流信息，请核对运单号是否正确"
            )
        )
    }

    private fun makeException(): PickupResult {
        return PickupResult(
            failure = Failure(
                type = FailureType.UNKNOWN,
                message = "快件状态异常，请联系快递员核实",
                rawResponse = "{\"StateEx\": 401, \"Remark\": \"收件人电话无法接通\"}"
            )
        )
    }
}


