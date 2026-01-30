package io.github.devmugi.cv.agent.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.devmugi.arize.tracing.ArizeTracer
import io.github.devmugi.cv.agent.GroqConfig
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
import io.github.devmugi.cv.agent.repository.createDataStore
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * iOS-specific Koin module.
 *
 * Provides:
 * - ArizeTracer.NOOP (tracing disabled on iOS)
 * - DataStore with iOS file path
 * - GroqApiClient with NOOP tracer
 * - ChatViewModel factory
 */
val iosModule = module {
    // Tracing - use NOOP on iOS
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
            analytics = getOrNull<Analytics>() ?: Analytics.NOOP,
            tracer = getOrNull<ArizeTracer>() ?: ArizeTracer.NOOP
        )
    }
}
