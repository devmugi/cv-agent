package io.github.devmugi.cv.agent.di

import android.util.Log
import io.github.devmugi.cv.agent.BuildConfig
import io.github.devmugi.cv.agent.GroqConfig
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.tracing.AgentTracer
import io.github.devmugi.cv.agent.api.tracing.OpenTelemetryAgentTracer
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
    single<AgentTracer> {
        if (BuildConfig.ENABLE_PHOENIX_TRACING) {
            val host = BuildConfig.PHOENIX_HOST.ifEmpty { "10.0.2.2" }
            val endpoint = "http://$host:6006/v1/traces"
            Log.d(TAG, "Phoenix tracing ENABLED, endpoint: $endpoint")
            OpenTelemetryAgentTracer.create(
                endpoint = endpoint,
                serviceName = "cv-agent-android"
            )
        } else {
            Log.d(TAG, "Phoenix tracing DISABLED")
            AgentTracer.NOOP
        }
    }

    // Override GroqApiClient to include the tracer
    single {
        Log.d(TAG, "Creating GroqApiClient with tracer")
        GroqApiClient(get(), GroqConfig.apiKey, get<AgentTracer>())
    }
}
