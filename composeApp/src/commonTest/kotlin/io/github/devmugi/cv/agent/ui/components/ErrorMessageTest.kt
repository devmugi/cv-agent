package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.agent.ChatError
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessageTest {

    @Test
    fun networkErrorDisplaysCorrectMessage() {
        val error = ChatError.Network("Connection failed")
        val message = getErrorDisplayMessage(error)
        assertEquals("Connection failed", message)
    }

    @Test
    fun rateLimitErrorDisplaysUserFriendlyMessage() {
        val error = ChatError.RateLimit
        val message = getErrorDisplayMessage(error)
        assertEquals("Too many requests. Please wait a moment.", message)
    }

    @Test
    fun apiErrorDisplaysMessage() {
        val error = ChatError.Api("Server error")
        val message = getErrorDisplayMessage(error)
        assertEquals("Server error", message)
    }
}
