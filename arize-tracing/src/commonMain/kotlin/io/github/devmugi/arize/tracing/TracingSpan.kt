package io.github.devmugi.arize.tracing

import io.github.devmugi.arize.tracing.models.TokenUsage

/**
 * Represents an active tracing span for an LLM call.
 * Records data during and after the LLM interaction.
 */
interface TracingSpan {

    /**
     * Adds a response chunk from streaming output.
     * Used to accumulate the full response.
     */
    fun addResponseChunk(chunk: String)

    /**
     * Records the time to first token (TTFT) metric.
     * Should be called when the first content token is received.
     */
    fun recordFirstToken()

    /**
     * Completes the span with the full response and optional token usage.
     * Cost is automatically calculated if pricing was configured.
     */
    fun complete(fullResponse: String, tokenUsage: TokenUsage? = null)

    /**
     * Records an error and ends the span with error status.
     *
     * @param exception The exception that occurred
     * @param errorType Optional categorization: "auth", "rate_limit", "api", "timeout", "network"
     * @param retryable Whether the operation can be retried
     */
    fun error(
        exception: Throwable,
        errorType: String? = null,
        retryable: Boolean? = null
    )

    /**
     * Adds an evaluation score to the span.
     * Can be called multiple times for different evaluations.
     *
     * @param name The evaluation name (e.g., "relevance", "hallucination")
     * @param score The evaluation score
     * @param label Optional label for categorical evaluations
     */
    fun addEvaluation(name: String, score: Double, label: String? = null)

    /**
     * Adds a mid-span event with optional attributes.
     * Events represent discrete occurrences during span lifetime.
     */
    fun addEvent(name: String, attributes: Map<String, Any>? = null)

    /**
     * Adds metadata after span creation.
     * Useful for late-bound information.
     */
    fun addMetadata(key: String, value: Any)

    /**
     * Adds tags after span creation.
     * Tags are string labels for categorization.
     */
    fun addTags(vararg tags: String)
}
