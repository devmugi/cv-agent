package io.github.devmugi.cv.agent.agent

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val error: ChatError? = null,
    val suggestions: List<String> = defaultSuggestions
)

sealed class ChatError {
    data class Network(val message: String) : ChatError()
    data class Api(val message: String) : ChatError()
    data object RateLimit : ChatError()
}

val defaultSuggestions = listOf(
    "What's Denys's experience?",
    "Tell me about his skills",
    "What projects has he worked on?",
    "What are his achievements?"
)
