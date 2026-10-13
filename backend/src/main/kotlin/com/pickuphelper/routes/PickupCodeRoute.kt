package com.pickuphelper.routes

import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import com.pickuphelper.service.Kuaidi100Forwarder
import com.pickuphelper.service.PickupCodeRequest
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PickupCodeRoute")

fun Routing.pickupCodeRoute() {
    post("/api/pickup-code") {
        val requestBody = call.receive(PickupCodeRequest::class)
        logger.info("Received pickup-code request: waybillNo=${requestBody.waybillNo}, shipperCode=${requestBody.shipperCode}")

        try {
            val result = Kuaidi100Forwarder.forward(requestBody)
            call.respond(result)
        } catch (e: Exception) {
            logger.error("Failed to forward request to Kuaidi100", e)
            call.respond(mapOf(
                "success" to false,
                "message" to (e.message ?: "后端服务异常"))
            )
        }
    }
}
