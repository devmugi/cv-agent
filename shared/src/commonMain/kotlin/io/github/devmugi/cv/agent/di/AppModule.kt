package io.github.devmugi.cv.agent.di

import io.github.devmugi.cv.agent.GroqConfig
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.agent.SuggestionExtractor
import io.github.devmugi.cv.agent.agent.SystemPromptBuilder
import io.github.devmugi.cv.agent.api.GroqApiClient
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
                level = LogLevel.INFO
            }
        }
    }

    // API Layer
    single { GroqApiClient(get(), GroqConfig.apiKey) }

    // Agent Layer
    single { SystemPromptBuilder() }
    single { SuggestionExtractor() }

    // ViewModel factory
    factory { (dataProvider: AgentDataProvider?) ->
        ChatViewModel(
            apiClient = get(),
            promptBuilder = get(),
            suggestionExtractor = get(),
            dataProvider = dataProvider
        )
    }
}
