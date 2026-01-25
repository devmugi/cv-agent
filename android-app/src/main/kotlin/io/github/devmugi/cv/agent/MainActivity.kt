package io.github.devmugi.cv.agent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
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

private enum class ThemeVariant(val displayName: String) {
    ARCANE("Arcane"),
    PERPLEXITY("Perplexity"),
    P2D("P2D"),
    P2L("P2L"),
    CLAUDE_D("Claude Dark"),
    CLAUDE_L("Claude Light"),
    MTG("MTG")
}

private fun ThemeVariant.toColors(): ArcaneColors = when (this) {
    ThemeVariant.ARCANE -> ArcaneColors.default()
    ThemeVariant.PERPLEXITY -> ArcaneColors.perplexity()
    ThemeVariant.P2D -> ArcaneColors.p2d()
    ThemeVariant.P2L -> ArcaneColors.p2l()
    ThemeVariant.CLAUDE_D -> ArcaneColors.claudeD()
    ThemeVariant.CLAUDE_L -> ArcaneColors.claudeL()
    ThemeVariant.MTG -> ArcaneColors.mtg()
}

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
            var currentTheme by rememberSaveable { mutableStateOf(ThemeVariant.PERPLEXITY) }

            ArcaneTheme(colors = currentTheme.toColors()) {
                CVAgentApp(
                    onOpenUrl = { url ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    currentTheme = currentTheme,
                    onThemeChange = { currentTheme = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Suppress("FunctionNaming")
@Composable
private fun CVAgentApp(
    onOpenUrl: (String) -> Unit,
    currentTheme: ThemeVariant,
    onThemeChange: (ThemeVariant) -> Unit
) {
    val toastState = rememberArcaneToastState()
    var agentDataResult by remember { mutableStateOf<AgentDataResult?>(null) }
    var currentScreen by remember { mutableStateOf(Screen.Chat) }
    var selectedProject by remember { mutableStateOf<CareerProject?>(null) }

    LaunchedEffect(Unit) {
        if (agentDataResult == null) {
            agentDataResult = loadAgentData()
        }
    }

    // Wait for data to load before creating ViewModel
    val dataResult = agentDataResult ?: return

    // ViewModel is only created after dataProvider is available
    val viewModel: ChatViewModel = koinInject { parametersOf(dataResult.dataProvider) }
    val state by viewModel.state.collectAsState()

    AppContent(
        currentScreen = currentScreen,
        state = state,
        toastState = toastState,
        viewModel = viewModel,
        careerProjects = dataResult.timelineProjects,
        careerProjectsMap = dataResult.projectsMap,
        selectedProject = selectedProject,
        onScreenChange = { currentScreen = it },
        onProjectSelect = { selectedProject = it },
        onOpenUrl = onOpenUrl,
        currentTheme = currentTheme,
        onThemeChange = onThemeChange
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
            Logger.e("CareerProjects", e) { "Failed to load $path: ${e.message}" }
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
    onOpenUrl: (String) -> Unit,
    currentTheme: ThemeVariant,
    onThemeChange: (ThemeVariant) -> Unit
) {
    Box {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1

                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> direction * fullWidth / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )).togetherWith(
                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) +
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -direction * fullWidth / 3 },
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        )
                )
            },
            label = "screenTransition"
        ) { screen ->
            when (screen) {
                Screen.Chat -> ChatScreen(
                    state = state,
                    toastState = toastState,
                    onSendMessage = viewModel::sendMessage,
                    onSuggestionClick = viewModel::onSuggestionClicked,
                    onClearHistory = viewModel::clearHistory,
                    onNavigateToCareerTimeline = { onScreenChange(Screen.CareerTimeline) },
                    onNavigateToProject = { projectId ->
                        careerProjectsMap[projectId]?.let { project ->
                            onProjectSelect(project)
                            onScreenChange(Screen.ProjectDetails)
                        }
                    }
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
        }
        ArcaneToastHost(
            state = toastState,
            position = ArcaneToastPosition.BottomCenter,
            modifier = Modifier.padding(bottom = 160.dp)
        )

        // Debug-only theme picker FAB
        if (BuildConfig.DEBUG) {
            DebugThemePickerFab(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 240.dp)
            )
        }
    }
}

@Composable
private fun DebugThemePickerFab(
    currentTheme: ThemeVariant,
    onThemeChange: (ThemeVariant) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ArcaneTheme.colors.surfaceContainerHigh,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemeVariant.entries.forEach { theme ->
                        ThemeOptionRow(
                            theme = theme,
                            isSelected = theme == currentTheme,
                            onClick = {
                                onThemeChange(theme)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = ArcaneTheme.colors.primary,
            contentColor = ArcaneTheme.colors.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Theme Picker"
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    theme: ThemeVariant,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) ArcaneTheme.colors.primaryContainer
                else ArcaneTheme.colors.surfaceContainerHigh
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(theme.toColors().primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = theme.displayName,
            style = ArcaneTheme.typography.bodyMedium,
            color = if (isSelected) ArcaneTheme.colors.onPrimaryContainer
            else ArcaneTheme.colors.text,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ArcaneTheme.colors.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
