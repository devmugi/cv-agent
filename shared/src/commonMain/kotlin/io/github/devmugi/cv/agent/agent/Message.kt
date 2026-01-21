package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.data.models.CVReference
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

@OptIn(ExperimentalUuidApi::class)
data class Message(
    val id: String = Uuid.random().toString(),
    val role: MessageRole,
    val content: String,
    val references: List<CVReference> = emptyList(),
    val timestamp: Long = currentTimeMillis()
)

expect fun currentTimeMillis(): Long
