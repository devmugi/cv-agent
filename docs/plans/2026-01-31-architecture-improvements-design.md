# Architecture Improvements PRD

**Project:** CV Agent
**Date:** 2026-01-31
**Branch:** architecture
**Status:** Approved

---

## Overview

A 4-phase improvement plan focused on code quality and maintainability, delivered incrementally. Each phase addresses a single architectural concern with clear deliverables and success criteria.

### Goals

1. **Design System:** Eliminate duplicate color definitions, establish single source of truth
2. **Navigation:** Type-safe routes with Navigation 3, compile-time validation
3. **Adaptive Layouts:** Tablet/foldable support via WindowSizeClass
4. **Testing:** Screenshot coverage for new layouts, integration tests for API

### Non-Goals

- Localization/i18n (separate initiative)
- R8/ProGuard configuration (separate initiative)
- Baseline profiles (future phase)
- Deep linking implementation (routes will be ready, not wired)

---

## Phase Dependencies

```
Phase 1 (Design System)
    ↓
Phase 2 (Navigation 3)
    ↓
Phase 3 (Adaptive Layouts)  ← uses Navigation 3 + CareerColors
    ↓
Phase 4 (Testing)  ← screenshots for Phase 3 layouts
```

Each phase merges to `main` before starting the next.

---

## Phase 1: Design System Consolidation

### Problem

10+ files contain duplicate `AmberColor = Color(0xFFFFC107)` definitions. No centralized color tokens for the career/CV domain. Changes require updating multiple files.

### Solution

Create `CareerColors` object as single source of truth. Wrap in `CvAgentColors` for app-wide semantic tokens. Add `@Immutable` annotations for Compose stability.

### Deliverables

```
shared-career-projects/
└── src/commonMain/kotlin/io/github/devmugi/cv/agent/career/theme/
    ├── CareerColors.kt      # Primary color definitions
    └── CvAgentColors.kt     # Semantic wrapper (career, chat, status)

shared-ui/
└── src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/
    └── LocalCvAgentColors.kt  # CompositionLocal provider
```

### Implementation

**CareerColors.kt:**
```kotlin
package io.github.devmugi.cv.agent.career.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
object CareerColors {
    val Amber = Color(0xFFFFC107)
    val AmberLight = Color(0xFFFFD54F)
    val AmberDark = Color(0xFFFFA000)
    val Background = Color(0xFFFFF8E1)
    val BackgroundDark = Color(0xFF3E2723)
}
```

**CvAgentColors.kt:**
```kotlin
package io.github.devmugi.cv.agent.career.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class CvAgentColors(
    val career: CareerColorTokens = CareerColorTokens(),
    val status: StatusColorTokens = StatusColorTokens()
)

@Immutable
data class CareerColorTokens(
    val primary: Color = CareerColors.Amber,
    val primaryLight: Color = CareerColors.AmberLight,
    val background: Color = CareerColors.Background
)

@Immutable
data class StatusColorTokens(
    val success: Color = Color(0xFF4CAF50),
    val warning: Color = Color(0xFFFF9800),
    val error: Color = Color(0xFFF44336)
)
```

**LocalCvAgentColors.kt:**
```kotlin
package io.github.devmugi.cv.agent.ui.theme

import androidx.compose.runtime.compositionLocalOf
import io.github.devmugi.cv.agent.career.theme.CvAgentColors

val LocalCvAgentColors = compositionLocalOf { CvAgentColors() }
```

### Migration Pattern

```kotlin
// Before (in 10+ files)
private val AmberColor = Color(0xFFFFC107)
Icon(tint = AmberColor)

// After
Icon(tint = CareerColors.Amber)
// Or via semantic tokens
Icon(tint = LocalCvAgentColors.current.career.primary)
```

### Files to Migrate

| File | Duplicate Colors |
|------|------------------|
| CareerProjectCard.kt | AmberColor, AmberLight |
| TimelineItem.kt | AmberColor |
| SkillChip.kt | AmberColor |
| ProjectHeader.kt | AmberColor, Background |
| ExperienceCard.kt | AmberColor |
| EducationCard.kt | AmberColor |
| CertificationBadge.kt | AmberColor |
| AchievementItem.kt | AmberColor |
| ContactInfo.kt | AmberColor |
| SocialLinks.kt | AmberColor |

### Success Criteria

- [ ] Zero duplicate color definitions in codebase
- [ ] All colors accessed via `CareerColors.*` or semantic tokens
- [ ] `@Immutable` annotation on all color data classes
- [ ] Existing screenshot tests pass without visual changes
- [ ] No direct `Color(0xFF...)` literals in UI components

