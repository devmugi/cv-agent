package io.github.devmugi.cv.agent.api.models

import kotlinx.serialization.Serializable

@Serializable
data class StreamChunk(
    val choices: List<StreamChoice>,
    val usage: Usage? = null
)

@Serializable
data class Usage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
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
