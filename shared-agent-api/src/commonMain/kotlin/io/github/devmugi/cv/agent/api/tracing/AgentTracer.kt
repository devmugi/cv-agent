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
        turnNumber: Int? = null,
        promptMetadata: PromptMetadata? = null,
        installationId: String? = null
    ): TracingSpan

    companion object {
        val NOOP: AgentTracer = NoOpAgentTracer()
    }
}

interface TracingSpan {
    fun addResponseChunk(chunk: String)
    fun recordFirstToken()
    fun addEvaluation(name: String, score: Double, label: String? = null)
    fun complete(fullResponse: String, tokenUsage: TokenUsage? = null)
    fun error(exception: Throwable, errorType: String? = null, retryable: Boolean? = null)
}

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

data class PromptMetadata(
    val version: String,
    val variant: String
)

private class NoOpAgentTracer : AgentTracer {
    override fun startLlmSpan(
        model: String,
        systemPrompt: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        sessionId: String?,
        turnNumber: Int?,
        promptMetadata: PromptMetadata?,
        installationId: String?
    ): TracingSpan = NoOpTracingSpan

    @Suppress("EmptyFunctionBlock")
    private object NoOpTracingSpan : TracingSpan {
        override fun addResponseChunk(chunk: String) {}
        override fun recordFirstToken() {}
        override fun addEvaluation(name: String, score: Double, label: String?) {}
        override fun complete(fullResponse: String, tokenUsage: TokenUsage?) {}
        override fun error(exception: Throwable, errorType: String?, retryable: Boolean?) {}
    }
}
