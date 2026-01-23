package io.github.devmugi.cv.agent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cvagent.career.generated.resources.Res as CareerRes
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastHost
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastPosition
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastState
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
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private enum class Screen { Chat, CareerTimeline, ProjectDetails }

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

private val json = Json { ignoreUnknownKeys = true }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArcaneTheme(colors = ArcaneColors.perplexity()) {
                CVAgentApp(onOpenUrl = { url ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                })
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Suppress("FunctionNaming")
@Composable
private fun CVAgentApp(onOpenUrl: (String) -> Unit) {
    val toastState = rememberArcaneToastState()
    var dataProvider by remember { mutableStateOf<AgentDataProvider?>(null) }
    var jsonLoaded by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(Screen.Chat) }
    var selectedProject by remember { mutableStateOf<CareerProject?>(null) }
    var careerProjects by remember { mutableStateOf<List<ProjectDataTimeline>>(emptyList()) }
    var careerProjectsMap by remember { mutableStateOf<Map<String, CareerProject>>(emptyMap()) }

    LaunchedEffect(Unit) {
        if (!jsonLoaded) {
            val result = loadAgentData()
            dataProvider = result.dataProvider
            careerProjects = result.timelineProjects
            careerProjectsMap = result.projectsMap
            jsonLoaded = true
        }
    }

    val viewModel: ChatViewModel = koinInject { parametersOf(dataProvider) }
    val state by viewModel.state.collectAsState()

    AppContent(
        currentScreen = currentScreen,
        state = state,
        toastState = toastState,
        viewModel = viewModel,
        careerProjects = careerProjects,
        careerProjectsMap = careerProjectsMap,
        selectedProject = selectedProject,
        onScreenChange = { currentScreen = it },
        onProjectSelect = { selectedProject = it },
        onOpenUrl = onOpenUrl
    )
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadAgentData(): AgentDataResult {
    val personalInfoBytes = CareerRes.readBytes("files/personal_info.json")
    val personalInfo = json.decodeFromString<PersonalInfo>(personalInfoBytes.decodeToString())

    val loader = CareerProjectDataLoader()
    val fullProjects = mutableListOf<CareerProject>()
    val projectsMap = mutableMapOf<String, CareerProject>()

    val timelineProjects = projectJsonFiles.mapNotNull { path ->
        runCatching {
            val bytes = CareerRes.readBytes(path)
            val jsonString = bytes.decodeToString()
            val fullProject = loader.loadCareerProject(jsonString)
            fullProjects.add(fullProject)
            projectsMap[fullProject.id] = fullProject
            loader.loadProjectTimeline(jsonString)
        }.onFailure { e ->
            android.util.Log.e("CareerProjects", "Failed to load $path: ${e.message}", e)
        }.getOrNull()
    }.sortedByDescending { it.timelinePosition?.year }

    val dataProvider = AgentDataProvider(
        personalInfo = personalInfo,
        allProjects = fullProjects,
        featuredProjectIds = AgentDataProvider.FEATURED_PROJECT_IDS
    )

    return AgentDataResult(dataProvider, timelineProjects, projectsMap)
}

private data class AgentDataResult(
    val dataProvider: AgentDataProvider,
    val timelineProjects: List<ProjectDataTimeline>,
    val projectsMap: Map<String, CareerProject>
)

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun AppContent(
    currentScreen: Screen,
    state: io.github.devmugi.cv.agent.domain.models.ChatState,
    toastState: ArcaneToastState,
    viewModel: ChatViewModel,
    careerProjects: List<ProjectDataTimeline>,
    careerProjectsMap: Map<String, CareerProject>,
    selectedProject: CareerProject?,
    onScreenChange: (Screen) -> Unit,
    onProjectSelect: (CareerProject?) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Box {
        when (currentScreen) {
            Screen.Chat -> ChatScreen(
                state = state,
                toastState = toastState,
                onSendMessage = viewModel::sendMessage,
                onSuggestionClick = viewModel::onSuggestionClicked,
                onClearHistory = viewModel::clearHistory,
                onNavigateToCareerTimeline = { onScreenChange(Screen.CareerTimeline) }
            )
            Screen.CareerTimeline -> CareerProjectsTimelineScreen(
                projects = careerProjects,
                onProjectClick = { timelineProject ->
                    onProjectSelect(careerProjectsMap[timelineProject.id])
                    onScreenChange(Screen.ProjectDetails)
                },
                onBackClick = { onScreenChange(Screen.Chat) }
            )
            Screen.ProjectDetails -> selectedProject?.let { project ->
                CareerProjectDetailsScreen(
                    project = project,
                    onBackClick = { onScreenChange(Screen.CareerTimeline) },
                    onLinkClick = onOpenUrl
                )
            }
        }
        ArcaneToastHost(
            state = toastState,
            position = ArcaneToastPosition.BottomCenter,
            modifier = Modifier.padding(bottom = 160.dp)
        )
    }
}
