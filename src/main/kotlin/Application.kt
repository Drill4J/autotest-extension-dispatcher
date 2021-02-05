/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.autotest.helper

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*
import mu.*
import java.time.*

val logger = KotlinLogging.logger { DispatchingServer::class.java }
val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Suppress("unused")
fun Application.module() = run {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(150)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket(DispatchingServer())
    }
}

fun Route.webSocket(server: DispatchingServer) = webSocket("/") {
    val session = this
    try {
        logger.info { "Client was connected" }
        incoming.consumeEach { frame ->
            val message = (frame as Frame.Text).readText()
            val event = json.decodeFromString(WsMessage.serializer(), message)
            val client = event.from
            val id = client.id
            when (client.type) {
                "autotest-agent" -> {
                    if (event.type == EventType.CONNECT) {
                        server.addAgentSession(id, session)
                    } else {
                        server.sendToExtension(id, message)
                    }
                }
                "extension" -> {
                    server.addExtensionSession(id, session)
                    server.sendToAgent(id, message)
                }
                else -> logger.debug { "Not recognized message: $message" }
            }
        }
    } catch (e: Exception) {
        when (e) {
            is CancellationException -> logger.debug {
                "$SOCKET_NAME: ${session.toDebugString()} was cancelled."
            }
            else -> logger.error(e) {
                "$SOCKET_NAME: ${session.toDebugString()} finished with exception."
            }
        }
    } finally {
        server.disconnect(session)
    }
}

private fun DefaultWebSocketServerSession.toDebugString() = "session(${hashCode()})"
