package io.github.devmugi.cv.agent.api.tracing

import io.github.devmugi.cv.agent.api.models.ChatMessage

interface AgentTracer {
    fun startLlmSpan(
        model: String,
        systemPrompt: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int
    ): TracingSpan

    companion object {
        val NOOP: AgentTracer = NoOpAgentTracer()
    }
}

interface TracingSpan {
    fun addResponseChunk(chunk: String)
    fun complete(fullResponse: String, tokenCount: Int? = null)
    fun error(exception: Throwable)
}

private class NoOpAgentTracer : AgentTracer {
    override fun startLlmSpan(
        model: String,
        systemPrompt: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int
    ): TracingSpan = NoOpTracingSpan

    @Suppress("EmptyFunctionBlock")
    private object NoOpTracingSpan : TracingSpan {
        override fun addResponseChunk(chunk: String) {}
        override fun complete(fullResponse: String, tokenCount: Int?) {}
        override fun error(exception: Throwable) {}
    }
}
