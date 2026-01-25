package io.github.devmugi.cv.agent.domain.models

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingMessageId: String? = null,
    val thinkingStatus: String? = null,
    val error: ChatError? = null,
    val suggestions: List<String> = defaultSuggestions,
    val projectNames: Map<String, String> = emptyMap()
)

sealed class ChatError {
    data class Network(val message: String) : ChatError()
    data class Api(val message: String) : ChatError()
    data object RateLimit : ChatError()
}

val defaultSuggestions = listOf(
    "What's Denys's current role?",
    "Has he worked with Jetpack Compose?",
    "What's his Kotlin Multiplatform experience?",
    "Tell me about the McDonald's app",
    "What did he build at GEOSATIS?",
    "Tell me about the Adidas GMR project",
    "Has he trained other developers?",
    "Has Denys led teams before?"
)
