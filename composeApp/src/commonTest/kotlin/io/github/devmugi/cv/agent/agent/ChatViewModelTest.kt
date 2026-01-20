package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.GroqApiException
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.models.CVReference
import io.github.devmugi.cv.agent.data.models.Education
import io.github.devmugi.cv.agent.data.models.PersonalInfo
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
}

// Test doubles
class FakeGroqApiClient : GroqApiClient(
    httpClient = HttpClient {},
    apiKey = "fake"
) {
    var responseChunks: List<String> = listOf("Test response")
    var shouldFail: GroqApiException? = null
    var delayResponse = false

    override suspend fun streamChatCompletion(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (GroqApiException) -> Unit
    ) {
        if (delayResponse) {
            kotlinx.coroutines.delay(100)
        }
        shouldFail?.let {
            onError(it)
            return
        }
        responseChunks.forEach { onChunk(it) }
        onComplete()
    }
}

class FakeCVRepository(private val cvData: CVData) : CVRepository() {
    fun getCVData(): CVData = cvData
    override fun resolveReference(id: String): CVReference? = null
}
