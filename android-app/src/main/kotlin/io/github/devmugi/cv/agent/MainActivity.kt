package io.github.devmugi.cv.agent

import android.content.Intent
import android.net.Uri
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
import cvagent.career.generated.resources.Res as CareerRes
import org.jetbrains.compose.resources.ExperimentalResourceApi
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastHost
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastPosition
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.foundation.theme.ArcaneColors
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.career.data.CareerProjectDataLoader
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.career.models.ProjectDataTimeline
import io.github.devmugi.cv.agent.ui.CareerProjectDetailsScreen
import io.github.devmugi.cv.agent.ui.CareerProjectsTimelineScreen
import io.github.devmugi.cv.agent.ui.ChatScreen
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private enum class Screen {
    Chat,
    CareerTimeline,
    ProjectDetails
}

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

@OptIn(ExperimentalResourceApi::class)
class MainActivity : ComponentActivity() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val toastState = rememberArcaneToastState()

            ArcaneTheme(colors = ArcaneColors.perplexity()) {
                var dataProvider by remember { mutableStateOf<AgentDataProvider?>(null) }
                var jsonLoaded by remember { mutableStateOf(false) }
                var currentScreen by remember { mutableStateOf(Screen.Chat) }
                var selectedProject by remember { mutableStateOf<CareerProject?>(null) }
                var careerProjects by remember { mutableStateOf<List<ProjectDataTimeline>>(emptyList()) }
                var careerProjectsMap by remember { mutableStateOf<Map<String, CareerProject>>(emptyMap()) }

                LaunchedEffect(Unit) {
                    if (!jsonLoaded) {
                        // Load personal info
                        val personalInfoBytes = CareerRes.readBytes("files/personal_info.json")
                        val personalInfoJson = personalInfoBytes.decodeToString()
                        val personalInfo = json.decodeFromString<PersonalInfo>(personalInfoJson)

                        // Load career projects
                        val loader = CareerProjectDataLoader()
                        val fullProjects = mutableListOf<CareerProject>()
                        val projectsMap = mutableMapOf<String, CareerProject>()

                        careerProjects = projectJsonFiles.mapNotNull { path ->
                            try {
                                val bytes = CareerRes.readBytes(path)
                                val jsonString = bytes.decodeToString()
                                val fullProject = loader.loadCareerProject(jsonString)
                                fullProjects.add(fullProject)
                                projectsMap[fullProject.id] = fullProject
                                loader.loadProjectTimeline(jsonString)
                            } catch (e: Exception) {
                                android.util.Log.e("CareerProjects", "Failed to load $path: ${e.message}", e)
                                null
                            }
                        }.sortedByDescending { it.timelinePosition?.year }

                        careerProjectsMap = projectsMap

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

                Box {
                    when (currentScreen) {
                        Screen.Chat -> {
                            ChatScreen(
                                state = state,
                                toastState = toastState,
                                onSendMessage = viewModel::sendMessage,
                                onSuggestionClick = viewModel::onSuggestionClicked,
                                onClearHistory = viewModel::clearHistory,
                                onNavigateToCareerTimeline = { currentScreen = Screen.CareerTimeline }
                            )
                        }
                        Screen.CareerTimeline -> {
                            CareerProjectsTimelineScreen(
                                projects = careerProjects,
                                onProjectClick = { timelineProject ->
                                    selectedProject = careerProjectsMap[timelineProject.id]
                                    currentScreen = Screen.ProjectDetails
                                },
                                onBackClick = { currentScreen = Screen.Chat }
                            )
                        }
                        Screen.ProjectDetails -> {
                            selectedProject?.let { project ->
                                CareerProjectDetailsScreen(
                                    project = project,
                                    onBackClick = { currentScreen = Screen.CareerTimeline },
                                    onLinkClick = { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        startActivity(intent)
                                    }
                                )
                            }
                        }
                    }

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
