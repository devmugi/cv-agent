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
 * When ENABLE_PHOENIX_TRACING is true (debug builds), traces are sent to Phoenix.
 *
 * Configure PHOENIX_HOST in local.properties:
 * - Emulator: Leave empty (defaults to 10.0.2.2)
 * - Real device: Set to your computer's IP (e.g., PHOENIX_HOST=192.168.1.100)
 */
val tracingModule = module {
    single<ArizeTracer> {
        if (BuildConfig.ENABLE_PHOENIX_TRACING) {
            val host = BuildConfig.PHOENIX_HOST.ifEmpty { "10.0.2.2" }
            val endpoint = "http://$host:6006/v1/traces"
            Logger.d(TAG) { "Phoenix tracing ENABLED, endpoint: $endpoint" }
            OpenTelemetryArizeTracer.create(
                endpoint = endpoint,
                serviceName = "cv-agent-android",
                mode = TracingMode.PRODUCTION
            )
        } else {
            Logger.d(TAG) { "Phoenix tracing DISABLED" }
            ArizeTracer.NOOP
        }
    }

    // Override GroqApiClient to include the tracer
    single {
        Logger.d(TAG) { "Creating GroqApiClient with tracer" }
        GroqApiClient(get(), GroqConfig.apiKey, get<ArizeTracer>())
    }
}
