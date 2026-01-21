package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.GroqApiException
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.domain.models.CVData
import io.github.devmugi.cv.agent.domain.models.CVReference
import io.github.devmugi.cv.agent.domain.models.ChatError
import io.github.devmugi.cv.agent.domain.models.Education
import io.github.devmugi.cv.agent.domain.models.MessageRole
import io.github.devmugi.cv.agent.domain.models.PersonalInfo
import io.github.devmugi.cv.agent.domain.models.defaultSuggestions
import io.github.devmugi.cv.agent.data.repository.CVRepository
import io.ktor.client.HttpClient
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

    private val minimalCVData = CVData(
        personalInfo = PersonalInfo("Test", "Location", "email", "phone", "linkedin", "github", "portfolio"),
        summary = "Summary",
        skills = emptyList(),
        experience = emptyList(),
        projects = emptyList(),
        achievements = emptyList(),
        education = Education("BSc", "CS", "Uni")
    )

    private lateinit var fakeApiClient: FakeGroqApiClient
    private lateinit var fakeRepository: FakeCVRepository
    private lateinit var viewModel: ChatViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeApiClient = FakeGroqApiClient()
        fakeRepository = FakeCVRepository(minimalCVData)
        viewModel = ChatViewModel(
            apiClient = fakeApiClient,
            repository = fakeRepository,
            promptBuilder = SystemPromptBuilder(),
            referenceExtractor = ReferenceExtractor(fakeRepository)
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
        assertTrue(viewModel.state.value.streamingContent.isNotEmpty())

        advanceUntilIdle()
        assertFalse(viewModel.state.value.isStreaming)
        assertEquals("", viewModel.state.value.streamingContent)
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
    fun systemPromptIncludedWhenCVDataAvailable() = runTest {
        val viewModelWithData = ChatViewModel(
            apiClient = fakeApiClient,
            repository = fakeRepository,
            promptBuilder = SystemPromptBuilder(),
            referenceExtractor = ReferenceExtractor(fakeRepository),
            cvDataProvider = { minimalCVData }
        )

        viewModelWithData.sendMessage("Hi")
        advanceUntilIdle()

        val systemMessage = fakeApiClient.capturedMessages.find { it.role == "system" }
        assertNotNull(systemMessage)
        assertTrue(systemMessage.content.contains("Test")) // Contains name from minimalCVData
    }
}

// Test doubles
class FakeGroqApiClient : GroqApiClient(
    httpClient = HttpClient {},
    apiKey = "fake"
) {
    var responseChunks: List<String> = listOf("Test response")
    var shouldFail: GroqApiException? = null
    var delayResponse = false
    var delayBetweenChunks = false
    var capturedMessages: List<ChatMessage> = emptyList()

    override suspend fun streamChatCompletion(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (GroqApiException) -> Unit
    ) {
        capturedMessages = messages
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

class FakeCVRepository(private val cvData: CVData) : CVRepository() {
    fun getCVData(): CVData = cvData
    override fun resolveReference(id: String): CVReference? = null
}
