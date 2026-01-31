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
import io.github.devmugi.cv.agent.ui.theme.DEFAULT_THEME
import io.github.devmugi.cv.agent.ui.theme.toColors
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsProvider
import io.github.devmugi.cv.agent.analytics.SwiftAnalytics
import io.github.devmugi.cv.agent.career.data.CareerProjectDataLoader
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.crashlytics.CrashReporter
import io.github.devmugi.cv.agent.crashlytics.CrashReporterProvider
import io.github.devmugi.cv.agent.crashlytics.SwiftCrashReporter
import io.github.devmugi.cv.agent.di.appModule
import io.github.devmugi.cv.agent.di.createIosModule
import io.github.devmugi.cv.agent.identity.InstallationIdentity
import io.github.devmugi.cv.agent.identity.InstallationIdentityProvider
import io.github.devmugi.cv.agent.identity.SwiftInstallationIdentity
import io.github.devmugi.cv.agent.ui.ChatScreen
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
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

/**
 * Initialize Koin for iOS.
 * Call this from Swift before creating the MainViewController.
 *
 * @param identityProvider Optional Firebase Installations provider from Swift
 * @param analyticsProvider Optional Firebase Analytics provider from Swift
 * @param crashReporterProvider Optional Firebase Crashlytics provider from Swift
 */
fun initKoin(
    identityProvider: InstallationIdentityProvider? = null,
    analyticsProvider: AnalyticsProvider? = null,
    crashReporterProvider: CrashReporterProvider? = null
) {
    // Wrap Swift providers in Kotlin implementations
    val identity: InstallationIdentity? = identityProvider?.let { SwiftInstallationIdentity(it) }
    val analytics: Analytics? = analyticsProvider?.let { SwiftAnalytics(it) }
    val crashReporter: CrashReporter? = crashReporterProvider?.let { SwiftCrashReporter(it) }

    startKoin {
        modules(appModule, createIosModule(identity, analytics, crashReporter))
    }
}

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

        val viewModel: ChatViewModel by inject { parametersOf(dataProvider) }
        val state by viewModel.state.collectAsState()

        ArcaneTheme(colors = DEFAULT_THEME.toColors()) {
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
