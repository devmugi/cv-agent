# Voice-to-Text Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enable users to press-and-hold the microphone button to record audio, transcribe via Groq Whisper API, and insert text into chat input.

**Architecture:** Android-only implementation with `AudioRecorder` for MediaRecorder, `GroqAudioClient` for Whisper API, `VoiceInputController` for state coordination, and UI updates to `AnimatedChatInput` for press-and-hold gesture.

**Tech Stack:** Android MediaRecorder, Ktor multipart form data, Groq Whisper API (`whisper-large-v3-turbo`), Compose gesture detection.

---

## Task 1: Add RECORD_AUDIO Permission

**Files:**
- Modify: `android-app/src/main/AndroidManifest.xml:3`

**Step 1: Add permission**

Add the RECORD_AUDIO permission after the INTERNET permission:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

**Step 2: Verify build compiles**

Run: `./gradlew :android-app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add android-app/src/main/AndroidManifest.xml
git commit -m "feat(android): add RECORD_AUDIO permission for voice input"
```

---

## Task 2: Create TranscriptionError sealed class

**Files:**
- Create: `shared-agent-api/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/TranscriptionError.kt`

**Step 1: Create error types**

```kotlin
package io.github.devmugi.cv.agent.api

sealed class TranscriptionError(open val message: String) {
    object FileTooLarge : TranscriptionError("Audio file exceeds 25MB limit")
    object RateLimit : TranscriptionError("Too many requests, please wait")
    data class Api(override val message: String) : TranscriptionError(message)
    data class Recording(override val message: String) : TranscriptionError(message)
}
```

**Step 2: Verify build compiles**

Run: `./gradlew :shared-agent-api:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-agent-api/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/TranscriptionError.kt
git commit -m "feat(api): add TranscriptionError sealed class"
```

---

## Task 3: Create GroqAudioClient with failing test

**Files:**
- Create: `shared-agent-api/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/GroqAudioClientTest.kt`
- Create: `shared-agent-api/src/androidMain/kotlin/io/github/devmugi/cv/agent/api/GroqAudioClient.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.api

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroqAudioClientTest {

    @BeforeTest
    fun setup() {
        Logger.setMinSeverity(Severity.Assert)
    }

    private fun createMockClient(
        response: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = response,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun transcribeReturnsTextOnSuccess() = runTest {
        val jsonResponse = """{"text": "Hello world"}"""
        val client = GroqAudioClient(createMockClient(jsonResponse), "test-key")

        val result = client.transcribe(ByteArray(100), "test.m4a")

        assertTrue(result.isSuccess)
        assertEquals("Hello world", result.getOrNull())
    }

    @Test
    fun transcribeReturnsRateLimitErrorOn429() = runTest {
        val client = GroqAudioClient(
            createMockClient("Rate limited", HttpStatusCode.TooManyRequests),
            "test-key"
        )

        val result = client.transcribe(ByteArray(100), "test.m4a")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("rate") == true,
            "Expected rate limit error")
    }

    @Test
    fun transcribeReturnsApiErrorOnOtherFailures() = runTest {
        val client = GroqAudioClient(
            createMockClient("""{"error": {"message": "Invalid audio"}}""", HttpStatusCode.BadRequest),
            "test-key"
        )

        val result = client.transcribe(ByteArray(100), "test.m4a")

        assertTrue(result.isFailure)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared-agent-api:testDebugUnitTest --tests "*.GroqAudioClientTest" --info`
Expected: FAIL - class not found

**Step 3: Write minimal implementation**

Create `shared-agent-api/src/androidMain/kotlin/io/github/devmugi/cv/agent/api/GroqAudioClient.kt`:

