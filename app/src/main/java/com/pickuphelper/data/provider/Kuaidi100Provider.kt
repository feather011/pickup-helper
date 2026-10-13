package com.pickuphelper.data.provider

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pickuphelper.domain.model.Failure
import com.pickuphelper.domain.model.FailureType
import com.pickuphelper.domain.model.Parcel
import com.pickuphelper.domain.model.PickupResult
import com.pickuphelper.domain.model.Status
import com.pickuphelper.domain.model.StatusMapper
import com.pickuphelper.domain.model.Trace
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Kuaidi100 真实 API Provider
 *
 * 调用自建后端代理（Ktor），由后端转发至快递100。
 * App 不直接访问快递100，也不持有 key/secret。
 *
 * 切换方式：将 ProviderConfig.ACTIVE 设为 "kdniao" 即可。
 */
class Kuaidi100Provider(private val context: Context) : PickupProvider {

    private val gson = Gson()
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { gson() }
        engine {
            config {
                connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    companion object {
        private const val TAG = "Kuaidi100Provider"
    }

    override suspend fun query(
        waybillNo: String,
        shipperCode: String?,
        receiverPhone: String?
    ): PickupResult = withContext(Dispatchers.IO) {
        // 1. 获取后端地址
        val backendUrl = ProviderConfig.getBackendUrl(context)
            ?: return@withContext makeError(
                FailureType.UNKNOWN,
                "后端地址未配置，请在设置中填写后端服务地址"
            )

        // 2. 构建请求体
        val requestBody = mapOf(
            "waybillNo" to waybillNo,
            "shipperCode" to (shipperCode ?: ""),
            "receiverPhone" to (receiverPhone ?: "")
        )

        Log.d(TAG, "Query: waybillNo=$waybillNo, shipperCode=$shipperCode, backendUrl=$backendUrl")

        try {
            // 3. 发送请求到后端代理
            val response: HttpResponse = client.post("$backendUrl/api/pickup-code") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(requestBody))
            }

            val responseBody = response.bodyAsText()
            Log.d(TAG, "Response: $responseBody")

            // 4. 解析响应
            parseKuaidi100Response(responseBody, waybillNo, shipperCode)

        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            makeError(
                FailureType.NETWORK_ERROR,
                "网络请求失败：${e.message ?: "未知错误"}"
            )
        }
    }

    /**
     * 解析快递100 后端返回的 JSON 响应
     *
     * 响应格式（成功）：
     * {
     *   "result": true,
     *   "returnCode": "200",
     *   "data": [{
     *     "LogisticCode": "YT001234567890",
     *     "ShipperCode": "YTO",
     *     "State": "3",
     *     "StateEx": "211",
     *     "PickupCode": "A-03-1234",
     *     "StationName": "菜鸟驿站(望京SOHO店)",
     *     "StationAddress": "北京市朝阳区...",
     *     "Traces": [
     *       {"AcceptTime": "2026-10-13 14:30:00", "AcceptStation": "【北京】快件已到站", "Remark": ""}
     *     ]
     *   }]
     * }
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseKuaidi100Response(
        json: String,
        waybillNo: String,
        shipperCode: String?
    ): PickupResult {
        return try {
            val root = gson.fromJson(json, JsonObject::class.java)

            // 检查 result 字段
            val result = root.get("result")?.asBoolean ?: false
            if (!result) {
                val message = root.get("message")?.asString ?: "未知错误"
                return makeError(FailureType.UNKNOWN, message)
            }

            // 解析 data 数组
            val dataArray = root.getAsJsonArray("data")
            if (dataArray == null || dataArray.size() == 0) {
                return makeError(FailureType.NO_TRACE, "暂无物流信息")
            }

            val item = dataArray[0].asJsonObject

            // 提取字段
            val code = item.get("LogisticCode")?.asString ?: waybillNo
            val shipper = item.get("ShipperCode")?.asString ?: shipperCode ?: ""
            val state = item.get("State")?.asString ?: "0"
            val stateEx = item.get("StateEx")?.asString ?: ""
            val pickupCode = item.get("PickupCode")?.asString?.takeIf { it.isNotBlank() }
            val stationName = item.get("StationName")?.asString?.takeIf { it.isNotBlank() }
            val stationAddress = item.get("StationAddress")?.asString?.takeIf { it.isNotBlank() }
            val latestTrace = item.getAsJsonArray("Traces")?.firstOrNull()
                ?.asJsonObject?.get("AcceptStation")?.asString

            // 映射状态
            val status = StatusMapper.mapFromKuaidiNiao(state, stateEx)

            // 构建 Parcel
            val now = System.currentTimeMillis()
            val parcel = Parcel(
                waybillNo = code,
                shipperCode = shipper,
                shipperName = ShipperCodeMapper.toChineseName(shipper),
                pickupCode = pickupCode,
                stationName = stationName,
                stationAddress = stationAddress,
                status = status,
                latestTrace = latestTrace,
                arrivedAt = if (status == Status.ARRIVED) now else null,
                signedAt = if (status == Status.SIGNED) now else null,
                createdAt = now,
                updatedAt = now
            )

            // 构建 Trace 列表
            val traces = item.getAsJsonArray("Traces")?.mapIndexed { index, element ->
                val trace = element.asJsonObject
                val traceState = trace.get("State")?.asString ?: state
                val traceStateEx = trace.get("StateEx")?.asString ?: stateEx
                Trace(
                    waybillNo = code,
                    time = trace.get("AcceptTime")?.asString ?: "",
                    station = trace.get("AcceptStation")?.asString ?: "",
                    action = StatusMapper.mapFromKuaidiNiao(traceState, traceStateEx)
                )
            } ?: emptyList()

            PickupResult(parcel = parcel, traces = traces)

        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
            makeError(FailureType.UNKNOWN, "响应解析失败：${e.message}")
        }
    }

    private fun makeError(type: FailureType, message: String): PickupResult {
        return PickupResult(
            parcel = null,
            failure = Failure(type = type, message = message)
        )
    }
}

