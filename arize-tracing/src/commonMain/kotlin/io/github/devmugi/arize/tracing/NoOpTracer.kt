package io.github.devmugi.arize.tracing

import io.github.devmugi.arize.tracing.models.TokenUsage

/**
 * No-op implementation of [ArizeTracer].
 * Use this when tracing is disabled to avoid null checks.
 */
internal class NoOpTracer : ArizeTracer {

    override fun startAgentSpan(block: AgentSpanBuilder.() -> Unit): AgentSpan {
        return NoOpAgentSpan
    }

    override fun startLlmSpan(block: LlmSpanBuilder.() -> Unit): TracingSpan {
        return NoOpTracingSpan
    }

    override fun flush() {
        // No-op
    }

    override fun shutdown() {
        // No-op
    }

    @Suppress("EmptyFunctionBlock")
    private object NoOpAgentSpan : AgentSpan {
        override suspend fun <T> withContext(block: suspend () -> T): T = block()
        override fun complete() {}
        override fun error(exception: Throwable) {}
        override fun addMetadata(key: String, value: Any) {}
    }

    @Suppress("EmptyFunctionBlock")
    private object NoOpTracingSpan : TracingSpan {
        override fun addResponseChunk(chunk: String) {}
        override fun recordFirstToken() {}
        override fun complete(fullResponse: String, tokenUsage: TokenUsage?) {}
        override fun error(exception: Throwable, errorType: String?, retryable: Boolean?) {}
        override fun addEvaluation(name: String, score: Double, label: String?) {}
        override fun addEvent(name: String, attributes: Map<String, Any>?) {}
        override fun addMetadata(key: String, value: Any) {}
        override fun addTags(vararg tags: String) {}
    }
}