### Estimated Scope

- Files touched: ~15
- New files: 3
- Lines changed: ~200

---

## Phase 2: Type-Safe Navigation (Navigation 3)

### Problem

Current navigation uses manual enum-based screen switching. No compile-time route validation. String-based or enum matching prone to errors.

### Solution

Migrate to Navigation 3 with `@Serializable` route classes. Type-safe navigation with compile-time validation. Prepares for deep linking without wiring it.

### Dependencies

Add to `gradle/libs.versions.toml`:

```toml
[versions]
navigation3 = "1.0.0-alpha06"

[libraries]
navigation3-runtime = { module = "org.jetbrains.androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
```

Add to `shared-ui/build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
}
```

### Deliverables

```
shared-ui/
└── src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/
    ├── Routes.kt           # @Serializable route definitions
    └── AppNavHost.kt       # NavHost setup
```

### Implementation

**Routes.kt:**
```kotlin
package io.github.devmugi.cv.agent.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Chat : Route

    @Serializable
    data class Project(val id: String) : Route

    @Serializable
    data object Settings : Route
}
```

**AppNavHost.kt:**
```kotlin
package io.github.devmugi.cv.agent.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavHost
import androidx.navigation3.runtime.composable
import androidx.navigation3.runtime.rememberNavController
import androidx.navigation3.runtime.toRoute
import io.github.devmugi.cv.agent.ui.screens.ChatScreen
import io.github.devmugi.cv.agent.ui.screens.ProjectScreen
import io.github.devmugi.cv.agent.ui.screens.SettingsScreen

@Composable
fun AppNavHost(
    startDestination: Route = Route.Chat
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Route.Chat> {
            ChatScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Route.Project(projectId))
                },
                onSettingsClick = {
                    navController.navigate(Route.Settings)
                }
            )
        }

        composable<Route.Project> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Project>()
            ProjectScreen(
                projectId = route.id,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

### Migration Pattern

```kotlin
// Before (enum-based)
enum class Screen { CHAT, PROJECT, SETTINGS }

var currentScreen by remember { mutableStateOf(Screen.CHAT) }

when (currentScreen) {
    Screen.CHAT -> ChatScreen(onProjectClick = { currentScreen = Screen.PROJECT })
    Screen.PROJECT -> ProjectScreen()
    Screen.SETTINGS -> SettingsScreen()
}

// After (Navigation 3)
AppNavHost(startDestination = Route.Chat)
```

### Success Criteria

- [ ] All routes defined as `@Serializable` classes
- [ ] No string-based route matching
- [ ] Compile-time validation for route parameters
- [ ] Back navigation works correctly
- [ ] Project ID passed type-safely to ProjectScreen

### Estimated Scope

- Files touched: ~5
- New files: 2
- Lines changed: ~150

---

## Phase 3: Adaptive Layouts (WindowSizeClass)

### Problem

App only supports phone layouts. No tablet or foldable support. Fixed navigation pattern regardless of screen size.

### Solution

Implement `WindowSizeClass` with three breakpoints. Adaptive navigation (bottom nav → rail → drawer). Responsive ChatScreen layout for larger screens.

### Deliverables

```
shared-ui/
└── src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/
    ├── WindowSizeClass.kt       # Enum + calculation
    ├── AdaptiveNavigation.kt    # Navigation variants
    └── AdaptiveScaffold.kt      # Layout wrapper

shared-ui/
└── src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/screens/
    └── ChatScreen.kt            # Updated with adaptive support
```

### Implementation

**WindowSizeClass.kt:**
```kotlin
package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowWidthSizeClass {
    Compact,   // < 600dp (phones)
    Medium,    // 600-839dp (small tablets, foldables)
    Expanded   // >= 840dp (large tablets, desktop)
}

@Composable
fun calculateWindowSizeClass(): WindowWidthSizeClass {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp < 600 -> WindowWidthSizeClass.Compact
        configuration.screenWidthDp < 840 -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }
}
```

**AdaptiveNavigation.kt:**
```kotlin
package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import io.github.devmugi.cv.agent.ui.navigation.Route

