package io.github.devmugi.cv.agent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import cvagent.composeapp.generated.resources.Res
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.repository.CVRepository
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

        ChatScreen(
            viewModel = viewModel,
            cvData = cvData
        )
    }
}

fun MainViewController() = ViewControllerFactory.create()
