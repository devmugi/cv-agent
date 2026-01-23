package io.github.devmugi.cv.agent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cvagent.career.generated.resources.Res as CareerRes
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.career.data.CareerProjectDataLoader
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.di.appModule
import io.github.devmugi.cv.agent.ui.ChatScreen
import kotlinx.serialization.json.Json
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf

private val projectJsonFiles = listOf(
    "files/projects/geosatis_details_data.json",
    "files/projects/mcdonalds_details_data.json",
    "files/projects/adidas_gmr_details_data.json"
)

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

    val json = Json { ignoreUnknownKeys = true }

    KoinApplication(application = { modules(appModule) }) {
        ArcaneTheme {
            val toastState = rememberArcaneToastState()
            var dataProvider by remember { mutableStateOf<AgentDataProvider?>(null) }
            var jsonLoaded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (!jsonLoaded) {
                    // Load personal info
                    val personalInfoBytes = CareerRes.readBytes("files/personal_info.json")
                    val personalInfoJson = personalInfoBytes.decodeToString()
                    val personalInfo = json.decodeFromString<PersonalInfo>(personalInfoJson)

                    // Load career projects
                    val loader = CareerProjectDataLoader()
                    val fullProjects = projectJsonFiles.mapNotNull { path ->
                        runCatching {
                            val bytes = CareerRes.readBytes(path)
                            val jsonString = bytes.decodeToString()
                            loader.loadCareerProject(jsonString)
                        }.getOrNull()
                    }

                    // Create AgentDataProvider
                    dataProvider = AgentDataProvider(
                        personalInfo = personalInfo,
                        allProjects = fullProjects,
                        featuredProjectIds = AgentDataProvider.FEATURED_PROJECT_IDS
                    )

                    jsonLoaded = true
                }
            }

            val viewModel: ChatViewModel = koinInject { parametersOf(dataProvider) }
            val state by viewModel.state.collectAsState()

            ChatScreen(
                state = state,
                toastState = toastState,
                onSendMessage = viewModel::sendMessage,
                onSuggestionClick = viewModel::onSuggestionClicked
            )
        }
    }
}
