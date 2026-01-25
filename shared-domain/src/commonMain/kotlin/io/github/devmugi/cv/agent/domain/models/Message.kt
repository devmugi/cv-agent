package io.github.devmugi.cv.agent.domain.models

import io.github.devmugi.cv.agent.domain.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class Message(
    val id: String = Uuid.random().toString(),
    val role: MessageRole,
    val content: String,
    val suggestions: List<String> = emptyList(),
    val timestamp: Long = currentTimeMillis()
)