```kotlin
package io.github.devmugi.cv.agent.api

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class GroqAudioClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "GroqAudioClient"
        private const val BASE_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
        const val DEFAULT_MODEL = "whisper-large-v3-turbo"
    }

    @Serializable
    private data class TranscriptionResponse(val text: String)

    @Serializable
    private data class ErrorResponse(val error: ErrorDetail? = null)

    @Serializable
    private data class ErrorDetail(val message: String? = null)

    suspend fun transcribe(
        audioData: ByteArray,
        fileName: String,
        language: String = "en"
    ): Result<String> {
        Logger.d(TAG) { "Transcribing audio: ${audioData.size} bytes, file: $fileName" }

        return try {
            val response = httpClient.submitFormWithBinaryData(
                url = BASE_URL,
                formData = formData {
                    append("file", audioData, Headers.build {
                        append(HttpHeaders.ContentType, "audio/mp4")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                    append("model", model)
                    append("response_format", "json")
                    append("language", language)
                }
            ) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.bodyAsText()
                    val transcription = json.decodeFromString<TranscriptionResponse>(body)
                    Logger.d(TAG) { "Transcription successful: ${transcription.text.take(50)}..." }
                    Result.success(transcription.text)
                }
                HttpStatusCode.TooManyRequests -> {
                    Logger.w(TAG) { "Rate limit exceeded" }
                    Result.failure(Exception("Rate limit exceeded, please try again"))
                }
                HttpStatusCode.PayloadTooLarge -> {
                    Logger.w(TAG) { "File too large" }
                    Result.failure(Exception("Audio file exceeds size limit"))
                }
                else -> {
                    val body = response.bodyAsText()
                    val errorMessage = try {
                        json.decodeFromString<ErrorResponse>(body).error?.message
                    } catch (_: Exception) {
                        null
                    } ?: "Transcription failed: ${response.status}"
                    Logger.w(TAG) { "API error: $errorMessage" }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Transcription request failed" }
            Result.failure(e)
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared-agent-api:testDebugUnitTest --tests "*.GroqAudioClientTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed

**Step 5: Commit**

```bash
git add shared-agent-api/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/GroqAudioClientTest.kt
git add shared-agent-api/src/androidMain/kotlin/io/github/devmugi/cv/agent/api/GroqAudioClient.kt
git commit -m "feat(api): add GroqAudioClient for Whisper transcription"
```

---

## Task 4: Create AudioRecorder

**Files:**
- Create: `shared-agent-api/src/androidMain/kotlin/io/github/devmugi/cv/agent/api/audio/AudioRecorder.kt`

**Step 1: Write AudioRecorder implementation**

```kotlin
package io.github.devmugi.cv.agent.api.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import co.touchlab.kermit.Logger
import java.io.File

class AudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorder"
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    fun startRecording(): Result<Unit> {
        if (isRecording) {
            Logger.w(TAG) { "Already recording" }
            return Result.failure(IllegalStateException("Already recording"))
        }

        return try {
            val file = File(context.cacheDir, "voice_recording_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioChannels(1)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            Logger.d(TAG) { "Recording started: ${file.absolutePath}" }
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Failed to start recording" }
            cleanup()
            Result.failure(e)
        }
    }

    fun stopRecording(): Result<File> {
        val currentRecorder = recorder
        val currentFile = outputFile

        if (currentRecorder == null || currentFile == null) {
            Logger.w(TAG) { "Not currently recording" }
            return Result.failure(IllegalStateException("Not currently recording"))
        }

        return try {
            currentRecorder.stop()
            currentRecorder.release()
            recorder = null

            Logger.d(TAG) { "Recording stopped: ${currentFile.length()} bytes" }
            Result.success(currentFile)
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Failed to stop recording" }
            cleanup()
            Result.failure(e)
        }
    }

    fun cancelRecording() {
        Logger.d(TAG) { "Cancelling recording" }
        cleanup()
    }

    private fun cleanup() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // Ignore errors during cleanup
        }
        recorder = null

        outputFile?.delete()
        outputFile = null
    }
}
```

**Step 2: Verify build compiles**

Run: `./gradlew :shared-agent-api:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-agent-api/src/androidMain/kotlin/io/github/devmugi/cv/agent/api/audio/AudioRecorder.kt
git commit -m "feat(api): add AudioRecorder for voice capture"
```

---

## Task 5: Create VoiceInputState and VoiceInputController

**Files:**
- Create: `shared-agent/src/androidMain/kotlin/io/github/devmugi/cv/agent/agent/VoiceInputState.kt`
- Create: `shared-agent/src/androidMain/kotlin/io/github/devmugi/cv/agent/agent/VoiceInputController.kt`

**Step 1: Create VoiceInputState**

Create `shared-agent/src/androidMain/kotlin/io/github/devmugi/cv/agent/agent/VoiceInputState.kt`:

```kotlin
package io.github.devmugi.cv.agent.agent

sealed class VoiceInputState {
    object Idle : VoiceInputState()
    object Recording : VoiceInputState()
    object Transcribing : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}
