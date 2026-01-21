package io.github.devmugi.cv.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cvagent.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastHost
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastPosition
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.data.repository.CVRepository
import io.github.devmugi.cv.agent.domain.models.CVData
import io.github.devmugi.cv.agent.ui.ChatScreen
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalResourceApi::class)
class MainActivity : ComponentActivity() {

    private val repository: CVRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val toastState = rememberArcaneToastState()

            ArcaneTheme {
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

                val viewModel: ChatViewModel = koinInject { parametersOf({ cvData }) }
                val state by viewModel.state.collectAsState()

                Box {
                    ChatScreen(
                        state = state,
                        toastState = toastState,
                        onSendMessage = viewModel::sendMessage,
                        cvData = cvData,
                        onSuggestionClick = viewModel::onSuggestionClicked,
                        onClearHistory = viewModel::clearHistory
                    )

                    ArcaneToastHost(
                        state = toastState,
                        position = ArcaneToastPosition.BottomCenter,
                        modifier = Modifier.padding(bottom = 160.dp)
                    )
                }
            }
        }
    }
}
