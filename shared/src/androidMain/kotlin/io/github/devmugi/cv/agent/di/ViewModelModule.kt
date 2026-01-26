package io.github.devmugi.cv.agent.di

import androidx.lifecycle.SavedStateHandle
import io.github.devmugi.arize.tracing.ArizeTracer
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
import io.github.devmugi.cv.agent.repository.createDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    // DataStore (singleton per app)
    single { createDataStore(androidContext()) }

    viewModel { params ->
        val savedStateHandle: SavedStateHandle = params.get()
        val dataProvider: AgentDataProvider? = params.getOrNull()
        ChatViewModel(
            savedStateHandle = savedStateHandle,
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
