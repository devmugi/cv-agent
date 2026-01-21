package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.agent.MessageRole
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageBubbleTest {

    @Test
    fun userMessageAlignmentIsEnd() {
        val alignment = getMessageAlignment(MessageRole.USER)
        assertEquals(MessageAlignment.END, alignment)
    }

    @Test
    fun assistantMessageAlignmentIsStart() {
        val alignment = getMessageAlignment(MessageRole.ASSISTANT)
        assertEquals(MessageAlignment.START, alignment)
    }

    @Test
    fun userMessageUsesRoundedTopRightSmall() {
        val shape = getMessageCornerShape(MessageRole.USER)
        assertEquals(MessageCornerShape.TOP_RIGHT_SMALL, shape)
    }

    @Test
    fun assistantMessageUsesRoundedTopLeftSmall() {
        val shape = getMessageCornerShape(MessageRole.ASSISTANT)
        assertEquals(MessageCornerShape.TOP_LEFT_SMALL, shape)
    }
}
