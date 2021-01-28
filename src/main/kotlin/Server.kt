package com.epam.drill.autotest.helper

import io.ktor.http.cio.websocket.*
import kotlinx.atomicfu.*
import kotlinx.collections.immutable.*

const val SOCKET_NAME = "autotest-extension-dispatcher"

class DispatchingServer {
    private val _sessions = atomic(persistentMapOf<String, PersistentSet<WebSocketSession>>())
    private val _agentSessions = atomic(persistentMapOf<String, WebSocketSession>())

    fun addAgentSession(id: String, session: WebSocketSession) = _agentSessions.update {
        logger.info { "Autotest agent with id $id was registered" }
        it.put(id, session)
    }

    fun addExtensionSession(id: String, session: WebSocketSession) {
        val curSessions = _sessions.value[id] ?: persistentSetOf()
        if (session !in curSessions) {
            _sessions.update {
                logger.info { "Extension was connected to agent with id $id" }
                it.put(id, curSessions + session)
            }
        }
    }

    suspend fun disconnect(session: WebSocketSession) {
        _agentSessions.update { sessions ->
            sessions.filterNot { it.value == session }.toPersistentMap()
        }
        _sessions.value.filter { session in it.value }.map { (id, sessions) ->
            _sessions.update { it.put(id, sessions - session) }
        }
        session.close()
        logger.info { "Client was disconnected" }

    }

    suspend fun sendToExtension(id: String, messageStr: String) = _sessions.value[id]?.let { sessions ->
        sessions.forEach { it.send(messageStr) }
        logger.debug { "($id) - Autotest agent message: $messageStr" }
    } ?: logger.warn { "No connections with extension with such id $id" }

    suspend fun sendToAgent(
        id: String,
        messageStr: String
    ) = _agentSessions.value[id]?.let { session ->
        session.send(messageStr)
        logger.debug { "($id) - Extension message: $messageStr" }
    } ?: logger.warn { "No connections to agent with such id $id" }

}

