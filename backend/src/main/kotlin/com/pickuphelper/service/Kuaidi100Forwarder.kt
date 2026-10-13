package com.pickuphelper.service

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * 快递100 API 转发器
 * 
 * 签名算法：MD5(param + t + key + secret).uppercase()
 * 文档：https://www.kuaidi100.com/openapi/
 * 
 * 注意：key 和 secret 只从环境变量读取，绝不硬编码或进日志。
 */
object Kuaidi100Forwarder {

    // 快递100 即时查询 API 地址
    private const val KUAIDI100_API = "https://api.kuaidi100.com/eorderservice.do"

    // 内存缓存：key = "$waybillNo:$shipperCode", value = Pair(result, timestamp)
    // 缓存 30 分钟降成本（免费档每日 100 次）
    private val cache = ConcurrentHashMap<String, Pair<Map<String, Any?>, Long>>()
    private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 分钟

    private val httpClient = HttpClient(OkHttp)

    /**
     * 转发取件码查询请求到快递100
     *
     * @param request 包含 waybillNo, shipperCode, receiverPhone(可选)
     * @return 快递100 原始响应 JSON 转成的 Map
     */
    suspend fun forward(request: PickupCodeRequest): Map<String, Any?> = withContext(Dispatchers.IO) {
        val cacheKey = "${request.waybillNo}:${request.shipperCode ?: ""}"

        // 1. 检查缓存
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
            println("[Cache HIT] $cacheKey")
            return@withContext cached.first
        }

        // 2. 读取环境变量（key/secret 绝不进日志）
        val key = System.getenv("KUAIDI100_KEY")
            ?: throw IllegalStateException("KUAIDI100_KEY 环境变量未设置")
        val secret = System.getenv("KUAIDI100_SECRET")
            ?: throw IllegalStateException("KUAIDI100_SECRET 环境变量未设置")

        // 3. 构建请求参数
        val t = System.currentTimeMillis().toString()
        val param = buildParamJson(request)

        // 4. 计算签名：MD5(param + t + key + secret).uppercase()
        val signSource = "$param$t$key$secret"
        val sign = md5(signSource).uppercase()

        // 5. 发送请求到快递100
        println("[Kuaidi100] Forwarding: waybillNo=${request.waybillNo}, shipperCode=${request.shipperCode}")

        val response: HttpResponse = httpClient.post(KUAIDI100_API) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "key" to key,
                    "param" to param,
                    "t" to t,
                    "sign" to sign,
                    "show" to "1",
                    "muti" to "1",
                    "order" to "desc"
                ).formUrlEncode()
            )
        }

        val body = response.bodyAsText()
        println("[Kuaidi100] Response: $body")

        // 6. 解析响应（透传，不做业务处理，返回给 App 自己解析）
        val result = parseResponse(body)

        // 7. 写入缓存
        cache[cacheKey] = Pair(result, System.currentTimeMillis())

        return@withContext result
    }

    /**
     * 构建快递100 请求参数 JSON
     */
    private fun buildParamJson(request: PickupCodeRequest): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"logisticCode\":\"${request.waybillNo}\",")
        sb.append("\"shipperCode\":\"${request.shipperCode ?: ""}\",")
        if (!request.receiverPhone.isNullOrBlank()) {
            sb.append("\"receiverPhone\":\"${request.receiverPhone}\",")
        }
        sb.append("\"customerName\":\"\"}")
        return sb.toString()
    }

    /**
     * 解析快递100 响应 JSON
     */
    private fun parseResponse(json: String): Map<String, Any?> {
        return try {
            val gson = com.google.gson.Gson()
            gson.fromJson(json, Map::class.java) as Map<String, Any?>
        } catch (e: Exception) {
            mapOf(
                "success" to false,
                "message" to "响应解析失败: ${e.message}",
                "raw" to json
            )
        }
    }

    /** 计算 MD5 */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** 清理过期缓存（可定时调用） */
    fun cleanExpiredCache() {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries.filter { now - it.value.second >= CACHE_TTL_MS }.map { it.key }
        expiredKeys.forEach { cache.remove(it) }
    }
}
