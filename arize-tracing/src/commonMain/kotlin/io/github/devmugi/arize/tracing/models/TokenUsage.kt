package io.github.devmugi.arize.tracing.models

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val cachedTokens: Int = 0 // Groq prompt caching
)
