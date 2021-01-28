package com.epam.drill.autotest.helper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class WsMessage(
    val type: EventType,
    val from: Client,
    val payload: JsonElement = JsonPrimitive("")
)

@Serializable
enum class EventType {
    START_TEST, FINISH_TEST, CONNECT, READY
}

@Serializable
data class Client(
    val id: String,
    val type: String
)
