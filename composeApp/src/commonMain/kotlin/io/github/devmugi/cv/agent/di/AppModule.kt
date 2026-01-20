package io.github.devmugi.cv.agent.di

import io.github.devmugi.cv.agent.GroqConfig
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.agent.ReferenceExtractor
import io.github.devmugi.cv.agent.agent.SystemPromptBuilder
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.repository.CVDataLoader
import io.github.devmugi.cv.agent.data.repository.CVRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {
    // HTTP Client
    single {
        HttpClient(httpEngineFactory) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Logging) {
                level = LogLevel.BODY
            }
        }
    }

    // API Layer
    single { GroqApiClient(get(), GroqConfig.apiKey) }

    // Data Layer
    single { CVDataLoader() }
    single { CVRepository(get()) }

    // Agent Layer
    single { SystemPromptBuilder() }
    single { ReferenceExtractor(get()) }

    // ViewModel factory - use factory to create new instances
    factory { (cvDataProvider: () -> CVData?) ->
        ChatViewModel(
            apiClient = get(),
            repository = get(),
            promptBuilder = get(),
            referenceExtractor = get(),
            cvDataProvider = cvDataProvider
        )
    }
}
