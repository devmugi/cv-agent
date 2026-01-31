# Phase 2: Type-Safe Navigation (Navigation 3) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace manual enum-based navigation with Navigation 3's type-safe `@Serializable` routes.

**Architecture:** Create type-safe route definitions in shared-ui, wire NavHost in MainActivity, preserve existing animations and back handling.

**Tech Stack:** Kotlin, Navigation 3 (1.0.0-alpha06), Kotlinx Serialization

---

## Current State Analysis

The app uses enum-based navigation in `MainActivity.kt`:
```kotlin
private enum class Screen { Chat, CareerTimeline, ProjectDetails }
var currentScreen by remember { mutableStateOf(Screen.Chat) }
```

Navigation involves:
- 3 screens: Chat, CareerTimeline, ProjectDetails
- ProjectDetails requires a `projectId` parameter
- Custom AnimatedContent transitions
- BackHandler for gesture navigation
- Analytics tracking for navigation events

---

## Files Overview

**Modify:**
- `gradle/libs.versions.toml` - Add Navigation 3 dependencies

**Create:**
- `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/Routes.kt`

**Modify:**
- `shared-ui/build.gradle.kts` - Add Navigation 3 and serialization dependencies
- `android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt` - Use type-safe routes

---

## Task 1: Add Navigation 3 Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared-ui/build.gradle.kts`

**Step 1: Add versions to libs.versions.toml**

Add under `[versions]`:
```toml
navigation3 = "1.0.0-alpha06"
```

Add under `[libraries]`:
```toml
navigation3-runtime = { module = "org.jetbrains.androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
```

**Step 2: Add dependencies to shared-ui/build.gradle.kts**

Add to `commonMain.dependencies`:
```kotlin
implementation(libs.navigation3.runtime)
implementation(libs.navigation3.ui)
```

Ensure serialization plugin is applied (check if already present):
```kotlin
plugins {
    // ... existing plugins
    alias(libs.plugins.kotlin.serialization)  // Add if missing
}
```

If adding serialization plugin, also add dependency:
```kotlin
implementation(libs.kotlinx.serialization.json)
```

**Step 3: Verify compilation**

Run: `./gradlew :shared-ui:compileAndroidMain --quiet`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml shared-ui/build.gradle.kts
git commit -m "build: add Navigation 3 dependencies"
```

---

## Task 2: Create Type-Safe Routes

**Files:**
- Create: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/Routes.kt`

**Step 1: Create navigation directory**

```bash
mkdir -p shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation
```

**Step 2: Create Routes.kt**

```kotlin
package io.github.devmugi.cv.agent.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the CV Agent app.
 * Uses @Serializable for Navigation 3 compatibility.
 */
@Serializable
sealed interface Route {
    /**
     * Main chat screen - the app's home/default destination.
     */
    @Serializable
    data object Chat : Route

    /**
     * Career timeline showing all projects chronologically.
     */
    @Serializable
    data object CareerTimeline : Route

    /**
     * Project details screen showing full information for a specific project.
     * @param projectId The unique identifier for the project to display.
     */
    @Serializable
    data class ProjectDetails(val projectId: String) : Route
}
```

**Step 3: Verify compilation**

