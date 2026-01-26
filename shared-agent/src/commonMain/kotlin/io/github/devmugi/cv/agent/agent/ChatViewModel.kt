package io.github.devmugi.cv.agent.agent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.GroqApiException
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.api.tracing.PromptMetadata
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
import io.github.devmugi.cv.agent.domain.models.ChatError
import io.github.devmugi.cv.agent.domain.models.ChatState
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.models.MessageRole
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatViewModel(
    private val savedStateHandle: SavedStateHandle? = null,
    private val apiClient: GroqApiClient,
    private val promptBuilder: SystemPromptBuilder,
    private val suggestionExtractor: SuggestionExtractor,
    private val dataProvider: AgentDataProvider?,
    private val analytics: Analytics = Analytics.NOOP,
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val MAX_HISTORY = 10
        private const val KEY_MESSAGES = "chat_messages"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_TURN_NUMBER = "turn_number"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(
        ChatState(
            messages = restoreMessages(),
            projectNames = dataProvider?.getProjectIndex()
                ?.associate { it.id to it.name }
                ?: emptyMap()
        )
    )
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var lastUserMessage: String? = null
    @OptIn(ExperimentalUuidApi::class)
    private var sessionId: String = savedStateHandle?.get<String>(KEY_SESSION_ID) ?: Uuid.random().toString()
    private var turnNumber: Int = savedStateHandle?.get<Int>(KEY_TURN_NUMBER) ?: 0

    init {
        // Save session info on init if restored
        savedStateHandle?.let {
            it[KEY_SESSION_ID] = sessionId
            it[KEY_TURN_NUMBER] = turnNumber
        }
    }

    private fun restoreMessages(): List<Message> {
        val messagesJson = savedStateHandle?.get<String>(KEY_MESSAGES) ?: return emptyList()
        return try {
            json.decodeFromString<List<Message>>(messagesJson)
        } catch (e: Exception) {
            Logger.w(TAG) { "Failed to restore messages: ${e.message}" }
            emptyList()
        }
    }

    private fun saveMessages(messages: List<Message>) {
        savedStateHandle ?: return
        try {
            savedStateHandle[KEY_MESSAGES] = json.encodeToString(messages)
        } catch (e: Exception) {
            Logger.w(TAG) { "Failed to save messages: ${e.message}" }
        }
    }

    private fun saveSessionState() {
        savedStateHandle?.let {
            it[KEY_SESSION_ID] = sessionId
            it[KEY_TURN_NUMBER] = turnNumber
        }
    }

    fun sendMessage(content: String) {
        Logger.d(TAG) { "Sending message - length: ${content.length}" }
        lastUserMessage = content
        val userMessage = Message(role = MessageRole.USER, content = content)

        _state.update { current ->
            val newMessages = current.messages + userMessage
            saveMessages(newMessages)
            current.copy(
                messages = newMessages,
                isLoading = true,
                thinkingStatus = "Crafting personalized response.",
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

    @OptIn(ExperimentalUuidApi::class)
    fun clearHistory() {
        Logger.d(TAG) { "Clearing chat history" }
        _state.update { current -> ChatState(projectNames = current.projectNames) }
        lastUserMessage = null
        sessionId = Uuid.random().toString()
        turnNumber = 0
        // Clear saved state
        savedStateHandle?.remove<String>(KEY_MESSAGES)
        saveSessionState()
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun streamResponse() {
        turnNumber++
        val currentTurn = turnNumber
        val currentSessionId = sessionId
        Logger.d(TAG) { "Starting turn $currentTurn in session $currentSessionId" }

        val promptResult = dataProvider?.let { promptBuilder.buildWithMetadata(it) }
        val systemPrompt = promptResult?.prompt ?: ""
        val promptMetadata = promptResult?.let { PromptMetadata(it.version, it.variant) }
        val apiMessages = buildApiMessages(systemPrompt)
        val assistantMessageId = Uuid.random().toString()

        val assistantMessage = Message(
            id = assistantMessageId,
            role = MessageRole.ASSISTANT,
            content = "",
            suggestions = emptyList()
        )

        _state.update { current ->
            current.copy(
                messages = current.messages + assistantMessage,
                isLoading = false,
                isStreaming = true,
                streamingMessageId = assistantMessageId,
                thinkingStatus = null
            )
        }

        var streamedContent = ""

        apiClient.streamChatCompletion(
            messages = apiMessages,
            systemPrompt = systemPrompt,
            sessionId = currentSessionId,
            turnNumber = currentTurn,
            promptMetadata = promptMetadata,
            onChunk = { chunk ->
                Logger.d(TAG) { "ON_CHUNK: ${chunk.length} chars at ${System.currentTimeMillis()}" }
                streamedContent += chunk
                _state.update { current ->
                    current.copy(
                        messages = current.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(content = streamedContent)
                            } else {
                                msg
                            }
                        }
                    )
                }
            },
            onComplete = {
                Logger.d(TAG) { "Stream completed - content length: ${streamedContent.length}" }
                val extractionResult = suggestionExtractor.extract(streamedContent)
                _state.update { current ->
                    val newMessages = current.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(
                                content = extractionResult.cleanedContent,
                                suggestions = extractionResult.suggestions
                            )
                        } else {
                            msg
                        }
                    }
                    saveMessages(newMessages)
                    saveSessionState()
                    current.copy(
                        messages = newMessages,
                        isStreaming = false,
                        streamingMessageId = null
                    )
                }
            },
            onError = { exception ->
                Logger.w(TAG) { "Stream error: ${exception.javaClass.simpleName}" }
                val error = when (exception) {
                    is GroqApiException.NetworkError -> ChatError.Network(exception.reason)
                    is GroqApiException.RateLimitError -> ChatError.RateLimit
                    is GroqApiException.AuthError -> ChatError.Api("Authentication failed")
                    is GroqApiException.ApiError -> ChatError.Api(exception.message)
                }
                _state.update { current ->
                    val newMessages = current.messages.filter { it.id != assistantMessageId }
                    saveMessages(newMessages)
                    current.copy(
                        messages = newMessages,
                        isLoading = false,
                        isStreaming = false,
                        streamingMessageId = null,
                        error = error
                    )
                }
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
