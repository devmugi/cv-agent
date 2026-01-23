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
import cvagent.career.generated.resources.Res as CareerRes
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastHost
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastPosition
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.career.data.CareerProjectDataLoader
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.ui.ChatScreen
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

private val projectJsonFiles = listOf(
    "files/projects/geosatis_details_data.json",
    "files/projects/mcdonalds_details_data.json",
    "files/projects/adidas_gmr_details_data.json",
    "files/projects/lesara_details_data.json",
    "files/projects/veev_details_data.json",
    "files/projects/food_network_kitchen_details_data.json",
    "files/projects/android_school_details_data.json",
    "files/projects/stoamigo_details_data.json",
    "files/projects/rifl_media_details_data.json",
    "files/projects/smildroid_details_data.json",
    "files/projects/valentina_details_data.json",
    "files/projects/aitweb_details_data.json",
    "files/projects/kntu_it_details_data.json"
)

object ViewControllerFactory : KoinComponent {
    private val json = Json { ignoreUnknownKeys = true }

    fun create() = ComposeUIViewController {
        CVAgentApp()
    }

    @Composable
    private fun CVAgentApp() {
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
                    try {
                        val bytes = CareerRes.readBytes(path)
                        val jsonString = bytes.decodeToString()
                        loader.loadCareerProject(jsonString)
                    } catch (e: Exception) {
                        null
                    }
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

        val viewModel: ChatViewModel by inject { parametersOf(dataProvider) }
        val state by viewModel.state.collectAsState()

        ArcaneTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ChatScreen(
                    state = state,
                    toastState = toastState,
                    onSendMessage = { message -> viewModel.sendMessage(message) },
                    onSuggestionClick = { suggestion -> viewModel.onSuggestionClicked(suggestion) }
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
