package io.github.devmugi.cv.agent.api

sealed class GroqApiException(message: String) : Exception(message) {
    data class NetworkError(val reason: String) : GroqApiException("Network error: $reason")
    data class AuthError(val code: Int) : GroqApiException("Authentication failed: $code")
    data class RateLimitError(val retryAfter: Int?) : GroqApiException("Rate limit exceeded")
    data class ApiError(val code: Int, override val message: String) : GroqApiException(message)
}
