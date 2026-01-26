package io.github.devmugi.cv.agent.domain.repository

import io.github.devmugi.cv.agent.domain.models.Message

interface ChatRepository {
    suspend fun getMessages(): List<Message>
    suspend fun saveMessages(messages: List<Message>)
    suspend fun clearMessages()

    suspend fun getSessionId(): String?
    suspend fun saveSessionId(sessionId: String)

    suspend fun getTurnNumber(): Int
    suspend fun saveTurnNumber(turnNumber: Int)

    suspend fun clearAll()
}
