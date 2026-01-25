package io.github.devmugi.cv.agent.di

import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { (dataProvider: AgentDataProvider?) ->
        ChatViewModel(
            savedStateHandle = get(),
            apiClient = get(),
            promptBuilder = get(),
            suggestionExtractor = get(),
            dataProvider = dataProvider
        )
    }
}
