# iOS Navigation Parity Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add 3-screen navigation (Chat → Timeline → Details) to iOS using shared Compose Navigation, achieving feature parity with Android.

**Architecture:** Create `AppNavHost` in shared-ui/commonMain that both Android and iOS use. Platform entry points (`MainActivity`, `MainViewController`) become thin wrappers that provide dependencies and call `AppNavHost`.

**Tech Stack:** JetBrains Navigation Compose (`org.jetbrains.androidx.navigation:navigation-compose`), expect/actual for BackHandler, Compose Multiplatform.

---

## Task 1: Add Navigation Dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared-ui/build.gradle.kts`

**Step 1: Add navigation version and library to libs.versions.toml**

```toml
# In [versions] section, add:
navigation = "2.9.0-alpha01"

# In [libraries] section, add:
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }
```

**Step 2: Add dependency to shared-ui/build.gradle.kts**

In `commonMain.dependencies` block, add:

```kotlin
// Navigation
implementation(libs.navigation.compose)
```

**Step 3: Verify build compiles**

Run: `./gradlew :shared-ui:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml shared-ui/build.gradle.kts
git commit -m "build: add navigation-compose dependency for shared navigation"
```

---

## Task 2: Create BackHandler Expect/Actual

**Files:**
- Create: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/BackHandler.kt`
- Create: `shared-ui/src/androidMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/BackHandler.android.kt`
- Create: `shared-ui/src/iosMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/BackHandler.ios.kt`

**Step 1: Create expect declaration**

Create `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/BackHandler.kt`:

```kotlin
package io.github.devmugi.cv.agent.ui.navigation

import androidx.compose.runtime.Composable

/**
 * Platform-specific back navigation handler.
 * - Android: Uses androidx.activity.compose.BackHandler
 * - iOS: Uses gesture detection or no-op (NavController handles back)
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
```

**Step 2: Create Android actual**

Create `shared-ui/src/androidMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/BackHandler.android.kt`:

```kotlin
package io.github.devmugi.cv.agent.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
```

**Step 3: Create iOS actual**

Create `shared-ui/src/iosMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/BackHandler.ios.kt`:

```kotlin
package io.github.devmugi.cv.agent.ui.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses NavController's built-in back handling
    // Swipe gesture will be added separately via UIKit interop
}
```

**Step 4: Verify build**

Run: `./gradlew :shared-ui:compileKotlinAndroid :shared-ui:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/
git add shared-ui/src/androidMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/
git add shared-ui/src/iosMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/
git commit -m "feat: add PlatformBackHandler expect/actual for navigation"
```

---

## Task 3: Create Navigation Routes

**Files:**
- Create: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/Routes.kt`

**Step 1: Create routes file**

Create `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/Routes.kt`:

```kotlin
package io.github.devmugi.cv.agent.ui.navigation

object Routes {
    const val CHAT = "chat"
    const val TIMELINE = "timeline"
    const val DETAILS = "details/{projectId}"

    fun details(projectId: String) = "details/$projectId"
}
```

**Step 2: Verify build**

Run: `./gradlew :shared-ui:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/Routes.kt
git commit -m "feat: add navigation route constants"
```

---

## Task 4: Create AppNavHost

**Files:**
- Create: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/AppNavHost.kt`

**Step 1: Create AppNavHost composable**

Create `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/AppNavHost.kt`:

```kotlin
package io.github.devmugi.cv.agent.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastHost
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastPosition
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastState
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.ProjectDataTimeline
import io.github.devmugi.cv.agent.domain.models.ChatState
import io.github.devmugi.cv.agent.ui.CareerProjectDetailsScreen
import io.github.devmugi.cv.agent.ui.CareerProjectsTimelineScreen
import io.github.devmugi.cv.agent.ui.ChatScreen
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared navigation host for both Android and iOS.
 * Contains all app screens and navigation logic.
 */
