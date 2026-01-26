package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.repository.ChatRepository

class FakeChatRepository : ChatRepository {
    private var messages = emptyList<Message>()
    private var sessionId: String? = null
    private var turnNumber = 0

    // For test assertions
    var saveMessagesCalled = false
    var clearAllCalled = false

    override suspend fun getMessages(): List<Message> = messages

    override suspend fun saveMessages(messages: List<Message>) {
        this.messages = messages
        saveMessagesCalled = true
    }

    override suspend fun clearMessages() {
        messages = emptyList()
    }

    override suspend fun getSessionId(): String? = sessionId

    override suspend fun saveSessionId(sessionId: String) {
        this.sessionId = sessionId
    }

    override suspend fun getTurnNumber(): Int = turnNumber

    override suspend fun saveTurnNumber(turnNumber: Int) {
        this.turnNumber = turnNumber
    }

    override suspend fun clearAll() {
        messages = emptyList()
        sessionId = null
        turnNumber = 0
        clearAllCalled = true
    }

    // Test helpers
    fun preloadMessages(messages: List<Message>) {
        this.messages = messages
    }

    fun preloadSession(sessionId: String, turnNumber: Int) {
        this.sessionId = sessionId
        this.turnNumber = turnNumber
    }

    fun reset() {
        messages = emptyList()
        sessionId = null
        turnNumber = 0
        saveMessagesCalled = false
        clearAllCalled = false
    }
}
