package com.pickuphelper

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.serialization.gson.*
import com.pickuphelper.routes.pickupCodeRoute

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) { gson { setPrettyPrinting() } }
        routing { pickupCodeRoute() }
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { gson { setPrettyPrinting() } }
    routing { pickupCodeRoute() }
}
