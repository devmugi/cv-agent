package io.github.devmugi.cv.agent.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.devmugi.arize.tracing.ArizeTracer
import io.github.devmugi.cv.agent.GroqConfig
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.crashlytics.CrashReporter
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
import io.github.devmugi.cv.agent.identity.InstallationIdentity
import io.github.devmugi.cv.agent.repository.createDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Creates iOS-specific Koin module with optional Firebase implementations.
 *
 * @param installationIdentity Optional Firebase Installation ID provider (null = in-memory stub)
 * @param analytics Optional Firebase Analytics provider (null = NOOP)
 * @param crashReporter Optional Firebase Crashlytics provider (null = NOOP)
 *
 * Provides:
 * - ArizeTracer.NOOP (tracing disabled on iOS)
 * - DataStore with iOS file path
 * - GroqApiClient with NOOP tracer
 * - ChatViewModel factory
 * - InstallationIdentity (from Swift or stub)
 * - Analytics (from Swift or NOOP)
 * - CrashReporter (from Swift or NOOP)
 */
fun createIosModule(
    installationIdentity: InstallationIdentity?,
    analytics: Analytics?,
    crashReporter: CrashReporter?
): Module = module {
    // Installation Identity - from Swift or fallback to stub
    single<InstallationIdentity> {
        installationIdentity ?: createStubInstallationIdentity()
    }

    // Analytics - from Swift or NOOP
    single<Analytics> {
        analytics ?: Analytics.NOOP
    }

    // Crash Reporter - from Swift or NOOP
    single<CrashReporter> {
        crashReporter ?: CrashReporter.NOOP
    }

    // Tracing - use NOOP on iOS (future: OpenTelemetry iOS)
    single<ArizeTracer> { ArizeTracer.NOOP }

    // DataStore with iOS path
    single<DataStore<Preferences>> { createDataStore() }

    // Override GroqApiClient to include the NOOP tracer
    single {
        GroqApiClient(
            httpClient = get(),
            apiKey = GroqConfig.apiKey,
            tracer = get(),
            rateLimiter = get()
        )
    }

    // ChatViewModel - factory with dataProvider parameter
    factory { params ->
        val dataProvider: AgentDataProvider? = params.getOrNull()
        ChatViewModel(
            savedStateHandle = null, // Not available on iOS
            apiClient = get(),
            promptBuilder = get(),
            suggestionExtractor = get(),
            dataProvider = dataProvider,
            chatRepository = get<ChatRepository>(),
            analytics = get<Analytics>(),
            tracer = get<ArizeTracer>()
        )
    }
}

/**
 * Creates a simple in-memory stub for InstallationIdentity.
 * The ID is stable for the app session but not persisted across restarts.
 */
private fun createStubInstallationIdentity(): InstallationIdentity {
    return object : InstallationIdentity {
        private var cachedId: String? = null

        override suspend fun getInstallationId(): String {
            return cachedId ?: platform.Foundation.NSUUID().UUIDString.also { cachedId = it }
        }
    }
}
