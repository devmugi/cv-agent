package io.github.devmugi.cv.agent.api.models

import kotlinx.serialization.Serializable

@Serializable
data class StreamChunk(
    val choices: List<StreamChoice>
)

@Serializable
data class StreamChoice(
    val delta: StreamDelta,
    val index: Int
)

@Serializable
data class StreamDelta(
    val content: String? = null,
    val role: String? = null
)
