package io.github.devmugi.cv.agent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cvagent.shared.generated.resources.Res
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.data.repository.CVRepository
import io.github.devmugi.cv.agent.di.appModule
import io.github.devmugi.cv.agent.domain.models.CVData
import io.github.devmugi.cv.agent.ui.ChatScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf

/**
 * Ensures Koin is stopped before starting tests.
 * Call this before each test.
 */
fun ensureKoinStopped() {
    if (GlobalContext.getOrNull() != null) {
        stopKoin()
    }
}

@Composable
fun MainApp() {
    // Initialize the API key for tests
    GroqConfigProvider.initialize(BuildConfig.GROQ_API_KEY)

    KoinApplication(application = { modules(appModule) }) {
        ArcaneTheme {
            val toastState = rememberArcaneToastState()
            var cvData by remember { mutableStateOf<CVData?>(null) }
            var jsonLoaded by remember { mutableStateOf(false) }
            val repository: CVRepository = koinInject()

            LaunchedEffect(Unit) {
                if (!jsonLoaded) {
                    val jsonBytes = Res.readBytes("files/cv_data.json")
                    val jsonString = jsonBytes.decodeToString()
                    cvData = repository.getCVData(jsonString)
                    jsonLoaded = true
                }
            }

            val viewModel: ChatViewModel = koinInject { parametersOf({ cvData }) }
            val state by viewModel.state.collectAsState()

            ChatScreen(
                state = state,
                toastState = toastState,
                onSendMessage = viewModel::sendMessage,
                cvData = cvData,
                onSuggestionClick = viewModel::onSuggestionClicked
            )
        }
    }
}
