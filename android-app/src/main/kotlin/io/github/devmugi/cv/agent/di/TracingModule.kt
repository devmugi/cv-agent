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
                Logger.d(TAG) { "Arize Cloud tracing ENABLED, endpoint: $endpoint" }
                OpenTelemetryArizeTracer.create(
                    endpoint = endpoint,
                    serviceName = "cv-agent-android",
                    mode = TracingMode.PRODUCTION,
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
        Logger.d(TAG) { "Creating GroqApiClient with tracer" }
        GroqApiClient(get(), GroqConfig.apiKey, get<ArizeTracer>())
    }
}
