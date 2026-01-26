package io.github.devmugi.cv.agent.agent

sealed class VoiceInputState {
    object Idle : VoiceInputState()
    object Recording : VoiceInputState()
    object Transcribing : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}
