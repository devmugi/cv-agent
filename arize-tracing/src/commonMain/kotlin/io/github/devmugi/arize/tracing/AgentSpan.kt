package io.github.devmugi.arize.tracing

/**
 * Represents an active Agent span for tracing.
 *
 * Agent spans are parent spans that can contain child LLM spans,
 * tool calls, or other operations. Use [withContext] to ensure
 * child spans are properly nested.
 */
interface AgentSpan {
    /**
     * Executes a block within this span's context.
     * Any spans created inside the block will be children of this agent span.
     *
     * Example:
     * ```
     * agentSpan.withContext {
     *     apiClient.streamChatCompletion(...) // LLM span becomes child
     * }
     * ```
     */
    suspend fun <T> withContext(block: suspend () -> T): T

    /**
     * Marks the agent span as successfully completed.
     */
    fun complete()

    /**
     * Marks the agent span as failed with an error.
     */
    fun error(exception: Throwable)

    /**
     * Adds metadata to the span after creation.
     */
    fun addMetadata(key: String, value: Any)
}