@Composable
fun AppNavHost(
    chatState: StateFlow<ChatState>,
    careerProjects: List<ProjectDataTimeline>,
    careerProjectsMap: Map<String, CareerProject>,
    toastState: ArcaneToastState,
    analytics: Analytics,
    onSendMessage: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onLikeMessage: (String) -> Unit,
    onDislikeMessage: (String) -> Unit,
    onRegenerateMessage: (String) -> Unit,
    onClearHistory: () -> Unit,
    onProjectSuggestionClicked: (String, Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val state by chatState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.CHAT,
            enterTransition = {
                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) +
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
            },
            popEnterTransition = {
                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) +
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth / 3 },
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
            }
        ) {
            composable(Routes.CHAT) {
                ChatScreen(
                    state = state,
                    toastState = toastState,
                    onSendMessage = onSendMessage,
                    analytics = analytics,
                    onSuggestionClick = onSuggestionClick,
                    onCopyMessage = onCopyMessage,
                    onShareMessage = { },
                    onLikeMessage = onLikeMessage,
                    onDislikeMessage = onDislikeMessage,
                    onRegenerateMessage = onRegenerateMessage,
                    onClearHistory = onClearHistory,
                    onNavigateToCareerTimeline = {
                        navController.navigate(Routes.TIMELINE)
                    },
                    onNavigateToProject = { projectId ->
                        careerProjectsMap[projectId]?.let {
                            onProjectSuggestionClicked(projectId, 0)
                            navController.navigate(Routes.details(projectId))
                        }
                    },
                    isRecording = false,
                    isTranscribing = false,
                    onRecordingStart = { toastState.show("Voice input not implemented yet") },
                    onRecordingStop = { },
                    onRequestMicPermission = { toastState.show("Voice input not implemented yet") },
                    hasMicPermission = false
                )
            }

            composable(Routes.TIMELINE) {
                // Handle back press
                PlatformBackHandler(enabled = true) {
                    analytics.logEvent(
                        AnalyticsEvent.Navigation.BackNavigation(
                            fromScreen = "career_timeline",
                            toScreen = "chat",
                            method = AnalyticsEvent.Navigation.NavigationMethod.GESTURE
                        )
                    )
                    navController.popBackStack()
                }

                CareerProjectsTimelineScreen(
                    projects = careerProjects,
                    onProjectClick = { timelineProject ->
                        navController.navigate(Routes.details(timelineProject.id))
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    analytics = analytics
                )
            }

            composable(Routes.DETAILS) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")
                val project = projectId?.let { careerProjectsMap[it] }

                // Handle back press
                PlatformBackHandler(enabled = true) {
                    analytics.logEvent(
                        AnalyticsEvent.Navigation.BackNavigation(
                            fromScreen = "project_details",
                            toScreen = "career_timeline",
                            method = AnalyticsEvent.Navigation.NavigationMethod.GESTURE
                        )
                    )
                    navController.popBackStack()
                }

                if (project != null) {
                    CareerProjectDetailsScreen(
                        project = project,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onLinkClick = onOpenUrl,
                        analytics = analytics
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
```

**Step 2: Verify build**

Run: `./gradlew :shared-ui:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/AppNavHost.kt
git commit -m "feat: add AppNavHost shared navigation component"
```

---

## Task 5: Refactor MainActivity to Use AppNavHost

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

**Step 1: Replace AppContent with AppNavHost**

The key changes:
1. Remove local `Screen` enum and navigation state
2. Remove `AnimatedContent` - navigation library handles this
3. Call `AppNavHost` instead of `AppContent`
4. Keep data loading, theme picker, and voice input logic

Replace the `AppContent` composable call and surrounding code. The file should look like:

```kotlin
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
```

**Step 2: Verify Android build**

Run: `./gradlew :android-app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Test on Android device/emulator**

Run: `./gradlew :android-app:installDebug`
Verify:
- Chat screen loads
- Suggestion chips navigate to project details
- Back button/gesture returns to previous screen
- Theme picker still works

**Step 4: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt
git commit -m "refactor: use shared AppNavHost for Android navigation"
```

---

## Task 6: Refactor MainViewController for iOS

**Files:**
- Modify: `shared/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt`

**Step 1: Update MainViewController to use AppNavHost**

Replace the entire file with:

```kotlin
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
                agentDataResult = loadAgentData()
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
```

**Step 2: Verify iOS build**

Run: `./gradlew :shared:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt
git commit -m "feat: add shared navigation to iOS MainViewController"
```

---

## Task 7: Add iOS Swipe Gesture Support

**Files:**
- Modify: `shared-ui/src/iosMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/BackHandler.ios.kt`

**Step 1: Enhance iOS BackHandler with swipe detection**

For now, keep the simple no-op implementation. The NavController's popBackStack handles back navigation, and the back button in the UI provides explicit navigation. Native iOS swipe gestures can be added as a future enhancement using UIKit interop.

The current implementation is sufficient because:
1. Each screen has a visible back button
2. NavController handles the back stack correctly
3. Full native swipe gesture would require significant UIKit interop

**Step 2: Verify build**

Run: `./gradlew :shared-ui:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL

**Step 3: (Optional) Document future enhancement**

The iOS edge swipe gesture can be added later using `UIScreenEdgePanGestureRecognizer`. This is not critical for initial feature parity since the back button provides the same functionality.

---

## Task 8: Final Verification

**Step 1: Run full Android build**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Run quality checks**

Run: `./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL (no lint/detekt errors)

**Step 3: Run tests**

Run: `./gradlew :shared-ui:testAndroidUnitTest`
Expected: Tests pass

**Step 4: Manual testing checklist**

Android:
- [ ] Chat screen loads with welcome message
- [ ] Can send messages and receive AI responses
- [ ] Suggestion chips appear in responses
- [ ] Tapping suggestion navigates to project details
- [ ] Back button returns to chat
- [ ] System back gesture works
- [ ] Theme picker works (debug build)

iOS (via Xcode):
- [ ] Chat screen loads with welcome message
- [ ] Can send messages and receive AI responses
- [ ] Suggestion chips appear in responses
- [ ] Tapping suggestion navigates to project details
- [ ] Back button returns to chat

**Step 5: Final commit**

```bash
git add .
git commit -m "feat: complete iOS navigation parity with Android"
```

---

## Summary

| Task | Files | Description |
|------|-------|-------------|
| 1 | libs.versions.toml, shared-ui/build.gradle.kts | Add navigation dependency |
| 2 | BackHandler.kt (expect/actual) | Platform back handling |
| 3 | Routes.kt | Navigation route constants |
| 4 | AppNavHost.kt | Shared navigation host |
| 5 | MainActivity.kt | Refactor to use AppNavHost |
| 6 | MainViewController.kt | Add navigation to iOS |
| 7 | BackHandler.ios.kt | iOS swipe gesture (deferred) |
| 8 | N/A | Final verification |

**Estimated LOC changes:**
- New code: ~250 lines (AppNavHost, Routes, BackHandler)
- Removed code: ~150 lines (MainActivity navigation logic)
- Net: ~100 new lines shared between platforms
