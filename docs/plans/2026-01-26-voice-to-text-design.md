# Voice-to-Text Input Design

## Overview

Add voice-to-text input to the CV Agent chat interface, allowing users to press-and-hold the microphone button to record audio, which is then transcribed via Groq's Whisper API and inserted into the text input field.

## Scope

- **Platform:** Android only
- **Interaction:** Press-and-hold to record
- **Feedback:** Minimal (button state change only)
- **Transcription:** Groq Whisper API (`whisper-large-v3-turbo`)

## Architecture

```
User press-hold mic → Android MediaRecorder captures audio → User releases
→ Audio file sent to Groq API → Transcribed text inserted into input field
```

### Components

| Component | Module | Location |
|-----------|--------|----------|
| `GroqAudioClient` | shared-agent-api | `androidMain/.../api/GroqAudioClient.kt` |
| `AudioRecorder` | shared-agent-api | `androidMain/.../api/audio/AudioRecorder.kt` |
| `VoiceInputController` | shared-agent | `androidMain/.../agent/VoiceInputController.kt` |
| UI changes | shared-ui | `AnimatedChatInput.kt`, `ChatScreen.kt` |

## GroqAudioClient

**API Details:**
- Endpoint: `POST https://api.groq.com/openai/v1/audio/transcriptions`
- Model: `whisper-large-v3-turbo`
- Auth: Same `GROQ_API_KEY` used by existing `GroqApiClient`

**Interface:**
```kotlin
class GroqAudioClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    suspend fun transcribe(audioFile: File): Result<String>
}
```

**Request:** Multipart form data with:
- `file`: Audio file bytes
- `model`: `whisper-large-v3-turbo`
- `response_format`: `json`
- `language`: `en` (optional, improves accuracy)

**Error types:**
```kotlin
sealed class TranscriptionError {
    object FileTooLarge : TranscriptionError()
    object RateLimit : TranscriptionError()
    data class Api(val message: String) : TranscriptionError()
}
```

## AudioRecorder

**Interface:**
```kotlin
class AudioRecorder(private val context: Context) {
    fun startRecording(): Result<Unit>
    fun stopRecording(): Result<File>
    fun cancelRecording()
    val isRecording: Boolean
}
```

**Recording configuration:**
- Format: MPEG-4 container (`.m4a`)
- Encoder: AAC
- Sample rate: 16kHz (Groq downsamples to this anyway)
- Channels: Mono
- Output: Temp file in `context.cacheDir`

**Implementation:**
```kotlin
MediaRecorder(context).apply {
    setAudioSource(AudioSource.MIC)
    setOutputFormat(OutputFormat.MPEG_4)
    setAudioEncoder(AudioEncoder.AAC)
    setAudioSamplingRate(16000)
    setAudioChannels(1)
    setOutputFile(tempFile.absolutePath)
    prepare()
    start()
}
```

**Lifecycle:**
- `startRecording()` → Creates temp file, configures and starts MediaRecorder
- `stopRecording()` → Stops recorder, releases resources, returns temp file
- `cancelRecording()` → Stops recorder, deletes temp file

## VoiceInputController

**State:**
```kotlin
sealed class VoiceInputState {
    object Idle : VoiceInputState()
    object Recording : VoiceInputState()
    object Transcribing : VoiceInputState()
    data class Error(val error: TranscriptionError) : VoiceInputState()
}
```

**Interface:**
```kotlin
class VoiceInputController(
    private val audioRecorder: AudioRecorder,
    private val audioClient: GroqAudioClient
) {
    val state: StateFlow<VoiceInputState>

    fun startRecording()
    fun stopRecordingAndTranscribe(onResult: (String) -> Unit)
    fun cancelRecording()
}
```

**Flow:**
1. `startRecording()` → State becomes `Recording`, calls `audioRecorder.startRecording()`
2. `stopRecordingAndTranscribe()` → State becomes `Transcribing`, gets audio file, calls API
3. On success → State becomes `Idle`, invokes `onResult` callback with transcribed text
4. On error → State becomes `Error`, UI shows toast

**Integration:** `ChatViewModel` holds `VoiceInputController`, exposes state to UI, transcribed text is inserted into input field for user review before sending.

## UI Changes

**AnimatedChatInput new parameters:**
```kotlin
@Composable
fun AnimatedChatInput(
    // ... existing params ...
    isRecording: Boolean = false,
    isTranscribing: Boolean = false,
    onRecordingStart: () -> Unit = {},
    onRecordingStop: () -> Unit = {},
)
```

**Mic button behavior:**
- Press-and-hold gesture using `pointerInput` with `detectTapGestures`
- On press down → `onRecordingStart()`
- On release → `onRecordingStop()`

**Visual states:**
- `Idle` → Default mic icon
- `Recording` → Mic icon with accent/red color tint
- `Transcribing` → Loading indicator or disabled button

**Permission handling:**
```kotlin
val micPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    if (granted) viewModel.onVoiceRecordStart()
}
```

## DI Wiring

**AppModule.kt (androidMain):**
```kotlin
single { AudioRecorder(androidContext()) }
single { GroqAudioClient(get(), GroqConfig.apiKey) }
factory { VoiceInputController(get(), get()) }
```

## Permissions

**AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## Testing Strategy

1. **Unit tests for `GroqAudioClient`** (`shared-agent-api`)
   - Mock HTTP responses
   - Test success parsing, error handling

2. **Unit tests for `VoiceInputController`** (`shared-agent`)
   - Fake `AudioRecorder` and `GroqAudioClient`
   - Test state transitions: Idle → Recording → Transcribing → Idle
   - Test error states

3. **Manual testing**
   - Real device required (emulator mic unreliable)
   - Test permission flow (grant, deny, deny permanently)
   - Test recording quality with different background noise

## Out of Scope (YAGNI)

- iOS support
- Silence detection / auto-stop
- Audio waveform visualization
- Multiple language support
- Audio playback before sending
- Continuous voice conversation mode
