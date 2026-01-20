package io.github.devmugi.cv.agent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cvagent.composeapp.generated.resources.Res
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.repository.CVRepository
import io.github.devmugi.cv.agent.di.appModule
import io.github.devmugi.cv.agent.ui.ChatScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun MainApp() {
    KoinApplication(application = { modules(appModule) }) {
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

        ChatScreen(
            viewModel = viewModel,
            cvData = cvData
        )
    }
}
