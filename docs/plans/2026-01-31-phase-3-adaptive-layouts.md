# Phase 3: Adaptive Layouts (WindowSizeClass) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Support tablet/foldable form factors with responsive navigation and layouts using WindowSizeClass.

**Architecture:** Create WindowSizeClass enum with breakpoints, adaptive navigation components (BottomNav/Rail/Drawer), and responsive ChatScreen layouts.

**Tech Stack:** Kotlin, Compose Multiplatform, Material 3 Adaptive

---

## Files Overview

**Create:**
- `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/WindowSizeClass.kt`
- `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/AdaptiveScaffold.kt`

**Modify:**
- `android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

---

## Task 1: Create WindowSizeClass

**Files:**
- Create: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/WindowSizeClass.kt`

**Step 1: Create adaptive directory**

```bash
mkdir -p shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive
```

**Step 2: Create WindowSizeClass.kt**

```kotlin
package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size class breakpoints following Material Design 3 guidelines.
 *
 * - Compact: < 600dp (phones in portrait)
 * - Medium: 600-839dp (tablets in portrait, foldables)
 * - Expanded: >= 840dp (tablets in landscape, desktop)
 */
@Immutable
enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded;

    companion object {
        private val CompactMaxWidth = 600.dp
        private val MediumMaxWidth = 840.dp

        /**
         * Calculate WindowWidthSizeClass from screen width.
         */
        fun fromWidth(width: Dp): WindowWidthSizeClass = when {
            width < CompactMaxWidth -> Compact
            width < MediumMaxWidth -> Medium
            else -> Expanded
        }
    }
}

/**
 * Calculate the current window size class.
 * Uses BoxWithConstraints internally to measure available width.
 */
@Composable
expect fun calculateWindowSizeClass(): WindowWidthSizeClass
```

**Step 3: Create Android implementation**

Create: `shared-ui/src/androidMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/WindowSizeClass.android.kt`

```kotlin
package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
actual fun calculateWindowSizeClass(): WindowWidthSizeClass {
    val configuration = LocalConfiguration.current
    return WindowWidthSizeClass.fromWidth(configuration.screenWidthDp.dp)
}
```

**Step 4: Create iOS implementation**

Create: `shared-ui/src/iosMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/WindowSizeClass.ios.kt`

```kotlin
package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun calculateWindowSizeClass(): WindowWidthSizeClass {
    val windowInfo = LocalWindowInfo.current
    val widthDp = windowInfo.containerSize.width.dp
    return WindowWidthSizeClass.fromWidth(widthDp)
}
```

**Step 5: Verify compilation**

Run: `./gradlew :shared-ui:compileAndroidMain --quiet`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/WindowSizeClass.kt
git add shared-ui/src/androidMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/WindowSizeClass.android.kt
git add shared-ui/src/iosMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/WindowSizeClass.ios.kt
git commit -m "feat(adaptive): add WindowSizeClass with platform implementations"
```

---

## Task 2: Create AdaptiveScaffold

**Files:**
- Create: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/AdaptiveScaffold.kt`

**Step 1: Create AdaptiveScaffold.kt**

