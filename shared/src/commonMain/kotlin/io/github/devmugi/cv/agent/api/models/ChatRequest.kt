package io.github.devmugi.cv.agent.api.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @EncodeDefault
    val stream: Boolean = true,
    @EncodeDefault
    val temperature: Double = 0.7,
    @EncodeDefault
    @SerialName("max_tokens")
    val maxTokens: Int = 1024
)