Run: `./gradlew :shared-ui:compileAndroidMain --quiet`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/navigation/Routes.kt
git commit -m "feat(navigation): add type-safe Route definitions"
```

---

## Task 3: Update MainActivity to Use Type-Safe Routes

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

**Step 1: Import Routes**

Add import:
```kotlin
import io.github.devmugi.cv.agent.ui.navigation.Route
```

**Step 2: Replace enum with Route**

Replace:
```kotlin
private enum class Screen { Chat, CareerTimeline, ProjectDetails }
```

With: (remove entirely - we'll use Route instead)

**Step 3: Update state variable**

Replace:
```kotlin
var currentScreen by remember { mutableStateOf(Screen.Chat) }
var selectedProject by remember { mutableStateOf<CareerProject?>(null) }
```

With:
```kotlin
var currentRoute by remember { mutableStateOf<Route>(Route.Chat) }
```

**Step 4: Update AppContent signature**

Change parameter:
```kotlin
currentScreen: Screen,
onScreenChange: (Screen) -> Unit,
onProjectSelect: (CareerProject?) -> Unit,
```

To:
```kotlin
currentRoute: Route,
onNavigate: (Route) -> Unit,
```

**Step 5: Update BackHandler**

Replace:
```kotlin
BackHandler(enabled = currentScreen != Screen.Chat) {
    val (fromScreen, toScreen, nextScreen) = when (currentScreen) {
        Screen.ProjectDetails -> Triple("project_details", "career_timeline", Screen.CareerTimeline)
        Screen.CareerTimeline -> Triple("career_timeline", "chat", Screen.Chat)
        Screen.Chat -> return@BackHandler
    }
    // ... analytics
    onScreenChange(nextScreen)
}
```

With:
```kotlin
BackHandler(enabled = currentRoute !is Route.Chat) {
    val (fromScreen, toScreen, nextRoute) = when (currentRoute) {
        is Route.ProjectDetails -> Triple("project_details", "career_timeline", Route.CareerTimeline)
        is Route.CareerTimeline -> Triple("career_timeline", "chat", Route.Chat)
        is Route.Chat -> return@BackHandler
    }
    analytics.logEvent(
        AnalyticsEvent.Navigation.BackNavigation(
            fromScreen = fromScreen,
            toScreen = toScreen,
            method = AnalyticsEvent.Navigation.NavigationMethod.GESTURE
        )
    )
    onNavigate(nextRoute)
}
```

**Step 6: Update AnimatedContent**

Replace:
```kotlin
AnimatedContent(
    targetState = currentScreen,
    // ...
) { screen ->
    when (screen) {
        Screen.Chat -> ChatScreen(...)
        Screen.CareerTimeline -> CareerProjectsTimelineScreen(...)
        Screen.ProjectDetails -> selectedProject?.let { ... }
    }
}
```

With:
```kotlin
AnimatedContent(
    targetState = currentRoute,
    // ... (keep existing transitionSpec)
) { route ->
    when (route) {
        is Route.Chat -> ChatScreen(
            // ... existing params ...
            onNavigateToCareerTimeline = { onNavigate(Route.CareerTimeline) },
            onNavigateToProject = { projectId ->
                careerProjectsMap[projectId]?.let { project ->
                    viewModel.onProjectSuggestionClicked(projectId, 0)
                    onNavigate(Route.ProjectDetails(projectId))
                }
            },
            // ... rest of params
        )
        is Route.CareerTimeline -> CareerProjectsTimelineScreen(
            projects = careerProjects,
            onProjectClick = { timelineProject ->
                onNavigate(Route.ProjectDetails(timelineProject.id))
            },
            onBackClick = { onNavigate(Route.Chat) },
            analytics = analytics
        )
        is Route.ProjectDetails -> {
            // Get project from map using the type-safe projectId
            careerProjectsMap[route.projectId]?.let { project ->
                CareerProjectDetailsScreen(
                    project = project,
                    onBackClick = { onNavigate(Route.CareerTimeline) },
                    onLinkClick = onOpenUrl,
                    analytics = analytics
                )
            }
        }
    }
}
```

**Step 7: Update CVAgentApp call to AppContent**

Replace:
```kotlin
AppContent(
    currentScreen = currentScreen,
    // ...
    onScreenChange = { currentScreen = it },
    onProjectSelect = { selectedProject = it },
    // ...
)
```

With:
```kotlin
AppContent(
    currentRoute = currentRoute,
    // ...
    onNavigate = { currentRoute = it },
    // ... (remove onProjectSelect - projectId now in route)
)
```

**Step 8: Remove unused selectedProject**

The `selectedProject` state is no longer needed since `Route.ProjectDetails` carries the `projectId`.

**Step 9: Verify compilation**

Run: `./gradlew :android-app:compileDebugKotlin --quiet`
Expected: BUILD SUCCESSFUL

**Step 10: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt
git commit -m "refactor: migrate MainActivity to type-safe Routes"
```

---

## Task 4: Run Tests and Verification

**Step 1: Run Android app build**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Run all tests**

Run: `./gradlew :shared-ui-screenshots:testDebugUnitTest --quiet`
Expected: All tests pass

**Step 3: Run quality checks**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

**Step 4: Verify routes are type-safe**

The compiler should now catch invalid route usage. These should fail to compile:
- `Route.ProjectDetails()` (missing projectId)
- `Route.ProjectDetails(123)` (wrong type)

Only this should compile:
- `Route.ProjectDetails("project-id")`

---

## Task 5: Final Commit and Summary

**Step 1: Verify clean state**

Run: `git status`
Expected: All changes committed

**Step 2: View commit log**

Run: `git log --oneline -5`

Should show:
```
refactor: migrate MainActivity to type-safe Routes
feat(navigation): add type-safe Route definitions
build: add Navigation 3 dependencies
```

**Step 3: Push branch**

Run: `git push -u origin feature/phase-2-navigation-3`

---

## Success Criteria Checklist

- [ ] Navigation 3 dependencies added to version catalog
- [ ] Routes.kt created with @Serializable routes
- [ ] MainActivity uses Route instead of enum Screen
- [ ] ProjectDetails route carries projectId type-safely
- [ ] All existing navigation animations preserved
- [ ] BackHandler works correctly
- [ ] Analytics navigation events still fire
- [ ] All tests pass
- [ ] ktlint passes

---

## Rollback Plan

If issues arise:

```bash
git revert --no-commit HEAD~3..HEAD
git commit -m "revert: Phase 2 navigation changes"
```

---

*Plan created: 2026-01-31*
*Estimated time: 30-45 minutes*
*Commits: ~3-4*
