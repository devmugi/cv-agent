package io.github.devmugi.arize.tracing

import co.touchlab.kermit.Logger
import io.github.devmugi.arize.tracing.models.TokenPricing
import io.github.devmugi.arize.tracing.models.TokenUsage
import io.github.devmugi.arize.tracing.models.TracingMode
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import java.util.concurrent.TimeUnit

/**
 * OpenTelemetry-based implementation of [ArizeTracer] for Arize Phoenix.
 *
 * Implements OpenInference semantic conventions for LLM observability.
 *
 * @see <a href="https://arize.com/docs/ax/observe/tracing">Arize Tracing Docs</a>
 */
class OpenTelemetryArizeTracer private constructor(
    private val tracer: Tracer,
    private val tracerProvider: SdkTracerProvider,
    private val mode: TracingMode
) : ArizeTracer {

    override fun startLlmSpan(block: LlmSpanBuilder.() -> Unit): TracingSpan {
        val builder = LlmSpanBuilder().apply(block)
        builder.validate()

        Logger.d(TAG) {
            "Starting LLM span - model: ${builder.model}, provider: ${builder.provider}, " +
                "messages: ${builder.messages.size}, session: ${builder.sessionId}, turn: ${builder.turnNumber}"
        }

        val spanBuilder = tracer.spanBuilder("LLM")
            .setSpanKind(SpanKind.CLIENT)

        // OpenInference semantic conventions
        spanBuilder.setAttribute("openinference.span.kind", "LLM")
        spanBuilder.setAttribute("llm.model_name", builder.model!!)

        // Provider
        builder.provider?.let { spanBuilder.setAttribute("llm.provider", it) }

        // Invocation parameters
        val invocationParams = buildInvocationParams(builder)
        if (invocationParams.isNotEmpty()) {
            spanBuilder.setAttribute("llm.invocation_parameters", invocationParams)
        }

        // Session tracking
        builder.sessionId?.let { spanBuilder.setAttribute("session.id", it) }
        builder.turnNumber?.let { spanBuilder.setAttribute("llm.turn_number", it.toLong()) }

        // User identification
        builder.userId?.let { spanBuilder.setAttribute("user.id", it) }
        builder.installationId?.let { spanBuilder.setAttribute("device.installation_id", it) }

        // Prompt template (for Arize Playground)
        builder.promptTemplate?.let { template ->
            spanBuilder.setAttribute("llm.prompt_template.template", template.template.take(MAX_CONTENT_LENGTH))
            template.version?.let { spanBuilder.setAttribute("llm.prompt_template.version", it) }
            template.variables?.let { vars ->
                spanBuilder.setAttribute("llm.prompt_template.variables", vars.toJsonString())
            }
        }

        // Prompt versioning (for custom tracking)
        builder.promptVersion?.let { spanBuilder.setAttribute("llm.prompt.version", it) }
        builder.promptVariant?.let { spanBuilder.setAttribute("llm.prompt.variant", it) }

        // Metadata
        builder.metadata.forEach { (key, value) ->
            spanBuilder.setAttributeAny("metadata.$key", value)
        }

        // Tags
        if (builder.tags.isNotEmpty()) {
            spanBuilder.setAttribute("tag.tags", builder.tags.toJsonArray())
        }

        // Input messages
        addInputMessages(spanBuilder, builder)

        return OpenTelemetryTracingSpan(spanBuilder.startSpan(), builder.pricing)
    }

    private fun buildInvocationParams(builder: LlmSpanBuilder): String {
        val params = mutableListOf<String>()
        builder.temperature?.let { params.add("\"temperature\":$it") }
        builder.maxTokens?.let { params.add("\"max_tokens\":$it") }
        return if (params.isNotEmpty()) "{${params.joinToString(",")}}" else ""
    }

    private fun addInputMessages(spanBuilder: SpanBuilder, builder: LlmSpanBuilder) {
        var messageIndex = 0

        // System prompt as first message
        builder.systemPrompt?.let { prompt ->
            if (prompt.isNotEmpty()) {
                spanBuilder.setAttribute("llm.input_messages.$messageIndex.message.role", "system")
                spanBuilder.setAttribute(
                    "llm.input_messages.$messageIndex.message.content",
                    prompt.take(MAX_CONTENT_LENGTH)
                )
                messageIndex++
            }
        }

        // Conversation messages
        builder.messages.forEach { msg ->
            spanBuilder.setAttribute("llm.input_messages.$messageIndex.message.role", msg.role)
            spanBuilder.setAttribute(
                "llm.input_messages.$messageIndex.message.content",
                msg.content.take(MAX_CONTENT_LENGTH)
            )
            messageIndex++
        }
    }

    override fun flush() {
        Logger.d(TAG) { "Flushing spans..." }
        tracerProvider.forceFlush().join(FLUSH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    override fun shutdown() {
        Logger.d(TAG) { "Shutting down tracer..." }
        tracerProvider.shutdown().join(FLUSH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private class OpenTelemetryTracingSpan(
        private val span: Span,
        private val pricing: TokenPricing?
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

        override fun complete(fullResponse: String, tokenUsage: TokenUsage?) {
            Logger.d(TAG) { "Completing LLM span - response length: ${fullResponse.length}" }

            // Output message
            span.setAttribute("llm.output_messages.0.message.role", "assistant")
            span.setAttribute("llm.output_messages.0.message.content", fullResponse.take(MAX_CONTENT_LENGTH))

            // Token usage
            tokenUsage?.let { usage ->
                span.setAttribute("llm.token_count.prompt", usage.promptTokens.toLong())
                span.setAttribute("llm.token_count.completion", usage.completionTokens.toLong())
                span.setAttribute("llm.token_count.total", usage.totalTokens.toLong())

                // Cost calculation
                pricing?.let { price ->
                    val cost = price.calculateCost(usage)
                    span.setAttribute("llm.cost.prompt", cost.promptCost)
                    span.setAttribute("llm.cost.completion", cost.completionCost)
                    span.setAttribute("llm.cost.total", cost.totalCost)
                    Logger.d(TAG) { "Cost calculated - total: $${cost.totalCost}" }
                }
            }

            span.setStatus(StatusCode.OK)
            span.end()
        }

        override fun error(exception: Throwable, errorType: String?, retryable: Boolean?) {
            Logger.e(TAG, exception) { "LLM span error - type: $errorType, retryable: $retryable" }
            span.setStatus(StatusCode.ERROR, exception.message ?: "Unknown error")
            span.recordException(exception)
            errorType?.let { span.setAttribute("error.type", it) }
            retryable?.let { span.setAttribute("error.retryable", it) }
            span.end()
        }

        override fun addEvaluation(name: String, score: Double, label: String?) {
            span.setAttribute("evals.$name.score", score)
            label?.let { span.setAttribute("evals.$name.label", it) }
            Logger.d(TAG) { "Added evaluation - $name: $score${label?.let { " ($it)" } ?: ""}" }
        }

        override fun addEvent(name: String, attributes: Map<String, Any>?) {
            if (attributes.isNullOrEmpty()) {
                span.addEvent(name)
            } else {
                val attrs = Attributes.builder()
                attributes.forEach { (key, value) ->
                    when (value) {
                        is String -> attrs.put(AttributeKey.stringKey(key), value)
                        is Long -> attrs.put(AttributeKey.longKey(key), value)
                        is Int -> attrs.put(AttributeKey.longKey(key), value.toLong())
                        is Double -> attrs.put(AttributeKey.doubleKey(key), value)
                        is Boolean -> attrs.put(AttributeKey.booleanKey(key), value)
                        else -> attrs.put(AttributeKey.stringKey(key), value.toString())
                    }
                }
                span.addEvent(name, attrs.build())
            }
        }

        override fun addMetadata(key: String, value: Any) {
            span.setAttributeAny("metadata.$key", value)
        }

        override fun addTags(vararg tags: String) {
            // Note: OTEL doesn't support appending to arrays, so we just set new tags
            span.setAttribute("tag.tags", tags.toList().toJsonArray())
        }
    }

    companion object {
        private const val TAG = "ArizeTracer"
        private const val MAX_CONTENT_LENGTH = 4000
        private const val FLUSH_TIMEOUT_SECONDS = 10L
        private const val DEFAULT_ENDPOINT = "http://localhost:6006/v1/traces"

        // Batch processor defaults
        private const val BATCH_MAX_QUEUE_SIZE = 2048
        private const val BATCH_SCHEDULE_DELAY_MS = 5000L
        private const val BATCH_MAX_EXPORT_SIZE = 512

        /**
         * Creates a new OpenTelemetry-based Arize tracer.
         *
         * @param endpoint The OTLP endpoint URL. Default: Phoenix local (http://localhost:6006/v1/traces)
         * @param serviceName The service name for traces. Default: "app"
         * @param mode Tracing mode: PRODUCTION (batch, async) or TESTING (simple, sync)
         * @param headers Optional HTTP headers (e.g., for Arize Cloud authentication)
         */
        fun create(
            endpoint: String = DEFAULT_ENDPOINT,
            serviceName: String = "app",
            mode: TracingMode = TracingMode.PRODUCTION,
            headers: Map<String, String>? = null
        ): OpenTelemetryArizeTracer {
            Logger.d(TAG) {
                "Creating Arize tracer - endpoint: $endpoint, service: $serviceName, mode: $mode"
            }

            val exporterBuilder: OtlpHttpSpanExporterBuilder = OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint)

            headers?.forEach { (key, value) ->
                exporterBuilder.addHeader(key, value)
            }

            val exporter = exporterBuilder.build()

            val spanProcessor = when (mode) {
                TracingMode.PRODUCTION -> BatchSpanProcessor.builder(exporter)
                    .setMaxQueueSize(BATCH_MAX_QUEUE_SIZE)
                    .setScheduleDelay(BATCH_SCHEDULE_DELAY_MS, TimeUnit.MILLISECONDS)
                    .setMaxExportBatchSize(BATCH_MAX_EXPORT_SIZE)
                    .build()
                TracingMode.TESTING -> SimpleSpanProcessor.create(exporter)
            }

            val tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .build()

            val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build()

            Logger.d(TAG) { "Arize tracer created successfully" }
            return OpenTelemetryArizeTracer(openTelemetry.getTracer(serviceName), tracerProvider, mode)
        }
    }
}

// Extension functions for attribute handling

private fun SpanBuilder.setAttributeAny(key: String, value: Any) {
    when (value) {
        is String -> setAttribute(key, value)
        is Long -> setAttribute(key, value)
        is Int -> setAttribute(key, value.toLong())
        is Double -> setAttribute(key, value)
        is Boolean -> setAttribute(key, value)
        else -> setAttribute(key, value.toString())
    }
}

private fun Span.setAttributeAny(key: String, value: Any) {
    when (value) {
        is String -> setAttribute(key, value)
        is Long -> setAttribute(key, value)
        is Int -> setAttribute(key, value.toLong())
        is Double -> setAttribute(key, value)
        is Boolean -> setAttribute(key, value)
        else -> setAttribute(key, value.toString())
    }
}

private fun Map<String, Any>.toJsonString(): String {
    return entries.joinToString(",", "{", "}") { (k, v) ->
        val valueStr = when (v) {
            is String -> "\"$v\""
            is Number, is Boolean -> v.toString()
            else -> "\"$v\""
        }
        "\"$k\":$valueStr"
    }
}

private fun List<String>.toJsonArray(): String {
    return joinToString(",", "[", "]") { "\"$it\"" }
}

private fun Array<out String>.toJsonArray(): String {
    return joinToString(",", "[", "]") { "\"$it\"" }
}