```

**Step 2: Create VoiceInputController**

Create `shared-agent/src/androidMain/kotlin/io/github/devmugi/cv/agent/agent/VoiceInputController.kt`:

```kotlin
package io.github.devmugi.cv.agent.agent

import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.api.GroqAudioClient
import io.github.devmugi.cv.agent.api.audio.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceInputController(
    private val audioRecorder: AudioRecorder,
    private val audioClient: GroqAudioClient,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "VoiceInputController"
    }

    private val _state = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    fun startRecording() {
        Logger.d(TAG) { "Starting recording" }

        audioRecorder.startRecording()
            .onSuccess {
                _state.value = VoiceInputState.Recording
            }
            .onFailure { error ->
                Logger.e(TAG, error) { "Failed to start recording" }
                _state.value = VoiceInputState.Error(
                    error.message ?: "Failed to start recording"
                )
            }
    }

    fun stopRecordingAndTranscribe(onResult: (String) -> Unit) {
        Logger.d(TAG) { "Stopping recording and transcribing" }

        val fileResult = audioRecorder.stopRecording()

        fileResult.onFailure { error ->
            Logger.e(TAG, error) { "Failed to stop recording" }
            _state.value = VoiceInputState.Error(
                error.message ?: "Failed to stop recording"
            )
            return
        }

        val file = fileResult.getOrNull() ?: return

        _state.value = VoiceInputState.Transcribing

        scope.launch {
            val audioData = file.readBytes()
            val result = audioClient.transcribe(audioData, file.name)

            // Clean up temp file
            file.delete()

            result.onSuccess { text ->
                Logger.d(TAG) { "Transcription successful: ${text.take(50)}..." }
                _state.value = VoiceInputState.Idle
                onResult(text)
            }.onFailure { error ->
                Logger.e(TAG, error) { "Transcription failed" }
                _state.value = VoiceInputState.Error(
                    error.message ?: "Transcription failed"
                )
            }
        }
    }

    fun cancelRecording() {
        Logger.d(TAG) { "Cancelling recording" }
        audioRecorder.cancelRecording()
        _state.value = VoiceInputState.Idle
    }

    fun clearError() {
        if (_state.value is VoiceInputState.Error) {
            _state.value = VoiceInputState.Idle
        }
    }
}
```

**Step 3: Verify build compiles**

Run: `./gradlew :shared-agent:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-agent/src/androidMain/kotlin/io/github/devmugi/cv/agent/agent/VoiceInputState.kt
git add shared-agent/src/androidMain/kotlin/io/github/devmugi/cv/agent/agent/VoiceInputController.kt
git commit -m "feat(agent): add VoiceInputController for voice recording coordination"
```

---

## Task 6: Add VoiceInputController tests

**Files:**
- Create: `shared-agent/src/androidUnitTest/kotlin/io/github/devmugi/cv/agent/agent/VoiceInputControllerTest.kt`

**Step 1: Write tests**

```kotlin
package io.github.devmugi.cv.agent.agent

