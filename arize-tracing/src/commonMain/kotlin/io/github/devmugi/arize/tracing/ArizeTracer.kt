package io.github.devmugi.arize.tracing

/**
 * Main interface for Arize/Phoenix tracing.
 *
 * Use the companion object to get a no-op implementation:
 * ```
 * val tracer = ArizeTracer.NOOP
 * ```
 *
 * For actual tracing, use the platform-specific implementation:
 * ```
 * // Android/JVM
 * val tracer = OpenTelemetryArizeTracer.create(
 *     endpoint = "http://localhost:6006/v1/traces",
 *     serviceName = "my-app",
 *     mode = TracingMode.TESTING
 * )
 * ```
 */
interface ArizeTracer {

    /**
     * Starts a new Agent span for orchestrating multiple operations.
     *
     * Agent spans are parent spans that can contain child LLM spans,
     * tool calls, or other operations. Use [AgentSpan.withContext] to
     * ensure child spans are properly nested.
     *
     * Example usage:
     * ```
     * val agentSpan = tracer.startAgentSpan {
     *     name("ChatAgent")
     *     sessionId("abc-123")
     * }
     *
     * agentSpan.withContext {
     *     // LLM spans created here become children of the agent span
     *     apiClient.streamChatCompletion(...)
     * }
     *
     * agentSpan.complete()
     * ```
     */
    fun startAgentSpan(block: AgentSpanBuilder.() -> Unit): AgentSpan

    /**
     * Starts a new LLM span with the given configuration.
     *
     * Example usage:
     * ```
     * val span = tracer.startLlmSpan {
     *     spanName("ChatGroq")  // Custom name for Phoenix UI
     *     model("llama-3.3-70b-versatile")
     *     provider("groq")
     *     systemPrompt(prompt)
     *     messages(chatHistory)
     *     sessionId("abc-123")
     *     turnNumber(3)
     *     pricing(promptPerMillion = 0.59, completionPerMillion = 0.79)
     * }
     *
     * // During streaming
     * span.recordFirstToken()
     * span.addResponseChunk(chunk)
     *
     * // On completion
     * span.complete(fullResponse, tokenUsage)
     * ```
     */
    fun startLlmSpan(block: LlmSpanBuilder.() -> Unit): TracingSpan

    /**
     * Flushes all pending spans to the exporter.
     * Call this at the end of tests to ensure traces are sent.
     */
    fun flush()

    /**
     * Shuts down the tracer and flushes remaining spans.
     * Call this when the application is terminating.
     */
    fun shutdown()

    companion object {
        /**
         * A no-op tracer that does nothing.
         * Use this as a default when tracing is disabled.
         */
        val NOOP: ArizeTracer = NoOpTracer()
    }
}
