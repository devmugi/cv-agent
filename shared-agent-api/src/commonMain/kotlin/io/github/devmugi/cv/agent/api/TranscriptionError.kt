package io.github.devmugi.cv.agent.api

sealed class TranscriptionError(open val message: String) {
    object FileTooLarge : TranscriptionError("Audio file exceeds 25MB limit")
    object RateLimit : TranscriptionError("Too many requests, please wait")
    data class Api(override val message: String) : TranscriptionError(message)
    data class Recording(override val message: String) : TranscriptionError(message)
}
