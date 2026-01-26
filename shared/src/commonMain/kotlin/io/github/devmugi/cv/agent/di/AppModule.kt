package io.github.devmugi.cv.agent.di

import io.github.devmugi.cv.agent.GroqConfig
import io.github.devmugi.cv.agent.agent.SuggestionExtractor
import io.github.devmugi.cv.agent.agent.SystemPromptBuilder
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
import io.github.devmugi.cv.agent.repository.DataStoreChatRepository
import io.github.devmugi.cv.agent.api.RateLimiter
import io.github.devmugi.cv.agent.api.TokenBucketRateLimiter
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

    // Rate Limiter (shared across all API calls)
    single<RateLimiter> { TokenBucketRateLimiter() }

    // API Layer
    single { GroqApiClient(get(), GroqConfig.apiKey, rateLimiter = get()) }

    // Agent Layer
    single { SystemPromptBuilder() }
    single { SuggestionExtractor() }

    // Repository Layer
    single<ChatRepository> { DataStoreChatRepository(get()) }

    // Note: ChatViewModel is registered in platform-specific viewModelModule
    // to properly handle SavedStateHandle injection
}
