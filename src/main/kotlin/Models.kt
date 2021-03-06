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
