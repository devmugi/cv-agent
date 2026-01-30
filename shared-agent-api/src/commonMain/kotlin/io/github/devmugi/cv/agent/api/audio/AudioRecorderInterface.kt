package io.github.devmugi.cv.agent.api.audio

/**
 * Multiplatform interface for audio recording.
 * Returns file paths as Strings for cross-platform compatibility.
 */
interface AudioRecorderInterface {
    val isRecording: Boolean
    fun startRecording(): Result<Unit>
    fun stopRecording(): Result<String> // Returns file path
    fun cancelRecording()
}
