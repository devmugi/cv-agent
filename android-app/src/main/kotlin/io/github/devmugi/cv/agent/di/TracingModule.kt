package io.github.devmugi.cv.agent.di

import co.touchlab.kermit.Logger
import io.github.devmugi.arize.tracing.ArizeTracer
import io.github.devmugi.arize.tracing.OpenTelemetryArizeTracer
import io.github.devmugi.arize.tracing.models.TracingMode
import io.github.devmugi.cv.agent.BuildConfig
import io.github.devmugi.cv.agent.GroqConfig
import io.github.devmugi.cv.agent.api.GroqApiClient
import org.koin.dsl.module

private const val TAG = "TracingModule"

/**
 * Android-specific DI module for OpenTelemetry tracing.
 *
 * Tracing targets:
 * - dev flavor: Local Phoenix at localhost (http://10.0.2.2:6006/v1/traces)
 * - prod flavor: Arize Cloud with API key authentication
 */
val tracingModule = module {
    single<ArizeTracer> {
        if (BuildConfig.ENABLE_PHOENIX_TRACING) {
            val endpoint = BuildConfig.PHOENIX_ENDPOINT
            val isArizeCloud = BuildConfig.ARIZE_API_KEY.isNotEmpty()

            if (isArizeCloud) {
                Logger.i(TAG) {
                    "Arize Cloud tracing ENABLED\n" +
                        "  endpoint: $endpoint\n" +
                        "  space_id: ${BuildConfig.ARIZE_SPACE_ID.take(15)}...\n" +
                        "  api_key: ${BuildConfig.ARIZE_API_KEY.take(15)}..."
                }
                OpenTelemetryArizeTracer.create(
                    endpoint = endpoint,
                    serviceName = "cv-agent-android",
                    projectName = "cv-agent", // Required for Arize Cloud
                    mode = TracingMode.TESTING, // Use sync export for debugging
                    headers = mapOf(
                        "space_id" to BuildConfig.ARIZE_SPACE_ID,
                        "api_key" to BuildConfig.ARIZE_API_KEY
                    )
                )
            } else {
                Logger.d(TAG) { "Local Phoenix tracing ENABLED, endpoint: $endpoint" }
                OpenTelemetryArizeTracer.create(
                    endpoint = endpoint,
                    serviceName = "cv-agent-android",
                    mode = TracingMode.PRODUCTION
                )
            }
        } else {
            Logger.d(TAG) { "Tracing DISABLED" }
            ArizeTracer.NOOP
        }
    }

    // Override GroqApiClient to include the tracer
    single {
        val tracer = get<ArizeTracer>()
        Logger.i(TAG) { "Creating GroqApiClient with tracer: ${tracer::class.simpleName}" }
        GroqApiClient(get(), GroqConfig.apiKey, tracer = tracer, rateLimiter = get())
    }
}
