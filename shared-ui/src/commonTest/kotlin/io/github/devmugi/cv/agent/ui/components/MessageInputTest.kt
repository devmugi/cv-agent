package io.github.devmugi.cv.agent.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageInputTest {

    @Test
    fun sendButtonEnabledWhenTextNotEmpty() {
        val enabled = isSendButtonEnabled(text = "Hello", isLoading = false)
        assertTrue(enabled)
    }

    @Test
    fun sendButtonDisabledWhenTextEmpty() {
        val enabled = isSendButtonEnabled(text = "", isLoading = false)
        assertFalse(enabled)
    }

    @Test
    fun sendButtonDisabledWhenLoading() {
        val enabled = isSendButtonEnabled(text = "Hello", isLoading = true)
        assertFalse(enabled)
    }

    @Test
    fun sendButtonDisabledWhenTextIsBlank() {
        val enabled = isSendButtonEnabled(text = "   ", isLoading = false)
        assertFalse(enabled)
    }
}