import app.cash.turbine.test
import io.github.devmugi.cv.agent.api.GroqAudioClient
import io.github.devmugi.cv.agent.api.audio.AudioRecorder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceInputControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockRecorder = mockk<AudioRecorder>(relaxed = true)
    private val mockAudioClient = mockk<GroqAudioClient>()

    private fun createController() = VoiceInputController(
        audioRecorder = mockRecorder,
        audioClient = mockAudioClient,
        scope = testScope
    )

    @Test
    fun initialStateIsIdle() = runTest {
        val controller = createController()
        assertEquals(VoiceInputState.Idle, controller.state.value)
    }

    @Test
    fun startRecordingChangesStateToRecording() = runTest {
        every { mockRecorder.startRecording() } returns Result.success(Unit)

        val controller = createController()
        controller.startRecording()

        assertEquals(VoiceInputState.Recording, controller.state.value)
    }

    @Test
    fun startRecordingFailureSetsErrorState() = runTest {
        every { mockRecorder.startRecording() } returns Result.failure(Exception("Mic unavailable"))

        val controller = createController()
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

        every { mockRecorder.startRecording() } returns Result.success(Unit)
        every { mockRecorder.stopRecording() } returns Result.success(tempFile)
        coEvery { mockAudioClient.transcribe(any(), any(), any()) } returns Result.success("Hello world")

        val controller = createController()
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

        every { mockRecorder.startRecording() } returns Result.success(Unit)
        every { mockRecorder.stopRecording() } returns Result.success(tempFile)
        coEvery { mockAudioClient.transcribe(any(), any(), any()) } returns
            Result.failure(Exception("API error"))

        val controller = createController()

        controller.startRecording()
        controller.stopRecordingAndTranscribe { }
        advanceUntilIdle()

        val state = controller.state.value
        assertTrue(state is VoiceInputState.Error)
    }

    @Test
    fun cancelRecordingResetsToIdle() = runTest {
        every { mockRecorder.startRecording() } returns Result.success(Unit)

        val controller = createController()
        controller.startRecording()
        controller.cancelRecording()

        assertEquals(VoiceInputState.Idle, controller.state.value)
        verify { mockRecorder.cancelRecording() }
    }

    @Test
    fun clearErrorResetsToIdle() = runTest {
        every { mockRecorder.startRecording() } returns Result.failure(Exception("Error"))

        val controller = createController()
        controller.startRecording()
        assertTrue(controller.state.value is VoiceInputState.Error)

        controller.clearError()
        assertEquals(VoiceInputState.Idle, controller.state.value)
    }
}
```

**Step 2: Verify tests need androidUnitTest source set**

Check if `shared-agent/src/androidUnitTest` directory exists:

Run: `mkdir -p /Users/den/IdeaProjects/cv-agent/shared-agent/src/androidUnitTest/kotlin/io/github/devmugi/cv/agent/agent`

**Step 3: Run tests**

Run: `./gradlew :shared-agent:testDebugUnitTest --tests "*.VoiceInputControllerTest"`
Expected: BUILD SUCCESSFUL, all tests pass

**Step 4: Commit**

```bash
git add shared-agent/src/androidUnitTest/kotlin/io/github/devmugi/cv/agent/agent/VoiceInputControllerTest.kt
git commit -m "test(agent): add VoiceInputController tests"
```

---

## Task 7: Wire up DI for voice input components

**Files:**
- Modify: `shared/src/androidMain/kotlin/io/github/devmugi/cv/agent/di/ViewModelModule.kt`

**Step 1: Read current module structure**

First check what modules exist in the DI setup.

**Step 2: Add voice input dependencies**

Update `ViewModelModule.kt` to include:

```kotlin
package io.github.devmugi.cv.agent.di

import androidx.lifecycle.SavedStateHandle
import io.github.devmugi.arize.tracing.ArizeTracer
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.agent.VoiceInputController
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.api.GroqAudioClient
import io.github.devmugi.cv.agent.api.GroqConfig
import io.github.devmugi.cv.agent.api.audio.AudioRecorder
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    // Voice input components
    single { AudioRecorder(androidContext()) }
    single { GroqAudioClient(get(), GroqConfig.apiKey) }

    viewModel { params ->
        val savedStateHandle: SavedStateHandle = params.get()
        val dataProvider: AgentDataProvider? = params.getOrNull()
        ChatViewModel(
            savedStateHandle = savedStateHandle,
            apiClient = get(),
            promptBuilder = get(),
            suggestionExtractor = get(),
            dataProvider = dataProvider,
            analytics = getOrNull<Analytics>() ?: Analytics.NOOP,
            tracer = getOrNull<ArizeTracer>() ?: ArizeTracer.NOOP
        )
    }
}
```

**Step 3: Verify build**

Run: `./gradlew :shared:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/src/androidMain/kotlin/io/github/devmugi/cv/agent/di/ViewModelModule.kt
git commit -m "feat(di): wire up AudioRecorder and GroqAudioClient"
```

---

## Task 8: Update AnimatedChatInput for press-and-hold

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/AnimatedChatInput.kt`

**Step 1: Add new parameters and gesture handling**

Update to add voice recording state and callbacks. The key changes:

1. Add `isRecording: Boolean` and `isTranscribing: Boolean` parameters
2. Add `onRecordingStart: () -> Unit` and `onRecordingStop: () -> Unit` callbacks
3. Modify mic button behavior to use press-and-hold gesture

Note: The actual mic button is inside `ArcaneAgentChatInput` from the Arcane design system. We need to check if it supports custom gesture handling or if we need to pass these callbacks through.

**Step 2: Check ArcaneAgentChatInput interface**

