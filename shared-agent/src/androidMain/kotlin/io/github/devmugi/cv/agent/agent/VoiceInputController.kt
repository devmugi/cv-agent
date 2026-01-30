package io.github.devmugi.cv.agent.agent

import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.api.GroqAudioClient
import io.github.devmugi.cv.agent.api.audio.AudioRecorderInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class VoiceInputController(
    private val audioRecorder: AudioRecorderInterface,
    private val audioClient: GroqAudioClient,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "VoiceInputController"
        private const val LOG_PREVIEW_LENGTH = 50
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

        val pathResult = audioRecorder.stopRecording()

        pathResult.onFailure { error ->
            Logger.e(TAG, error) { "Failed to stop recording" }
            _state.value = VoiceInputState.Error(
                error.message ?: "Failed to stop recording"
            )
            return
        }

        val filePath = pathResult.getOrNull() ?: return
        val file = File(filePath)

        _state.value = VoiceInputState.Transcribing

        scope.launch {
            val audioData = file.readBytes()
            val result = audioClient.transcribe(audioData, file.name)

            // Clean up temp file
            file.delete()

            result.onSuccess { text ->
                Logger.d(TAG) { "Transcription successful: ${text.take(LOG_PREVIEW_LENGTH)}..." }
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