```kotlin
package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.devmugi.cv.agent.ui.navigation.Route

/**
 * Navigation destination for adaptive navigation components.
 */
@Immutable
data class NavDestination(
    val route: Route,
    val icon: ImageVector,
    val label: String
)

/**
 * Default navigation destinations.
 */
val defaultDestinations = listOf(
    NavDestination(Route.Chat, Icons.Default.Chat, "Chat"),
    NavDestination(Route.CareerTimeline, Icons.Default.Timeline, "Career")
)

/**
 * Adaptive scaffold that switches navigation style based on window size:
 * - Compact: Bottom navigation bar
 * - Medium: Navigation rail
 * - Expanded: Permanent navigation drawer
 */
@Composable
fun AdaptiveScaffold(
    windowSizeClass: WindowWidthSizeClass,
    currentRoute: Route,
    onNavigate: (Route) -> Unit,
    destinations: List<NavDestination> = defaultDestinations,
    content: @Composable () -> Unit
) {
    when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> {
            Scaffold(
                bottomBar = {
                    AdaptiveBottomBar(
                        currentRoute = currentRoute,
                        destinations = destinations,
                        onNavigate = onNavigate
                    )
                }
            ) { paddingValues ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(paddingValues)
                ) {
                    content()
                }
            }
        }
        WindowWidthSizeClass.Medium -> {
            Row(modifier = Modifier.fillMaxSize()) {
                AdaptiveNavRail(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigate = onNavigate
                )
                content()
            }
        }
        WindowWidthSizeClass.Expanded -> {
            PermanentNavigationDrawer(
                drawerContent = {
                    AdaptiveNavDrawer(
                        currentRoute = currentRoute,
                        destinations = destinations,
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
private fun AdaptiveBottomBar(
    currentRoute: Route,
    destinations: List<NavDestination>,
    onNavigate: (Route) -> Unit
) {
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = isRouteSelected(currentRoute, destination.route),
                onClick = { onNavigate(destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
private fun AdaptiveNavRail(
    currentRoute: Route,
    destinations: List<NavDestination>,
    onNavigate: (Route) -> Unit
) {
    NavigationRail {
        destinations.forEach { destination ->
            NavigationRailItem(
                selected = isRouteSelected(currentRoute, destination.route),
                onClick = { onNavigate(destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
private fun AdaptiveNavDrawer(
    currentRoute: Route,
    destinations: List<NavDestination>,
    onNavigate: (Route) -> Unit
) {
    PermanentDrawerSheet {
        destinations.forEach { destination ->
            androidx.compose.material3.NavigationDrawerItem(
                selected = isRouteSelected(currentRoute, destination.route),
                onClick = { onNavigate(destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}

/**
 * Check if a route matches the destination route.
 * ProjectDetails is considered under CareerTimeline for navigation highlighting.
 */
private fun isRouteSelected(currentRoute: Route, destinationRoute: Route): Boolean {
    return when {
        currentRoute == destinationRoute -> true
        // ProjectDetails is part of the Career flow
        currentRoute is Route.ProjectDetails && destinationRoute is Route.CareerTimeline -> true
        else -> false
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-ui:compileAndroidMain --quiet`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/adaptive/AdaptiveScaffold.kt
git commit -m "feat(adaptive): add AdaptiveScaffold with responsive navigation"
```

---

## Task 3: Integrate AdaptiveScaffold in MainActivity

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

**Step 1: Add imports**

```kotlin
import io.github.devmugi.cv.agent.ui.adaptive.AdaptiveScaffold
import io.github.devmugi.cv.agent.ui.adaptive.WindowWidthSizeClass
import io.github.devmugi.cv.agent.ui.adaptive.calculateWindowSizeClass
```

**Step 2: Calculate window size in CVAgentApp**

Add after `var currentRoute`:
```kotlin
val windowSizeClass = calculateWindowSizeClass()
```

**Step 3: Pass windowSizeClass to AppContent**

Update AppContent call and signature to include:
```kotlin
windowSizeClass = windowSizeClass,
```

**Step 4: Wrap content with AdaptiveScaffold**

In AppContent, wrap the existing Box with AdaptiveScaffold:
```kotlin
AdaptiveScaffold(
    windowSizeClass = windowSizeClass,
    currentRoute = currentRoute,
    onNavigate = onNavigate
) {
    Box {
        AnimatedContent(
            // ... existing AnimatedContent
        )
        // ... existing ArcaneToastHost and DebugThemePickerFab
    }
}
```

**Step 5: Hide bottom navigation for non-Chat screens on Compact**

The AnimatedContent already handles screen switching. The AdaptiveScaffold provides the navigation chrome. For Compact mode, we may want to hide the bottom bar when viewing ProjectDetails to maximize content space.

**Step 6: Verify compilation**

Run: `./gradlew :android-app:compileDevDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt
git commit -m "feat(adaptive): integrate AdaptiveScaffold in MainActivity"
```

---

## Task 4: Run Tests and Verification

**Step 1: Run screenshot tests**

Run: `./gradlew :shared-ui-screenshots:testDebugUnitTest --quiet`
Expected: All tests pass

**Step 2: Build Android app**

Run: `./gradlew :android-app:assembleDevDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Run quality checks**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

---

## Task 5: Push and Summary

**Step 1: Push branch**

Run: `git push -u origin feature/phase-3-adaptive-layouts`

**Step 2: Verify commit log**

Run: `git log --oneline -5`

---

## Success Criteria Checklist

- [ ] WindowSizeClass enum with Compact/Medium/Expanded
- [ ] Platform-specific calculateWindowSizeClass implementations
- [ ] AdaptiveScaffold with BottomBar/Rail/Drawer variants
- [ ] MainActivity uses AdaptiveScaffold
- [ ] Navigation highlights correctly across routes
- [ ] All tests pass
- [ ] Build succeeds

---

*Plan created: 2026-01-31*
*Estimated time: 30-45 minutes*
*Commits: ~3-4*
