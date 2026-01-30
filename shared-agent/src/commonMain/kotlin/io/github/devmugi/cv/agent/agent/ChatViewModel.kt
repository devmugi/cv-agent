package io.github.devmugi.cv.agent.agent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.devmugi.arize.tracing.ArizeTracer
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.GroqApiException
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
import io.github.devmugi.cv.agent.domain.currentTimeMillis
import io.github.devmugi.cv.agent.domain.models.ChatError
import io.github.devmugi.cv.agent.domain.models.ChatState
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.models.MessageRole
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
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
    private val chatRepository: ChatRepository? = null,
    private val analytics: Analytics = Analytics.NOOP,
    private val tracer: ArizeTracer = ArizeTracer.NOOP,
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val MAX_HISTORY = 10
        private const val KEY_MESSAGES = "chat_messages"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_TURN_NUMBER = "turn_number"
        private const val KEY_HAS_EVER_OPENED = "has_ever_opened"
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
        // Load from repository if available (async)
        viewModelScope.launch {
            chatRepository?.let { repo ->
                val repoMessages = repo.getMessages()
                val repoSessionId = repo.getSessionId()
                val repoTurnNumber = repo.getTurnNumber()

                if (repoMessages.isNotEmpty()) {
                    _state.update { it.copy(messages = repoMessages) }
                }
                repoSessionId?.let { sessionId = it }
                if (repoTurnNumber > 0) {
                    turnNumber = repoTurnNumber
                }
            }

            // Track session start/resume
            val currentMessages = _state.value.messages
            val isFirstEverOpen = savedStateHandle?.get<Boolean>(KEY_HAS_EVER_OPENED) != true

            // Mark as opened (persists even after clearHistory)
            savedStateHandle?.set(KEY_HAS_EVER_OPENED, true)

            if (currentMessages.isNotEmpty()) {
                analytics.logEvent(
                    AnalyticsEvent.Session.SessionResume(
                        sessionId = sessionId,
                        messageCount = currentMessages.size
                    )
                )
            } else {
                analytics.logEvent(
                    AnalyticsEvent.Session.SessionStart(
                        sessionId = sessionId,
                        isNewInstall = isFirstEverOpen
                    )
                )
            }

            // Save session info
            savedStateHandle?.let {
                it[KEY_SESSION_ID] = sessionId
                it[KEY_TURN_NUMBER] = turnNumber
            }
        }
    }

    private fun restoreMessages(): List<Message> {
        // Repository-based restore happens in init block via coroutine
        // This synchronous version is fallback for SavedStateHandle only
        val messagesJson = savedStateHandle?.get<String>(KEY_MESSAGES) ?: return emptyList()
        return try {
            json.decodeFromString<List<Message>>(messagesJson)
        } catch (e: Exception) {
            Logger.w(TAG) { "Failed to restore messages from SavedStateHandle: ${e.message}" }
            emptyList()
        }
    }

    private fun saveMessages(messages: List<Message>) {
        // Save to SavedStateHandle (process death)
        savedStateHandle?.let {
            try {
                it[KEY_MESSAGES] = json.encodeToString(messages)
            } catch (e: Exception) {
                Logger.w(TAG) { "Failed to save messages to SavedStateHandle: ${e.message}" }
            }
        }
        // Save to repository (app restart)
        viewModelScope.launch {
            chatRepository?.saveMessages(messages)
        }
    }

    private fun saveSessionState() {
        savedStateHandle?.let {
            it[KEY_SESSION_ID] = sessionId
            it[KEY_TURN_NUMBER] = turnNumber
        }
        viewModelScope.launch {
            chatRepository?.saveSessionId(sessionId)
            chatRepository?.saveTurnNumber(turnNumber)
        }
    }

    fun sendMessage(content: String) {
        Logger.d(TAG) { "Sending message - length: ${content.length}" }
        lastUserMessage = content

        // Log analytics event
        analytics.logEvent(
            AnalyticsEvent.Chat.MessageSent(
                messageLength = content.length,
                sessionId = sessionId,
                turnNumber = turnNumber + 1
            )
        )

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
        val messageCount = _state.value.messages.size
        Logger.d(TAG) { "Clearing chat history" }

        // Log analytics event
        analytics.logEvent(
            AnalyticsEvent.Chat.HistoryCleared(
                messageCount = messageCount,
                sessionId = sessionId
            )
        )

        _state.update { current -> ChatState(projectNames = current.projectNames) }
        lastUserMessage = null
        sessionId = Uuid.random().toString()
        turnNumber = 0
        // Clear saved state
        savedStateHandle?.remove<String>(KEY_MESSAGES)
        viewModelScope.launch {
            chatRepository?.clearAll()
        }
        saveSessionState()
    }

    // ============ Analytics Callback Wrappers ============

    fun onMessageCopied(messageId: String) {
        val message = _state.value.messages.find { it.id == messageId }
        analytics.logEvent(
            AnalyticsEvent.Chat.MessageCopied(
                messageId = messageId,
                messageLength = message?.content?.length ?: 0
            )
        )
    }

    fun onMessageLiked(messageId: String) {
        analytics.logEvent(AnalyticsEvent.Chat.MessageLiked(messageId = messageId))
    }

    fun onMessageDisliked(messageId: String) {
        analytics.logEvent(AnalyticsEvent.Chat.MessageDisliked(messageId = messageId))
    }

    fun onRegenerateClicked(messageId: String) {
        analytics.logEvent(
            AnalyticsEvent.Chat.RegenerateClicked(
                messageId = messageId,
                turnNumber = turnNumber
            )
        )
        retry()
    }

    fun onProjectSuggestionClicked(projectId: String, position: Int) {
        analytics.logEvent(
            AnalyticsEvent.Chat.SuggestionClicked(
                projectId = projectId,
                position = position
            )
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun streamResponse() {
        turnNumber++
        val currentTurn = turnNumber
        val currentSessionId = sessionId
        val streamStartTime = currentTimeMillis()
        Logger.d(TAG) { "Starting turn $currentTurn in session $currentSessionId" }

        val promptResult = dataProvider?.let { promptBuilder.buildWithMetadata(it) }
        val systemPrompt = promptResult?.prompt ?: ""
        val apiMessages = buildApiMessages()
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

        // Start agent span to wrap the LLM call
        val agentSpan = tracer.startAgentSpan {
            name("ChatAgent")
            sessionId(currentSessionId)
        }

        agentSpan.withContext {
            apiClient.streamChatCompletion(
            messages = apiMessages,
            systemPrompt = systemPrompt,
            sessionId = currentSessionId,
            turnNumber = currentTurn,
            promptVersion = promptResult?.version,
            promptVariant = promptResult?.variant,
            onChunk = { chunk ->
                Logger.d(TAG) { "ON_CHUNK: ${chunk.length} chars at ${currentTimeMillis()}" }
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
                val responseTimeMs = currentTimeMillis() - streamStartTime
                Logger.d(TAG) { "Stream completed - content length: ${streamedContent.length}" }

                // Log analytics event
                analytics.logEvent(
                    AnalyticsEvent.Chat.ResponseCompleted(
                        responseTimeMs = responseTimeMs,
                        tokenCount = null,
                        sessionId = currentSessionId
                    )
                )

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

                // Complete the agent span
                agentSpan.complete()
            },
            onError = { exception ->
                Logger.w(TAG) { "Stream error: ${exception::class.simpleName}" }

                val errorType = when (exception) {
                    is GroqApiException.NetworkError -> AnalyticsEvent.Error.ErrorType.NETWORK
                    is GroqApiException.RateLimitError -> AnalyticsEvent.Error.ErrorType.RATE_LIMIT
                    is GroqApiException.AuthError -> AnalyticsEvent.Error.ErrorType.AUTH
                    is GroqApiException.ApiError -> AnalyticsEvent.Error.ErrorType.API
                }

                // Log analytics event
                analytics.logEvent(
                    AnalyticsEvent.Error.ErrorDisplayed(
                        errorType = errorType,
                        errorMessage = exception.message,
                        sessionId = currentSessionId
                    )
                )

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

                // Error the agent span
                agentSpan.error(exception)
            }
        )
        } // End of agentSpan.withContext
    }

    private fun buildApiMessages(): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

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
