package io.github.devmugi.cv.agent.api.tracing

import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor

class OpenTelemetryAgentTracer private constructor(
    private val tracer: Tracer,
    private val tracerProvider: SdkTracerProvider
) : AgentTracer {

    /**
     * Flushes all pending spans to the exporter.
     * Call this at the end of tests to ensure traces are sent before test completes.
     */
    fun flush() {
        Logger.d(TAG) { "Flushing spans..." }
        tracerProvider.forceFlush().join(FLUSH_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
    }

    /**
     * Shuts down the tracer provider and flushes remaining spans.
     */
    fun shutdown() {
        Logger.d(TAG) { "Shutting down tracer..." }
        tracerProvider.shutdown().join(FLUSH_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
    }

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
    ): TracingSpan {
        Logger.d(TAG) { "Starting LLM span - model: $model, messages: ${messages.size}, session: $sessionId, turn: $turnNumber" }
        val spanBuilder = tracer.spanBuilder("LLM")
            .setSpanKind(SpanKind.CLIENT)
            // OpenInference semantic conventions
            .setAttribute("openinference.span.kind", "LLM")
            .setAttribute("llm.model_name", model)
            .setAttribute("llm.invocation_parameters", """{"temperature":$temperature,"max_tokens":$maxTokens}""")

        // Session tracking
        sessionId?.let { spanBuilder.setAttribute("session.id", it) }
        turnNumber?.let { spanBuilder.setAttribute("llm.turn_number", it.toLong()) }

        // Device identification
        installationId?.let { spanBuilder.setAttribute("device.installation_id", it) }

        // Prompt versioning
        promptMetadata?.let { meta ->
            spanBuilder.setAttribute("llm.prompt.version", meta.version)
            spanBuilder.setAttribute("llm.prompt.variant", meta.variant)
        }

        // Add system prompt as first input message (index 0)
        var messageIndex = 0
        if (systemPrompt.isNotEmpty()) {
            spanBuilder.setAttribute("llm.input_messages.$messageIndex.message.role", "system")
            spanBuilder.setAttribute(
                "llm.input_messages.$messageIndex.message.content",
                systemPrompt.take(MAX_CONTENT_LENGTH)
            )
            messageIndex++
        }

        // Add conversation messages
        messages.forEach { msg ->
            spanBuilder.setAttribute("llm.input_messages.$messageIndex.message.role", msg.role)
            spanBuilder.setAttribute(
                "llm.input_messages.$messageIndex.message.content",
                msg.content.take(MAX_CONTENT_LENGTH)
            )
            messageIndex++
        }

        return OpenTelemetryTracingSpan(spanBuilder.startSpan())
    }

    private class OpenTelemetryTracingSpan(
        private val span: Span
    ) : TracingSpan {
        private val responseBuilder = StringBuilder()
        private val startTimeNanos = System.nanoTime()
        private var firstTokenRecorded = false

        override fun addResponseChunk(chunk: String) {
            responseBuilder.append(chunk)
        }

        override fun recordFirstToken() {
            if (!firstTokenRecorded) {
                firstTokenRecorded = true
                val ttftMs = (System.nanoTime() - startTimeNanos) / 1_000_000
                span.setAttribute("llm.latency.time_to_first_token_ms", ttftMs)
                Logger.d(TAG) { "First token received - TTFT: ${ttftMs}ms" }
            }
        }

        override fun addEvaluation(name: String, score: Double, label: String?) {
            span.setAttribute("evals.$name.score", score)
            label?.let { span.setAttribute("evals.$name.label", it) }
            Logger.d(TAG) { "Added evaluation - $name: $score${label?.let { " ($it)" } ?: ""}" }
        }

        override fun complete(fullResponse: String, tokenUsage: TokenUsage?) {
            Logger.d(TAG) { "Completing LLM span - response length: ${fullResponse.length}" }
            // OpenInference semantic conventions for output
            span.setAttribute("llm.output_messages.0.message.role", "assistant")
            span.setAttribute("llm.output_messages.0.message.content", fullResponse.take(MAX_CONTENT_LENGTH))
            tokenUsage?.let { usage ->
                span.setAttribute("llm.token_count.prompt", usage.promptTokens.toLong())
                span.setAttribute("llm.token_count.completion", usage.completionTokens.toLong())
                span.setAttribute("llm.token_count.total", usage.totalTokens.toLong())
            }
            span.setStatus(StatusCode.OK)
            span.end()
        }

        override fun error(exception: Throwable, errorType: String?, retryable: Boolean?) {
            span.setStatus(StatusCode.ERROR, exception.message ?: "Unknown error")
            span.recordException(exception)
            errorType?.let { span.setAttribute("error.type", it) }
            retryable?.let { span.setAttribute("error.retryable", it) }
            span.end()
        }
    }

    companion object {
        private const val TAG = "PhoenixTracer"
        private const val MAX_CONTENT_LENGTH = 4000
        private const val FLUSH_TIMEOUT_SECONDS = 10L
        private const val DEFAULT_ENDPOINT = "http://localhost:6006/v1/traces"

        fun create(
            endpoint: String = DEFAULT_ENDPOINT,
            serviceName: String = "cv-agent"
        ): OpenTelemetryAgentTracer {
            Logger.d(TAG) { "Creating OpenTelemetry tracer - endpoint: $endpoint, service: $serviceName" }

            val exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint)
                .build()

            val tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build()

            val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build()

            Logger.d(TAG) { "OpenTelemetry tracer created successfully" }
            return OpenTelemetryAgentTracer(openTelemetry.getTracer(serviceName), tracerProvider)
        }
    }
}
