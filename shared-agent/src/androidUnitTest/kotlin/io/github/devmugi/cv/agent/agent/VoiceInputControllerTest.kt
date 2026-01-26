package io.github.devmugi.cv.agent.agent

import app.cash.turbine.test
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.github.devmugi.cv.agent.api.GroqAudioClient
import io.github.devmugi.cv.agent.api.audio.AudioRecorderInterface
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceInputControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeTest
    fun setup() {
        // Disable logging in tests to avoid android.util.Log dependency
        Logger.setMinSeverity(Severity.Assert)
    }

    @Test
    fun initialStateIsIdle() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val fakeClient = createFakeAudioClient("""{"text": "Hello"}""")
        val controller = VoiceInputController(fakeRecorder, fakeClient, testScope)

        assertEquals(VoiceInputState.Idle, controller.state.value)
    }

    @Test
    fun startRecordingChangesStateToRecording() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val fakeClient = createFakeAudioClient("""{"text": "Hello"}""")
        val controller = VoiceInputController(fakeRecorder, fakeClient, testScope)

        controller.startRecording()

        assertEquals(VoiceInputState.Recording, controller.state.value)
        assertTrue(fakeRecorder.startRecordingCalled)
    }

    @Test
    fun startRecordingFailureSetsErrorState() = runTest {
        val fakeRecorder = FakeAudioRecorder(
            startRecordingResult = Result.failure(Exception("Mic unavailable"))
        )
        val fakeClient = createFakeAudioClient("""{"text": "Hello"}""")
        val controller = VoiceInputController(fakeRecorder, fakeClient, testScope)

        controller.startRecording()

        val state = controller.state.value
        assertTrue(state is VoiceInputState.Error)
        assertEquals("Mic unavailable", (state as VoiceInputState.Error).message)
    }

    @Test
    fun stopRecordingAndTranscribeGoesToTranscribingThenIdle() = runTest(testDispatcher) {
        val tempFile = File.createTempFile("test", ".m4a").apply {
            writeBytes(ByteArray(100))
            deleteOnExit()
        }

        val fakeRecorder = FakeAudioRecorder(
            stopRecordingResult = Result.success(tempFile)
        )
        val fakeClient = createFakeAudioClient("""{"text": "Hello world"}""")
        val controller = VoiceInputController(fakeRecorder, fakeClient, testScope)

        var transcribedText: String? = null

        controller.state.test {
            assertEquals(VoiceInputState.Idle, awaitItem())

            controller.startRecording()
            assertEquals(VoiceInputState.Recording, awaitItem())

            controller.stopRecordingAndTranscribe { transcribedText = it }
            assertEquals(VoiceInputState.Transcribing, awaitItem())

            advanceUntilIdle()
            assertEquals(VoiceInputState.Idle, awaitItem())
        }

        assertEquals("Hello world", transcribedText)
    }

    @Test
    fun transcriptionFailureSetsErrorState() = runTest(testDispatcher) {
        val tempFile = File.createTempFile("test", ".m4a").apply {
            writeBytes(ByteArray(100))
            deleteOnExit()
        }

        val fakeRecorder = FakeAudioRecorder(
            stopRecordingResult = Result.success(tempFile)
        )
        val fakeClient = createFakeAudioClient(
            response = """{"error": {"message": "API error"}}""",
            status = HttpStatusCode.BadRequest
        )
        val controller = VoiceInputController(fakeRecorder, fakeClient, testScope)

        controller.state.test {
            assertEquals(VoiceInputState.Idle, awaitItem())

            controller.startRecording()
            assertEquals(VoiceInputState.Recording, awaitItem())

            controller.stopRecordingAndTranscribe { }
            assertEquals(VoiceInputState.Transcribing, awaitItem())

            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is VoiceInputState.Error)
        }
    }

    @Test
    fun cancelRecordingResetsToIdle() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val fakeClient = createFakeAudioClient("""{"text": "Hello"}""")
        val controller = VoiceInputController(fakeRecorder, fakeClient, testScope)

        controller.startRecording()
        controller.cancelRecording()

        assertEquals(VoiceInputState.Idle, controller.state.value)
        assertTrue(fakeRecorder.cancelRecordingCalled)
    }

    @Test
    fun clearErrorResetsToIdle() = runTest {
        val fakeRecorder = FakeAudioRecorder(
            startRecordingResult = Result.failure(Exception("Error"))
        )
        val fakeClient = createFakeAudioClient("""{"text": "Hello"}""")
        val controller = VoiceInputController(fakeRecorder, fakeClient, testScope)

        controller.startRecording()
        assertTrue(controller.state.value is VoiceInputState.Error)

        controller.clearError()
        assertEquals(VoiceInputState.Idle, controller.state.value)
    }

    private fun createFakeAudioClient(
        response: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): GroqAudioClient {
        val mockEngine = MockEngine {
            respond(
                content = response,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        return GroqAudioClient(httpClient, "test-key")
    }
}

class FakeAudioRecorder(
    private val startRecordingResult: Result<Unit> = Result.success(Unit),
    private val stopRecordingResult: Result<File> = Result.success(
        File.createTempFile("test", ".m4a").apply {
            writeBytes(ByteArray(100))
            deleteOnExit()
        }
    )
) : AudioRecorderInterface {

    var startRecordingCalled = false
    var stopRecordingCalled = false
    var cancelRecordingCalled = false

    private var _isRecording = false

    override val isRecording: Boolean
        get() = _isRecording

    override fun startRecording(): Result<Unit> {
        startRecordingCalled = true
        return startRecordingResult.also {
            if (it.isSuccess) _isRecording = true
        }
    }

    override fun stopRecording(): Result<File> {
        stopRecordingCalled = true
        _isRecording = false
        return stopRecordingResult
    }

    override fun cancelRecording() {
        cancelRecordingCalled = true
        _isRecording = false
    }
}
