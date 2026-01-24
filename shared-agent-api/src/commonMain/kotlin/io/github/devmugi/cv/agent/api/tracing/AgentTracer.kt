package io.github.devmugi.cv.agent.api.tracing

import io.github.devmugi.cv.agent.api.models.ChatMessage

interface AgentTracer {
    fun startLlmSpan(
        model: String,
        systemPrompt: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        sessionId: String? = null,
        turnNumber: Int? = null
    ): TracingSpan

    companion object {
        val NOOP: AgentTracer = NoOpAgentTracer()
    }
}

interface TracingSpan {
    fun addResponseChunk(chunk: String)
    fun recordFirstToken()
    fun complete(fullResponse: String, tokenUsage: TokenUsage? = null)
    fun error(exception: Throwable, errorType: String? = null, retryable: Boolean? = null)
}

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

private class NoOpAgentTracer : AgentTracer {
    override fun startLlmSpan(
        model: String,
        systemPrompt: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        sessionId: String?,
        turnNumber: Int?
    ): TracingSpan = NoOpTracingSpan

    @Suppress("EmptyFunctionBlock")
    private object NoOpTracingSpan : TracingSpan {
        override fun addResponseChunk(chunk: String) {}
        override fun recordFirstToken() {}
        override fun complete(fullResponse: String, tokenUsage: TokenUsage?) {}
        override fun error(exception: Throwable, errorType: String?, retryable: Boolean?) {}
    }
}
