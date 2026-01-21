package io.github.devmugi.cv.agent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import cvagent.shared.generated.resources.Res
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastHost
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastPosition
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.data.repository.CVRepository
import io.github.devmugi.cv.agent.domain.models.CVData
import io.github.devmugi.cv.agent.ui.ChatScreen
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

object ViewControllerFactory : KoinComponent {
    private val repository: CVRepository by inject()

    fun create() = ComposeUIViewController {
        CVAgentApp()
    }

    @Composable
    private fun CVAgentApp() {
        val toastState = rememberArcaneToastState()
        var cvData by remember { mutableStateOf<CVData?>(null) }
        var jsonLoaded by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (!jsonLoaded) {
                val jsonBytes = Res.readBytes("files/cv_data.json")
                val jsonString = jsonBytes.decodeToString()
                cvData = repository.getCVData(jsonString)
                jsonLoaded = true
            }
        }

        val viewModel: ChatViewModel by inject { parametersOf({ cvData }) }
        val state by viewModel.state.collectAsState()

        ArcaneTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ChatScreen(
                    state = state,
                    toastState = toastState,
                    onSendMessage = { message -> viewModel.sendMessage(message) },
                    cvData = cvData,
                    onSuggestionClick = { suggestion -> viewModel.onSuggestionClicked(suggestion) },
                    onRetry = { viewModel.retry() }
                )
                ArcaneToastHost(
                    state = toastState,
                    position = ArcaneToastPosition.BottomCenter
                )
            }
        }
    }
}

fun MainViewController() = ViewControllerFactory.create()