The `onVoiceToTextClick` callback exists but for tap-to-click. We need to either:
- A) Modify how we pass the callback to handle press-and-hold
- B) Create a wrapper that intercepts the gesture

For now, let's update AnimatedChatInput to pass new parameters that can be used when the design system is updated:

```kotlin
package io.github.devmugi.cv.agent.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.devmugi.arcane.design.foundation.tokens.ArcaneRadius
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import io.github.devmugi.arcane.chat.components.input.ArcaneAgentChatInput
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

@Composable
fun AnimatedChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String = "",
    enabled: Boolean = true,
    isRecording: Boolean = false,
    isTranscribing: Boolean = false,
    onRecordingStart: () -> Unit = {},
    onRecordingStop: () -> Unit = {},
    onVoiceToTextClick: (() -> Unit)? = null,
    onAudioRecordClick: (() -> Unit)? = null,
    activeItemsContent: (@Composable RowScope.() -> Unit)? = null,
    onInputFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val widthFraction by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.9f,
        animationSpec = tween(durationMillis = 200),
        label = "inputWidthAnimation"
    )

    val inputShape = ArcaneRadius.Large

    LaunchedEffect(isFocused) {
        onInputFocusChanged(isFocused)
    }

    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .background(
                color = ArcaneTheme.colors.surfaceContainerLow,
                shape = inputShape
            ),
        contentAlignment = Alignment.Center
    ) {
        ArcaneAgentChatInput(
            value = value,
            onValueChange = onValueChange,
            onSend = onSend,
            placeholder = placeholder,
            enabled = enabled && !isRecording && !isTranscribing,
            onVoiceToTextClick = onVoiceToTextClick,
            onAudioRecordClick = onAudioRecordClick,
            activeItemsContent = activeItemsContent,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused || focusState.hasFocus
                }
        )
    }
}
```

**Step 3: Verify build**

Run: `./gradlew :shared-ui:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/AnimatedChatInput.kt
git commit -m "feat(ui): add voice recording state parameters to AnimatedChatInput"
```

---

## Task 9: Integrate voice input into ChatScreen

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`

**Step 1: Add voice input state and callbacks to ChatScreen**

Add new parameters for voice input:

```kotlin
@Composable
fun ChatScreen(
    state: ChatState,
    toastState: ArcaneToastState,
    onSendMessage: (String) -> Unit,
    analytics: Analytics = Analytics.NOOP,
    modifier: Modifier = Modifier,
    // Existing callbacks...
    // Voice input
    isRecording: Boolean = false,
    isTranscribing: Boolean = false,
    onRecordingStart: () -> Unit = {},
    onRecordingStop: () -> Unit = {},
    onRequestMicPermission: () -> Unit = {},
    hasMicPermission: Boolean = false,
)
```

**Step 2: Update AnimatedChatInput usage**

Pass the new voice parameters:

```kotlin
AnimatedChatInput(
    value = inputText,
    onValueChange = { inputText = it },
    onSend = {
        if (inputText.isNotBlank()) {
            onSendMessage(inputText)
            inputText = ""
        }
    },
    placeholder = "Ask about my experience...",
    enabled = !(state.isLoading || state.isStreaming),
    isRecording = isRecording,
    isTranscribing = isTranscribing,
    onVoiceToTextClick = {
        if (hasMicPermission) {
            onRecordingStart()
        } else {
            onRequestMicPermission()
        }
    },
    onAudioRecordClick = {
        toastState.show("Voice conversation not implemented yet")
    },
    // ... rest of params
)
```

**Step 3: Verify build**

Run: `./gradlew :shared-ui:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt
git commit -m "feat(ui): integrate voice input parameters into ChatScreen"
```

---

## Task 10: Create Android-specific ChatScreenWrapper with permission handling

**Files:**
- Create: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/ui/ChatScreenWrapper.kt`

**Step 1: Create wrapper with permission handling**

