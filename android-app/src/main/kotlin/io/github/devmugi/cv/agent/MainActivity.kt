package io.github.devmugi.cv.agent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import co.touchlab.kermit.Logger
import cvagent.career.generated.resources.Res as CareerRes
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.career.data.CareerProjectDataLoader
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.career.models.ProjectDataTimeline
import io.github.devmugi.cv.agent.ui.navigation.AppNavHost
import io.github.devmugi.cv.agent.ui.theme.DEFAULT_THEME
import io.github.devmugi.cv.agent.ui.theme.ThemeVariant
import io.github.devmugi.cv.agent.ui.theme.isLight
import io.github.devmugi.cv.agent.ui.theme.toColors
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
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

private val json = Json { ignoreUnknownKeys = true }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentTheme by rememberSaveable { mutableStateOf(DEFAULT_THEME) }

            val isLightTheme = currentTheme.isLight()
            SideEffect {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.isAppearanceLightStatusBars = isLightTheme
                windowInsetsController.isAppearanceLightNavigationBars = isLightTheme
            }

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
    val analytics: Analytics = koinInject()
    var agentDataResult by remember { mutableStateOf<AgentDataResult?>(null) }

    LaunchedEffect(Unit) {
        if (agentDataResult == null) {
            agentDataResult = loadAgentData()
        }
    }

    val dataResult = agentDataResult ?: return

    val viewModel: ChatViewModel = koinViewModel { parametersOf(dataResult.dataProvider) }

    Box(modifier = Modifier.fillMaxSize()) {
        AppNavHost(
            chatState = viewModel.state,
            careerProjects = dataResult.timelineProjects,
            careerProjectsMap = dataResult.projectsMap,
            toastState = toastState,
            analytics = analytics,
            onSendMessage = viewModel::sendMessage,
            onSuggestionClick = viewModel::onSuggestionClicked,
            onCopyMessage = viewModel::onMessageCopied,
            onLikeMessage = viewModel::onMessageLiked,
            onDislikeMessage = viewModel::onMessageDisliked,
            onRegenerateMessage = viewModel::onRegenerateClicked,
            onClearHistory = viewModel::clearHistory,
            onProjectSuggestionClicked = viewModel::onProjectSuggestionClicked,
            onOpenUrl = onOpenUrl
        )

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
