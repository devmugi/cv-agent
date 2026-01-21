package io.github.devmugi.cv.agent.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.GroqApiException
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.domain.models.CVData
import io.github.devmugi.cv.agent.domain.models.ChatError
import io.github.devmugi.cv.agent.domain.models.ChatState
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.models.MessageRole
import io.github.devmugi.cv.agent.data.repository.CVRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val apiClient: GroqApiClient,
    private val repository: CVRepository,
    private val promptBuilder: SystemPromptBuilder,
    private val referenceExtractor: ReferenceExtractor,
    private val cvDataProvider: () -> CVData? = { null }
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var lastUserMessage: String? = null

    companion object {
        private const val MAX_HISTORY = 10
    }

    fun sendMessage(content: String) {
        lastUserMessage = content
        val userMessage = Message(role = MessageRole.USER, content = content)

        _state.update { current ->
            current.copy(
                messages = current.messages + userMessage,
                isLoading = true,
                error = null,
                suggestions = emptyList()
            )
        }

        viewModelScope.launch {
            streamResponse()
        }
    }

    fun onSuggestionClicked(suggestion: String) {
        sendMessage(suggestion)
    }

    fun retry() {
        lastUserMessage?.let { sendMessage(it) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun streamResponse() {
        val cvData = cvDataProvider()
        val systemPrompt = cvData?.let { promptBuilder.build(it) } ?: ""

        val apiMessages = buildApiMessages(systemPrompt)
        var streamedContent = ""

        _state.update { it.copy(isLoading = false, isStreaming = true, streamingContent = "") }

        apiClient.streamChatCompletion(
            messages = apiMessages,
            onChunk = { chunk ->
                streamedContent += chunk
                _state.update { it.copy(streamingContent = streamedContent) }
            },
            onComplete = {
                val extractionResult = referenceExtractor.extract(streamedContent)
                val assistantMessage = Message(
                    role = MessageRole.ASSISTANT,
                    content = extractionResult.cleanedContent,
                    references = extractionResult.references
                )
                _state.update { current ->
                    current.copy(
                        messages = current.messages + assistantMessage,
                        isStreaming = false,
                        streamingContent = ""
                    )
                }
            },
            onError = { exception ->
                val error = when (exception) {
                    is GroqApiException.NetworkError -> ChatError.Network(exception.reason)
                    is GroqApiException.RateLimitError -> ChatError.RateLimit
                    is GroqApiException.AuthError -> ChatError.Api("Authentication failed")
                    is GroqApiException.ApiError -> ChatError.Api(exception.message)
                }
                _state.update { it.copy(isLoading = false, isStreaming = false, error = error) }
            }
        )
    }

    private fun buildApiMessages(systemPrompt: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        if (systemPrompt.isNotEmpty()) {
            messages.add(ChatMessage(role = "system", content = systemPrompt))
        }

        val recentMessages = _state.value.messages.takeLast(MAX_HISTORY)
        recentMessages.forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            messages.add(ChatMessage(role = role, content = msg.content))
        }

        return messages
    }
}