@Composable
fun AdaptiveNavigation(
    windowSizeClass: WindowWidthSizeClass,
    currentRoute: Route,
    onNavigate: (Route) -> Unit,
    content: @Composable () -> Unit
) {
    when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> {
            Scaffold(
                bottomBar = {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = onNavigate
                    )
                }
            ) { paddingValues ->
                content()
            }
        }

        WindowWidthSizeClass.Medium -> {
            Row {
                NavRail(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate
                )
                content()
            }
        }

        WindowWidthSizeClass.Expanded -> {
            PermanentNavigationDrawer(
                drawerContent = {
                    NavDrawer(
                        currentRoute = currentRoute,
                        onNavigate = onNavigate
                    )
                }
            ) {
                content()
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    currentRoute: Route,
    onNavigate: (Route) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute is Route.Chat,
            onClick = { onNavigate(Route.Chat) },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
            label = { Text("Chat") }
        )
        NavigationBarItem(
            selected = currentRoute is Route.Settings,
            onClick = { onNavigate(Route.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

@Composable
private fun NavRail(
    currentRoute: Route,
    onNavigate: (Route) -> Unit
) {
    NavigationRail {
        NavigationRailItem(
            selected = currentRoute is Route.Chat,
            onClick = { onNavigate(Route.Chat) },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
            label = { Text("Chat") }
        )
        NavigationRailItem(
            selected = currentRoute is Route.Settings,
            onClick = { onNavigate(Route.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

@Composable
private fun NavDrawer(
    currentRoute: Route,
    onNavigate: (Route) -> Unit
) {
    PermanentDrawerSheet {
        NavigationDrawerItem(
            selected = currentRoute is Route.Chat,
            onClick = { onNavigate(Route.Chat) },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
            label = { Text("Chat") }
        )
        NavigationDrawerItem(
            selected = currentRoute is Route.Settings,
            onClick = { onNavigate(Route.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}
```

**AdaptiveScaffold.kt:**
```kotlin
package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.runtime.Composable
import io.github.devmugi.cv.agent.ui.navigation.Route

@Composable
fun AdaptiveScaffold(
    currentRoute: Route,
    onNavigate: (Route) -> Unit,
    content: @Composable () -> Unit
) {
    val windowSizeClass = calculateWindowSizeClass()

    AdaptiveNavigation(
        windowSizeClass = windowSizeClass,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        content = content
    )
}
```

### ChatScreen Adaptations

| Size Class | Layout |
|------------|--------|
| Compact | Full-width chat, floating input at bottom |
| Medium | Chat with collapsible project sidebar (40% width) |
| Expanded | Two-pane: chat (60%) + project details (40%) |

```kotlin
@Composable
fun ChatScreen(
    windowSizeClass: WindowWidthSizeClass = calculateWindowSizeClass(),
    state: ChatState,
    onSendMessage: (String) -> Unit,
    onProjectClick: (String) -> Unit
) {
    when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> {
            CompactChatLayout(state, onSendMessage, onProjectClick)
        }
        WindowWidthSizeClass.Medium -> {
            MediumChatLayout(state, onSendMessage, onProjectClick)
        }
        WindowWidthSizeClass.Expanded -> {
            ExpandedChatLayout(state, onSendMessage, onProjectClick)
        }
    }
}
```

### Success Criteria

- [ ] Layouts respond to window size changes (foldable unfold)
- [ ] Navigation style adapts: bottom bar → rail → drawer
- [ ] No horizontal scrolling on any form factor
- [ ] ChatScreen shows sidebar on Medium/Expanded
- [ ] WindowSizeClass passed as parameter for testability

### Estimated Scope

- Files touched: ~8
- New files: 3
- Lines changed: ~350

---

## Phase 4: Testing Expansion

### Problem

No screenshot tests for adaptive layouts. No integration tests for API client. Limited error path coverage.

### Solution

Add Roborazzi screenshot tests for all breakpoints and navigation variants. Add integration tests for GroqApiClient with real/mock endpoints.

### Deliverables

```
shared-ui-screenshots/
└── src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/
    ├── adaptive/
    │   ├── WindowSizeClassScreenshotTest.kt    # Layout at each breakpoint
    │   ├── AdaptiveNavigationScreenshotTest.kt # Nav variants
    │   └── ChatScreenAdaptiveTest.kt           # Chat at each size
    └── components/
        └── CareerColorsScreenshotTest.kt       # Color token validation

shared-agent-api/
└── src/commonTest/kotlin/io/github/devmugi/cv/agent/api/
    ├── GroqApiClientIntegrationTest.kt   # Real endpoint tests
    └── GroqApiClientErrorTest.kt         # Error path coverage
```

### Screenshot Tests

**WindowSizeClassScreenshotTest.kt:**
```kotlin
package io.github.devmugi.cv.agent.ui.screenshots.adaptive

import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.devmugi.cv.agent.ui.adaptive.WindowWidthSizeClass
import io.github.devmugi.cv.agent.ui.screens.ChatScreen
import io.github.devmugi.cv.agent.ui.theme.ArcaneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WindowSizeClassScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testState = ChatScreenTestData.defaultState()

    @Test
    fun chatScreen_compact() {
        composeTestRule.setContent {
            ArcaneTheme {
                ChatScreen(
                    windowSizeClass = WindowWidthSizeClass.Compact,
                    state = testState,
                    onSendMessage = {},
                    onProjectClick = {}
                )
            }
        }
        captureRoboImage("ChatScreen_Compact")
    }

    @Test
    fun chatScreen_medium() {
        composeTestRule.setContent {
            ArcaneTheme {
                ChatScreen(
                    windowSizeClass = WindowWidthSizeClass.Medium,
                    state = testState,
                    onSendMessage = {},
                    onProjectClick = {}
                )
            }
        }
        captureRoboImage("ChatScreen_Medium")
    }

    @Test
    fun chatScreen_expanded() {
        composeTestRule.setContent {
            ArcaneTheme {
                ChatScreen(
                    windowSizeClass = WindowWidthSizeClass.Expanded,
                    state = testState,
                    onSendMessage = {},
                    onProjectClick = {}
                )
            }
        }
        captureRoboImage("ChatScreen_Expanded")
    }
}
```

**AdaptiveNavigationScreenshotTest.kt:**
```kotlin
@Test
fun navigation_bottomBar_compact() {
    composeTestRule.setContent {
        ArcaneTheme {
            AdaptiveNavigation(
                windowSizeClass = WindowWidthSizeClass.Compact,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }
    captureRoboImage("Navigation_BottomBar")
}

@Test
fun navigation_rail_medium() {
    composeTestRule.setContent {
        ArcaneTheme {
            AdaptiveNavigation(
                windowSizeClass = WindowWidthSizeClass.Medium,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }
    captureRoboImage("Navigation_Rail")
}

@Test
fun navigation_drawer_expanded() {
    composeTestRule.setContent {
        ArcaneTheme {
            AdaptiveNavigation(
                windowSizeClass = WindowWidthSizeClass.Expanded,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }
    captureRoboImage("Navigation_Drawer")
}
```

### Integration Tests

**GroqApiClientIntegrationTest.kt:**
```kotlin
package io.github.devmugi.cv.agent.api

import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.domain.models.Message
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class GroqApiClientIntegrationTest {

    private val client = GroqApiClient(
        config = GroqConfig.fromEnvironment()
    )

    @Test
    fun streamCompletion_returnsChunks() = runTest {
        // Skip if no API key
        if (System.getenv("GROQ_API_KEY").isNullOrBlank()) {
            println("Skipping: GROQ_API_KEY not set")
            return@runTest
        }

        val chunks = mutableListOf<String>()
        var completed = false

        client.streamCompletion(
            messages = listOf(
                Message(role = "user", content = "Say hello in one word")
            ),
            onChunk = { chunk -> chunks.add(chunk) },
            onComplete = { completed = true },
            onError = { error -> fail("Unexpected error: $error") }
        )

        assertTrue(chunks.isNotEmpty(), "Expected at least one chunk")
        assertTrue(completed, "Expected completion callback")
    }

    @Test
    fun streamCompletion_handlesLongResponse() = runTest {
        if (System.getenv("GROQ_API_KEY").isNullOrBlank()) return@runTest

        val chunks = mutableListOf<String>()

        client.streamCompletion(
            messages = listOf(
                Message(role = "user", content = "List 5 programming languages")
            ),
            onChunk = { chunks.add(it) },
            onComplete = {},
            onError = { fail("Unexpected error: $it") }
        )

        val response = chunks.joinToString("")
        assertTrue(response.length > 50, "Expected substantial response")
    }
}
```

**GroqApiClientErrorTest.kt:**
```kotlin
package io.github.devmugi.cv.agent.api

import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.GroqConfig
import io.github.devmugi.cv.agent.domain.models.Message
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroqApiClientErrorTest {

    @Test
    fun invalidApiKey_returnsAuthError() = runTest {
        val client = GroqApiClient(
            config = GroqConfig(apiKey = "invalid-key")
        )

        var errorType: String? = null

        client.streamCompletion(
            messages = listOf(Message(role = "user", content = "Hello")),
            onChunk = {},
            onComplete = {},
            onError = { error -> errorType = error.type }
        )

        assertEquals("auth", errorType)
    }

    @Test
    fun emptyMessages_returnsApiError() = runTest {
        val client = GroqApiClient(config = GroqConfig.fromEnvironment())

        var errorReceived = false

        client.streamCompletion(
            messages = emptyList(),
            onChunk = {},
            onComplete = {},
            onError = { errorReceived = true }
        )

        assertTrue(errorReceived, "Expected error for empty messages")
    }

    @Test
    fun timeout_returnsTimeoutError() = runTest {
        val client = GroqApiClient(
            config = GroqConfig.fromEnvironment().copy(timeoutMs = 1)
        )

        var errorType: String? = null

        client.streamCompletion(
            messages = listOf(Message(role = "user", content = "Hello")),
            onChunk = {},
            onComplete = {},
            onError = { error -> errorType = error.type }
        )

        assertEquals("timeout", errorType)
    }
}
```

### Test Matrix

| Test Category | Count | Coverage |
|---------------|-------|----------|
| ChatScreen breakpoints | 3 | Compact, Medium, Expanded |
| Navigation variants | 3 | BottomBar, Rail, Drawer |
| Navigation states | 3 | Chat selected, Settings selected, Project active |
| Color token validation | 2 | Light theme, Dark theme |
| API integration | 3 | Basic, long response, multi-turn |
| API error paths | 4 | Auth, timeout, rate limit, network |

**Total new tests: ~18**

### Success Criteria

- [ ] Screenshot tests for 3 breakpoints × 3 navigation styles
- [ ] ChatScreen screenshots at Compact, Medium, Expanded
- [ ] CareerColors visual validation screenshots
- [ ] Integration tests pass with real Groq API
- [ ] Integration tests skipped gracefully without API key
- [ ] Error paths tested: auth, timeout, rate limit, empty input

### Estimated Scope

- Files touched: ~6
- New files: 6
- Lines changed: ~400

---

## Summary

| Phase | Focus | Files | LOC | Dependencies |
|-------|-------|-------|-----|--------------|
| 1 | Design System | ~15 | ~200 | None |
| 2 | Navigation 3 | ~5 | ~150 | Phase 1 |
| 3 | Adaptive Layouts | ~8 | ~350 | Phase 2 |
| 4 | Testing | ~6 | ~400 | Phase 3 |

**Total:** ~34 files, ~1100 lines changed

### Branch Strategy

```
main
├── feature/phase-1-design-system
│   └── merge to main
├── feature/phase-2-navigation-3
│   └── merge to main
├── feature/phase-3-adaptive-layouts
│   └── merge to main
└── feature/phase-4-testing
    └── merge to main
```

### Rollout

Each phase is independently deployable. If issues arise, previous phases remain stable.

---

## Appendix: File Inventory

### Phase 1 Files
- `shared-career-projects/src/commonMain/.../theme/CareerColors.kt` (new)
- `shared-career-projects/src/commonMain/.../theme/CvAgentColors.kt` (new)
- `shared-ui/src/commonMain/.../theme/LocalCvAgentColors.kt` (new)
- `shared-career-projects/src/commonMain/.../components/CareerProjectCard.kt` (modify)
- `shared-career-projects/src/commonMain/.../components/TimelineItem.kt` (modify)
- (+ 10 more component files)

### Phase 2 Files
- `gradle/libs.versions.toml` (modify)
- `shared-ui/build.gradle.kts` (modify)
- `shared-ui/src/commonMain/.../navigation/Routes.kt` (new)
- `shared-ui/src/commonMain/.../navigation/AppNavHost.kt` (new)
- `shared-ui/src/commonMain/.../App.kt` (modify)

### Phase 3 Files
- `shared-ui/src/commonMain/.../adaptive/WindowSizeClass.kt` (new)
- `shared-ui/src/commonMain/.../adaptive/AdaptiveNavigation.kt` (new)
- `shared-ui/src/commonMain/.../adaptive/AdaptiveScaffold.kt` (new)
- `shared-ui/src/commonMain/.../screens/ChatScreen.kt` (modify)
- `shared-ui/src/commonMain/.../screens/CompactChatLayout.kt` (new)
- `shared-ui/src/commonMain/.../screens/MediumChatLayout.kt` (new)
- `shared-ui/src/commonMain/.../screens/ExpandedChatLayout.kt` (new)

### Phase 4 Files
- `shared-ui-screenshots/src/test/.../adaptive/WindowSizeClassScreenshotTest.kt` (new)
- `shared-ui-screenshots/src/test/.../adaptive/AdaptiveNavigationScreenshotTest.kt` (new)
- `shared-ui-screenshots/src/test/.../adaptive/ChatScreenAdaptiveTest.kt` (new)
- `shared-ui-screenshots/src/test/.../components/CareerColorsScreenshotTest.kt` (new)
- `shared-agent-api/src/commonTest/.../GroqApiClientIntegrationTest.kt` (new)
- `shared-agent-api/src/commonTest/.../GroqApiClientErrorTest.kt` (new)

---

*Document created: 2026-01-31*
*Status: Approved*
