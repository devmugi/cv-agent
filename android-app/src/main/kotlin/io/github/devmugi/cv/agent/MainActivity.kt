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
import cvagent.career.generated.resources.Res as CareerRes
import cvagent.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastHost
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastPosition
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.career.data.CareerProjectDataLoader
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.ProjectDataTimeline
import io.github.devmugi.cv.agent.data.repository.CVRepository
import io.github.devmugi.cv.agent.domain.models.CVData
import io.github.devmugi.cv.agent.ui.CareerProjectDetailsScreen
import io.github.devmugi.cv.agent.ui.CareerProjectsTimelineScreen
import io.github.devmugi.cv.agent.ui.ChatScreen
import org.koin.android.ext.android.inject
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

    private val repository: CVRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val toastState = rememberArcaneToastState()

            ArcaneTheme {
                var cvData by remember { mutableStateOf<CVData?>(null) }
                var jsonLoaded by remember { mutableStateOf(false) }
                var currentScreen by remember { mutableStateOf(Screen.Chat) }
                var selectedProject by remember { mutableStateOf<CareerProject?>(null) }
                var careerProjects by remember { mutableStateOf<List<ProjectDataTimeline>>(emptyList()) }
                var careerProjectsMap by remember { mutableStateOf<Map<String, CareerProject>>(emptyMap()) }

                LaunchedEffect(Unit) {
                    if (!jsonLoaded) {
                        // Load CV data
                        val jsonBytes = Res.readBytes("files/cv_data.json")
                        val jsonString = jsonBytes.decodeToString()
                        cvData = repository.getCVData(jsonString)

                        // Load career projects
                        val loader = CareerProjectDataLoader()
                        val fullProjects = mutableMapOf<String, CareerProject>()
                        careerProjects = projectJsonFiles.mapNotNull { path ->
                            try {
                                val bytes = CareerRes.readBytes(path)
                                val jsonString = bytes.decodeToString()
                                val fullProject = loader.loadCareerProject(jsonString)
                                fullProjects[fullProject.id] = fullProject
                                loader.loadProjectTimeline(jsonString)
                            } catch (e: Exception) {
                                android.util.Log.e("CareerProjects", "Failed to load $path: ${e.message}", e)
                                null
                            }
                        }.sortedByDescending { it.timelinePosition?.year }
                        careerProjectsMap = fullProjects

                        jsonLoaded = true
                    }
                }

                val viewModel: ChatViewModel = koinInject { parametersOf({ cvData }) }
                val state by viewModel.state.collectAsState()

                Box {
                    when (currentScreen) {
                        Screen.Chat -> {
                            ChatScreen(
                                state = state,
                                toastState = toastState,
                                onSendMessage = viewModel::sendMessage,
                                cvData = cvData,
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
                                    onBackClick = { currentScreen = Screen.CareerTimeline }
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
