# Career Projects Module Design

## Overview

Add a new `shared-career-projects` KMP module with models and Compose UI for displaying career project timeline.

## Module Structure

```
shared-career-projects/
├── build.gradle.kts
└── src/commonMain/kotlin/io/github/devmugi/cv/agent/career/
    ├── models/
    │   └── CareerProject.kt
    └── ui/
        ├── CareerProjectsTimelineScreenScaffold.kt
        ├── CareerProjectTimelineInfo.kt
        └── CareerProjectDetailsScreenScaffold.kt
```

**Dependency flow:** `shared-career-projects` ← `shared-ui` ← `shared` ← `android-app`

## Model

```kotlin
data class CareerProject(
    val id: String,
    val name: String,
    val description: String,
    val companyName: String
)
```

## UI Components

### CareerProjectTimelineInfo
Card showing project summary with name, description (truncated), company name, and "Details" button.

```kotlin
@Composable
fun CareerProjectTimelineInfo(
    project: CareerProject,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

### CareerProjectsTimelineScreenScaffold
Timeline list screen with top bar (back + title) and LazyColumn of project cards.

```kotlin
@Composable
fun CareerProjectsTimelineScreenScaffold(
    projects: List<CareerProject>,
    onProjectDetailsClick: (CareerProject) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

### CareerProjectDetailsScreenScaffold
Full project view with all fields.

```kotlin
@Composable
fun CareerProjectDetailsScreenScaffold(
    project: CareerProject,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

## Integration

### ChatScreen Changes
- Add FAB with hamburger menu icon below TopBar
- FAB opens dropdown menu with "Career Timeline" option
- New callback: `onNavigateToCareerTimeline: () -> Unit`

### New Screens in shared-ui
- `CareerProjectsTimelineScreen` - wraps scaffold
- `CareerProjectDetailsScreen` - wraps scaffold

### MainActivity Changes
- Navigation state: `enum class Screen { Chat, CareerTimeline, ProjectDetails }`
- Track `currentScreen` and `selectedProject`
- `when (currentScreen)` to render correct screen
- Simple callback-based navigation (no Navigation Compose)

## Mock Data

```kotlin
val mockProjects = listOf(
    CareerProject(
        id = "1",
        name = "CV Agent App",
        description = "AI-powered mobile app for exploring CV data with chat interface",
        companyName = "Personal Project"
    ),
    CareerProject(
        id = "2",
        name = "E-Commerce Platform",
        description = "Full-stack marketplace with payment integration and real-time inventory",
        companyName = "TechCorp Inc"
    )
)
```

## Verification

1. Build and run app
2. See ChatScreen with FAB (hamburger icon) below toolbar
3. Tap FAB → menu shows "Career Timeline"
4. Tap "Career Timeline" → see list with 2 project cards
5. Tap "Details" on a card → see full project details
6. Tap back → return to timeline
7. Tap back → return to chat