```kotlin
package io.github.devmugi.cv.agent.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastState
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.agent.VoiceInputController
import io.github.devmugi.cv.agent.agent.VoiceInputState
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.ui.ChatScreen

@Composable
fun ChatScreenWrapper(
    viewModel: ChatViewModel,
    voiceController: VoiceInputController?,
    toastState: ArcaneToastState,
    analytics: Analytics,
    modifier: Modifier = Modifier,
    onNavigateToCareerTimeline: () -> Unit = {},
    onNavigateToProject: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val chatState by viewModel.state.collectAsState()
    val voiceState by voiceController?.state?.collectAsState()
        ?: remember { mutableStateOf(VoiceInputState.Idle) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PermissionChecker.PERMISSION_GRANTED
        )
    }

    var pendingRecordingStart by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted && pendingRecordingStart) {
            voiceController?.startRecording()
        }
        pendingRecordingStart = false
        if (!granted) {
            toastState.show("Microphone permission required for voice input")
        }
    }

    // Handle voice input errors
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceInputState.Error) {
            toastState.show((voiceState as VoiceInputState.Error).message)
            voiceController?.clearError()
        }
    }

    ChatScreen(
        state = chatState,
        toastState = toastState,
        onSendMessage = viewModel::sendMessage,
        analytics = analytics,
        modifier = modifier,
        onSuggestionClick = viewModel::onSuggestionClicked,
        onCopyMessage = viewModel::onMessageCopied,
        onShareMessage = { /* TODO */ },
        onLikeMessage = viewModel::onMessageLiked,
        onDislikeMessage = viewModel::onMessageDisliked,
        onRegenerateMessage = viewModel::onRegenerateClicked,
        onClearHistory = viewModel::clearHistory,
        onNavigateToCareerTimeline = onNavigateToCareerTimeline,
        onNavigateToProject = onNavigateToProject,
        // Voice input
        isRecording = voiceState is VoiceInputState.Recording,
        isTranscribing = voiceState is VoiceInputState.Transcribing,
        onRecordingStart = {
            voiceController?.startRecording()
        },
        onRecordingStop = {
            voiceController?.stopRecordingAndTranscribe { transcribedText ->
                // Insert transcribed text into the input field
                // For now, we'll send it directly as a message
                if (transcribedText.isNotBlank()) {
                    viewModel.sendMessage(transcribedText)
                }
            }
        },
        onRequestMicPermission = {
            pendingRecordingStart = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        hasMicPermission = hasMicPermission
    )
}
```

**Step 2: Verify build**

Run: `./gradlew :android-app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/ui/ChatScreenWrapper.kt
git commit -m "feat(android): add ChatScreenWrapper with mic permission handling"
```

---

## Task 11: Update MainActivity to use ChatScreenWrapper

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

**Step 1: Read current MainActivity**

First check the current implementation to understand integration points.

**Step 2: Add VoiceInputController to MainActivity**

Inject and create VoiceInputController, pass to ChatScreenWrapper:

```kotlin
// In MainActivity, add:
val audioRecorder: AudioRecorder by inject()
val audioClient: GroqAudioClient by inject()

// Create VoiceInputController with viewModelScope or rememberCoroutineScope
val voiceController = remember {
    VoiceInputController(audioRecorder, audioClient, lifecycleScope)
}

// Use ChatScreenWrapper instead of ChatScreen
ChatScreenWrapper(
    viewModel = chatViewModel,
    voiceController = voiceController,
    toastState = toastState,
    analytics = analytics,
    // ... other params
)
```

**Step 3: Verify build and run**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt
git commit -m "feat(android): integrate VoiceInputController into MainActivity"
```

---

## Task 12: Run full test suite and verify

**Step 1: Run all tests**

Run: `./gradlew :shared-agent-api:testDebugUnitTest :shared-agent:testDebugUnitTest`
Expected: All tests pass

**Step 2: Run quality checks**

Run: `./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL

**Step 3: Build debug APK**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Final commit (if any cleanup needed)**

```bash
git status
# If any files need cleanup, commit them
```

---

## Summary

| Task | Description | Test Coverage |
|------|-------------|---------------|
| 1 | Add RECORD_AUDIO permission | Manual |
| 2 | Create TranscriptionError | Build verification |
| 3 | Create GroqAudioClient | 3 unit tests |
| 4 | Create AudioRecorder | Build verification |
| 5 | Create VoiceInputController | Build verification |
| 6 | VoiceInputController tests | 6 unit tests |
| 7 | Wire up DI | Build verification |
| 8 | Update AnimatedChatInput | Build verification |
| 9 | Integrate into ChatScreen | Build verification |
| 10 | ChatScreenWrapper with permissions | Build verification |
| 11 | Update MainActivity | Integration test |
| 12 | Full verification | All tests + build |

**Manual testing required:**
- Permission flow on real device
- Recording quality
- Transcription accuracy
- Error handling (no mic, API failures)
