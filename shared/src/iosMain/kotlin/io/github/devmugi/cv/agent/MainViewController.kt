package io.github.devmugi.cv.agent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import cvagent.career.generated.resources.Res as CareerRes
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsProvider
import io.github.devmugi.cv.agent.analytics.SwiftAnalytics
import io.github.devmugi.cv.agent.career.data.CareerProjectDataLoader
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.career.models.ProjectDataTimeline
import io.github.devmugi.cv.agent.crashlytics.CrashReporter
import io.github.devmugi.cv.agent.crashlytics.CrashReporterProvider
import io.github.devmugi.cv.agent.crashlytics.SwiftCrashReporter
import io.github.devmugi.cv.agent.di.appModule
import io.github.devmugi.cv.agent.di.createIosModule
import io.github.devmugi.cv.agent.identity.InstallationIdentity
import io.github.devmugi.cv.agent.identity.InstallationIdentityProvider
import io.github.devmugi.cv.agent.identity.SwiftInstallationIdentity
import io.github.devmugi.cv.agent.ui.navigation.AppNavHost
import io.github.devmugi.cv.agent.ui.theme.DEFAULT_THEME
import io.github.devmugi.cv.agent.ui.theme.toColors
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

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

fun initKoin(
    identityProvider: InstallationIdentityProvider? = null,
    analyticsProvider: AnalyticsProvider? = null,
    crashReporterProvider: CrashReporterProvider? = null
) {
    val identity: InstallationIdentity? = identityProvider?.let { SwiftInstallationIdentity(it) }
    val analytics: Analytics? = analyticsProvider?.let { SwiftAnalytics(it) }
    val crashReporter: CrashReporter? = crashReporterProvider?.let { SwiftCrashReporter(it) }

    startKoin {
        modules(appModule, createIosModule(identity, analytics, crashReporter))
    }
}

private data class IosAgentDataResult(
    val dataProvider: AgentDataProvider,
    val timelineProjects: List<ProjectDataTimeline>,
    val projectsMap: Map<String, CareerProject>
)

object ViewControllerFactory : KoinComponent {
    private val json = Json { ignoreUnknownKeys = true }

    fun create() = ComposeUIViewController {
        CVAgentApp()
    }

    @Composable
    private fun CVAgentApp() {
        val toastState = rememberArcaneToastState()
        val analytics: Analytics by inject()
        var agentDataResult by remember { mutableStateOf<IosAgentDataResult?>(null) }

        LaunchedEffect(Unit) {
            if (agentDataResult == null) {
                try {
                    agentDataResult = loadAgentData()
                } catch (e: Exception) {
                    toastState.show("Error loading data: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        val dataResult = agentDataResult ?: return@CVAgentApp

        val viewModel: ChatViewModel by inject { parametersOf(dataResult.dataProvider) }

        ArcaneTheme(colors = DEFAULT_THEME.toColors()) {
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
                onOpenUrl = { url ->
                    NSURL.URLWithString(url)?.let { nsUrl ->
                        UIApplication.sharedApplication.openURL(nsUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private suspend fun loadAgentData(): IosAgentDataResult {
        val personalInfoBytes = CareerRes.readBytes("files/personal_info.json")
        val personalInfoJson = personalInfoBytes.decodeToString()
        val personalInfo = json.decodeFromString<PersonalInfo>(personalInfoJson)

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
            }.getOrNull()
        }.sortedByDescending { it.timelinePosition?.year }

        val dataProvider = AgentDataProvider(
            personalInfo = personalInfo,
            allProjects = fullProjects,
            featuredProjectIds = AgentDataProvider.FEATURED_PROJECT_IDS
        )

        return IosAgentDataResult(dataProvider, timelineProjects, projectsMap)
    }
}

fun MainViewController() = ViewControllerFactory.create()
