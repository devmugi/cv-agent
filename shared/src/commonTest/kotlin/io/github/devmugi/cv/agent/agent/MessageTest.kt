package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.domain.currentTimeMillis
import io.github.devmugi.cv.agent.domain.models.CVReference
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.models.MessageRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MessageTest {

    @Test
    fun messageHasUniqueIdByDefault() {
        val msg1 = Message(role = MessageRole.USER, content = "Hello")
        val msg2 = Message(role = MessageRole.USER, content = "Hello")
        assertNotEquals(msg1.id, msg2.id)
    }

    @Test
    fun messageStoresRole() {
        val userMsg = Message(role = MessageRole.USER, content = "Hi")
        val assistantMsg = Message(role = MessageRole.ASSISTANT, content = "Hello")
        assertEquals(MessageRole.USER, userMsg.role)
        assertEquals(MessageRole.ASSISTANT, assistantMsg.role)
    }

    @Test
    fun messageStoresReferences() {
        val ref = CVReference(id = "experience.test", type = "experience", label = "Test")
        val msg = Message(
            role = MessageRole.ASSISTANT,
            content = "Test content",
            references = listOf(ref)
        )
        assertEquals(1, msg.references.size)
        assertEquals("experience.test", msg.references.first().id)
    }

    @Test
    fun messageHasTimestamp() {
        val before = currentTimeMillis()
        val msg = Message(role = MessageRole.USER, content = "Hi")
        val after = currentTimeMillis()
        assertTrue(msg.timestamp in before..after)
    }
}
