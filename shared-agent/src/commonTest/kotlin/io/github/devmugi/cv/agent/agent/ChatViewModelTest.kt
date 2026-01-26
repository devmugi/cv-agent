package io.github.devmugi.cv.agent.agent

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.GroqApiException
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.career.models.SkillCategory
import io.github.devmugi.cv.agent.domain.models.ChatError
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.models.MessageRole
import io.github.devmugi.cv.agent.domain.models.defaultSuggestions
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val testPersonalInfo = PersonalInfo(
        name = "Test Name",
        title = "Test Title",
        location = "Test Location",
        email = "test@test.com",
        linkedin = "https://linkedin.com",
        github = "https://github.com",
        portfolio = "https://portfolio.com",
        summary = "Test summary",
        skills = listOf(SkillCategory("Category", listOf("Skill1", "Skill2")))
    )

    private val testProject = CareerProject(
        id = "test-project",
        name = "Test Project",
        slug = "test-project"
    )

    private val testDataProvider = AgentDataProvider(
        personalInfo = testPersonalInfo,
        allProjects = listOf(testProject),
        featuredProjectIds = emptyList()
    )

    private lateinit var fakeApiClient: FakeGroqApiClient
    private lateinit var fakeAnalytics: FakeAnalytics
    private lateinit var fakeChatRepository: FakeChatRepository
    private lateinit var viewModel: ChatViewModel

    @BeforeTest
    fun setup() {
        // Disable logging in tests to avoid android.util.Log dependency
        Logger.setMinSeverity(Severity.Assert)

        Dispatchers.setMain(testDispatcher)
        fakeApiClient = FakeGroqApiClient()
        fakeAnalytics = FakeAnalytics()
        fakeChatRepository = FakeChatRepository()
        viewModel = ChatViewModel(
            apiClient = fakeApiClient,
            promptBuilder = SystemPromptBuilder(),
            suggestionExtractor = SuggestionExtractor(),
            dataProvider = null,
            chatRepository = fakeChatRepository,
            analytics = fakeAnalytics
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateHasSuggestions() {
        assertEquals(defaultSuggestions, viewModel.state.value.suggestions)
        assertTrue(viewModel.state.value.messages.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun sendMessageAddsUserMessage() = runTest {
        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        val messages = viewModel.state.value.messages
        assertTrue(messages.any { it.role == MessageRole.USER && it.content == "Hello" })
    }

    @Test
    fun sendMessageTriggersLoading() = runTest {
        fakeApiClient.delayResponse = true
        viewModel.sendMessage("Hello")

        testDispatcher.scheduler.advanceTimeBy(50)
        assertTrue(viewModel.state.value.isLoading || viewModel.state.value.isStreaming)
    }

    @Test
    fun successfulResponseAddsAssistantMessage() = runTest {
        fakeApiClient.responseChunks = listOf("Hello", " there")
        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        val messages = viewModel.state.value.messages
        val assistantMsg = messages.find { it.role == MessageRole.ASSISTANT }
        assertNotNull(assistantMsg)
        assertEquals("Hello there", assistantMsg.content)
    }

    @Test
    fun errorStateSetsError() = runTest {
        fakeApiClient.shouldFail = GroqApiException.NetworkError("Connection failed")
        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.error is ChatError.Network)
    }

    @Test
    fun clearErrorRemovesError() = runTest {
        fakeApiClient.shouldFail = GroqApiException.NetworkError("Test")
        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        viewModel.clearError()
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun onSuggestionClickedSendsMessage() = runTest {
        val suggestion = "What's Denys's experience?"
        viewModel.onSuggestionClicked(suggestion)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.messages.any { it.content == suggestion })
    }

    @Test
    fun retryResendsLastUserMessage() = runTest {
        fakeApiClient.shouldFail = GroqApiException.NetworkError("First attempt failed")
        viewModel.sendMessage("Original question")
        advanceUntilIdle()

        // Clear the error and fix the API
        fakeApiClient.shouldFail = null
        fakeApiClient.responseChunks = listOf("Success response")
        viewModel.retry()
        advanceUntilIdle()

        val messages = viewModel.state.value.messages
        val userMessages = messages.filter { it.role == MessageRole.USER }
        assertEquals(2, userMessages.size)
        assertTrue(userMessages.all { it.content == "Original question" })
    }

    @Test
    fun retryDoesNothingWhenNoPreviousMessage() = runTest {
        val initialState = viewModel.state.value
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(initialState.messages, viewModel.state.value.messages)
    }

    @Test
    fun rateLimitErrorMappedToChatErrorRateLimit() = runTest {
        fakeApiClient.shouldFail = GroqApiException.RateLimitError(retryAfter = 30)
        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.error is ChatError.RateLimit)
    }

    @Test
    fun authErrorMappedToChatErrorApi() = runTest {
        fakeApiClient.shouldFail = GroqApiException.AuthError(code = 401)
        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        val error = viewModel.state.value.error
        assertTrue(error is ChatError.Api)
        assertEquals("Authentication failed", (error as ChatError.Api).message)
    }

    @Test
    fun apiErrorMappedToChatErrorApi() = runTest {
        fakeApiClient.shouldFail = GroqApiException.ApiError(code = 500, message = "Server error")
        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        val error = viewModel.state.value.error
        assertTrue(error is ChatError.Api)
        assertEquals("Server error", (error as ChatError.Api).message)
    }

    @Test
    fun streamingUpdatesContentProgressively() = runTest {
        fakeApiClient.responseChunks = listOf("Hello", " ", "World")
        fakeApiClient.delayBetweenChunks = true
        viewModel.sendMessage("Hi")

        // Advance partially to catch streaming state
        testDispatcher.scheduler.advanceTimeBy(15)
        assertTrue(viewModel.state.value.isStreaming)

        // Check that streamingMessageId is set and the message exists with partial content
        val streamingId = viewModel.state.value.streamingMessageId
        assertNotNull(streamingId)
        val streamingMessage = viewModel.state.value.messages.find { it.id == streamingId }
        assertNotNull(streamingMessage)
        assertTrue(streamingMessage.content.isNotEmpty())

        advanceUntilIdle()
        assertFalse(viewModel.state.value.isStreaming)
        assertEquals(null, viewModel.state.value.streamingMessageId)
    }

    @Test
    fun messageHistoryTruncatedToMaxHistory() = runTest {
        // Send 12 messages to exceed MAX_HISTORY of 10
        repeat(12) { i ->
            fakeApiClient.responseChunks = listOf("Response $i")
            viewModel.sendMessage("Message $i")
            advanceUntilIdle()
        }

        // The last API call should only include the last 10 messages (not all 24)
        // capturedMessages includes system prompt + conversation messages
        val conversationMessages = fakeApiClient.capturedMessages.filter { it.role != "system" }
        assertTrue(conversationMessages.size <= 10, "Expected at most 10 messages, got ${conversationMessages.size}")
    }

    @Test
    fun systemPromptIncludedWhenDataProviderAvailable() = runTest {
        val viewModelWithData = ChatViewModel(
            apiClient = fakeApiClient,
            promptBuilder = SystemPromptBuilder(),
            suggestionExtractor = SuggestionExtractor(),
            dataProvider = testDataProvider,
            chatRepository = FakeChatRepository()
        )

        viewModelWithData.sendMessage("Hi")
        advanceUntilIdle()

        val systemMessage = fakeApiClient.capturedMessages.find { it.role == "system" }
        assertNotNull(systemMessage)
        assertTrue(systemMessage.content.contains("Test Name"))
    }

    @Test
    fun suggestionsExtractedFromResponse() = runTest {
        fakeApiClient.responseChunks = listOf(
            "Here is some info.\n\n```json\n{\"suggestions\": [\"test-project\"]}\n```"
        )
        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        val assistantMsg = viewModel.state.value.messages.find { it.role == MessageRole.ASSISTANT }
        assertNotNull(assistantMsg)
        assertEquals("Here is some info.", assistantMsg.content.trim())
        assertEquals(listOf("test-project"), assistantMsg.suggestions)
    }

    // ============ Analytics Tests ============

    @Test
    fun sendMessageLogsMessageSentEvent() = runTest {
        viewModel.sendMessage("Hello world")
        advanceUntilIdle()

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.MessageSent>()
        assertNotNull(event, "MessageSent event should be logged")
        assertEquals(11, event.messageLength)
        assertEquals(1, event.turnNumber)
    }

    @Test
    fun clearHistoryLogsHistoryClearedEvent() = runTest {
        // Send a message first to have history
        viewModel.sendMessage("Test message")
        advanceUntilIdle()
        fakeAnalytics.clear()

        viewModel.clearHistory()

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.HistoryCleared>()
        assertNotNull(event, "HistoryCleared event should be logged")
        assertTrue(event.messageCount >= 1)
    }

    @Test
    fun streamResponseLogsResponseCompletedEvent() = runTest {
        fakeApiClient.responseChunks = listOf("Hello", " world")

        viewModel.sendMessage("Test")
        advanceUntilIdle()

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.ResponseCompleted>()
        assertNotNull(event, "ResponseCompleted event should be logged")
        assertTrue(event.responseTimeMs >= 0)
    }

    @Test
    fun streamResponseErrorLogsErrorDisplayedEvent() = runTest {
        fakeApiClient.shouldFail = GroqApiException.NetworkError("Test error")

        viewModel.sendMessage("Test")
        advanceUntilIdle()

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Error.ErrorDisplayed>()
        assertNotNull(event, "ErrorDisplayed event should be logged")
        assertEquals(AnalyticsEvent.Error.ErrorType.NETWORK, event.errorType)
    }

    @Test
    fun onMessageCopiedLogsEvent() = runTest {
        fakeApiClient.responseChunks = listOf("Response content here")
        viewModel.sendMessage("Test")
        advanceUntilIdle()

        val assistantMessage = viewModel.state.value.messages.find { it.role == MessageRole.ASSISTANT }
        assertNotNull(assistantMessage)
        fakeAnalytics.clear()

        viewModel.onMessageCopied(assistantMessage.id)

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.MessageCopied>()
        assertNotNull(event, "MessageCopied event should be logged")
        assertEquals(assistantMessage.id, event.messageId)
    }

    @Test
    fun onMessageLikedLogsEvent() = runTest {
        viewModel.onMessageLiked("test-message-id")

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.MessageLiked>()
        assertNotNull(event)
        assertEquals("test-message-id", event.messageId)
    }

    @Test
    fun onMessageDislikedLogsEvent() = runTest {
        viewModel.onMessageDisliked("test-message-id")

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.MessageDisliked>()
        assertNotNull(event)
        assertEquals("test-message-id", event.messageId)
    }

    @Test
    fun onRegenerateClickedLogsEventAndRetries() = runTest {
        fakeApiClient.responseChunks = listOf("First response")
        viewModel.sendMessage("Test")
        advanceUntilIdle()
        fakeAnalytics.clear()

        viewModel.onRegenerateClicked("test-id")
        advanceUntilIdle()

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.RegenerateClicked>()
        assertNotNull(event)
    }

    @Test
    fun onProjectSuggestionClickedLogsEvent() = runTest {
        viewModel.onProjectSuggestionClicked("mcdonalds", 0)

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.SuggestionClicked>()
        assertNotNull(event)
        assertEquals("mcdonalds", event.projectId)
        assertEquals(0, event.position)
    }

    // ============ Repository Persistence Tests ============

    @Test
    fun sendMessageSavesMessagesToRepository() = runTest {
        fakeApiClient.responseChunks = listOf("Response")
        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        assertTrue(fakeChatRepository.saveMessagesCalled)
        val savedMessages = fakeChatRepository.getMessages()
        assertEquals(2, savedMessages.size) // user + assistant
    }

    @Test
    fun clearHistoryClearsRepository() = runTest {
        fakeApiClient.responseChunks = listOf("Response")
        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        viewModel.clearHistory()
        advanceUntilIdle()

        assertTrue(fakeChatRepository.clearAllCalled)
    }

    @Test
    fun viewModelRestoresMessagesFromRepository() = runTest {
        val preloadedMessages = listOf(
            Message(role = MessageRole.USER, content = "Restored message"),
            Message(role = MessageRole.ASSISTANT, content = "Restored response")
        )
        fakeChatRepository.preloadMessages(preloadedMessages)

        // Create new ViewModel that should load from repository
        val restoredViewModel = ChatViewModel(
            apiClient = fakeApiClient,
            promptBuilder = SystemPromptBuilder(),
            suggestionExtractor = SuggestionExtractor(),
            dataProvider = null,
            chatRepository = fakeChatRepository,
            analytics = fakeAnalytics
        )
        advanceUntilIdle()

        assertEquals(2, restoredViewModel.state.value.messages.size)
        assertEquals("Restored message", restoredViewModel.state.value.messages[0].content)
    }

    @Test
    fun viewModelRestoresSessionFromRepository() = runTest {
        fakeChatRepository.preloadSession(sessionId = "test-session-123", turnNumber = 5)

        val restoredViewModel = ChatViewModel(
            apiClient = fakeApiClient,
            promptBuilder = SystemPromptBuilder(),
            suggestionExtractor = SuggestionExtractor(),
            dataProvider = null,
            chatRepository = fakeChatRepository,
            analytics = fakeAnalytics
        )
        advanceUntilIdle()

        // Send a message and verify turnNumber continues from restored value
        fakeApiClient.responseChunks = listOf("Response")
        restoredViewModel.sendMessage("New message")
        advanceUntilIdle()

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.MessageSent>()
        assertNotNull(event)
        assertEquals(6, event.turnNumber) // Should be 5 + 1
    }
}

// Test doubles
class FakeGroqApiClient : GroqApiClient(
    httpClient = HttpClient(MockEngine) { engine { addHandler { respond("") } } },
    apiKey = "fake",
    rateLimiter = io.github.devmugi.cv.agent.api.RateLimiter.NOOP
) {
    var responseChunks: List<String> = listOf("Test response")
    var shouldFail: GroqApiException? = null
    var delayResponse = false
    var delayBetweenChunks = false
    var capturedMessages: List<ChatMessage> = emptyList()

    override suspend fun streamChatCompletion(
        messages: List<ChatMessage>,
        systemPrompt: String,
        sessionId: String?,
        turnNumber: Int?,
        promptVersion: String?,
        promptVariant: String?,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (GroqApiException) -> Unit
    ) {
        // Match real GroqApiClient behavior: prepend system prompt to messages
        capturedMessages = if (systemPrompt.isNotEmpty()) {
            listOf(ChatMessage(role = "system", content = systemPrompt)) + messages
        } else {
            messages
        }
        if (delayResponse) {
            kotlinx.coroutines.delay(100)
        }
        shouldFail?.let {
            onError(it)
            return
        }
        responseChunks.forEach { chunk ->
            if (delayBetweenChunks) {
                kotlinx.coroutines.delay(10)
            }
            onChunk(chunk)
        }
        onComplete()
    }
}

class FakeAnalytics : Analytics {
    val loggedEvents = mutableListOf<AnalyticsEvent>()

    override fun logEvent(event: AnalyticsEvent) {
        loggedEvents.add(event)
    }

    override fun setUserId(userId: String?) {}
    override fun setUserProperty(name: String, value: String?) {}
    override fun setCurrentScreen(screenName: String, screenClass: String?) {}

    fun clear() = loggedEvents.clear()

    inline fun <reified T : AnalyticsEvent> findEvent(): T? =
        loggedEvents.filterIsInstance<T>().firstOrNull()

    inline fun <reified T : AnalyticsEvent> hasEvent(): Boolean =
        loggedEvents.any { it is T }
}
