# iOS Navigation Parity Design

## Overview

Add 3-screen navigation to iOS matching Android's behavior, using shared Compose Navigation in commonMain.

## Current State

- **Android**: Full navigation in `MainActivity.kt` (472 lines) with Chat → Timeline → Details
- **iOS**: Single ChatScreen in `MainViewController.kt` (144 lines), no navigation
- **Shared UI**: All three screens exist in `shared-ui/commonMain`

## Design Goals

1. Share navigation logic between Android and iOS
2. Minimize platform-specific code
3. Support iOS back swipe gesture + visible back button
4. Maintain existing screen functionality

## Architecture

### Shared Navigation Host (commonMain)

Create `AppNavHost` in `shared-ui` that both platforms use:

```
shared-ui/src/commonMain/.../navigation/
├── AppNavHost.kt          # NavHost with all routes
├── NavigationRoutes.kt    # Route constants
└── NavHostState.kt        # Shared navigation state holder
```

### Navigation Flow

```
Chat ──[suggestion chip]──> Timeline ──[project card]──> Details
  ↑                            │                           │
  └────────[back]──────────────┴─────────[back]────────────┘
```

### Routes

| Route | Args | Screen |
|-------|------|--------|
| `chat` | none | ChatScreen |
| `timeline?projectId={id}` | optional projectId | CareerProjectsTimelineScreen |
| `details/{projectId}` | required projectId | CareerProjectDetailsScreen |

## Implementation

### 1. Add Navigation Dependency

Add to `libs.versions.toml`:
```toml
[versions]
navigation = "2.9.0-alpha01"

[libraries]
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }
```

### 2. Create Shared Data Holder

Since screens need `CareerProject` objects (not just IDs), create a shared state holder:

```kotlin
// shared-ui/src/commonMain/.../navigation/NavHostState.kt
class NavHostState(
    val chatViewModel: ChatViewModel,
    val careerProjects: List<ProjectDataTimeline>,
    val careerProjectsMap: Map<String, CareerProject>,
    val analytics: Analytics,
    val toastState: ArcaneToastState,
    val onOpenUrl: (String) -> Unit
)
```

### 3. Create AppNavHost

```kotlin
// shared-ui/src/commonMain/.../navigation/AppNavHost.kt
@Composable
fun AppNavHost(
    state: NavHostState,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    // Handle back press via BackHandler (Android) or onBack callback (iOS)
    NavHost(navController, startDestination = "chat", modifier = modifier) {
        composable("chat") {
            ChatScreen(
                state = state.chatViewModel.state.collectAsState().value,
                toastState = state.toastState,
                onSendMessage = state.chatViewModel::sendMessage,
                analytics = state.analytics,
                onNavigateToProject = { projectId ->
                    navController.navigate("details/$projectId")
                },
                onNavigateToCareerTimeline = {
                    navController.navigate("timeline")
                },
                // ... other callbacks
            )
        }

        composable("timeline?projectId={projectId}") { backStackEntry ->
            CareerProjectsTimelineScreen(
                projects = state.careerProjects,
                onProjectClick = { project ->
                    navController.navigate("details/${project.id}")
                },
                onBackClick = { navController.popBackStack() },
                analytics = state.analytics
            )
        }

        composable("details/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")
            val project = state.careerProjectsMap[projectId]
            if (project != null) {
                CareerProjectDetailsScreen(
                    project = project,
                    onBackClick = { navController.popBackStack() },
                    onLinkClick = state.onOpenUrl,
                    analytics = state.analytics
                )
            }
        }
    }
}
```

### 4. Simplify MainActivity (Android)

Replace navigation state machine with `AppNavHost`:

```kotlin
// android-app/.../MainActivity.kt
setContent {
    ArcaneTheme(colors = currentTheme.toColors()) {
        val state = NavHostState(
            chatViewModel = koinViewModel { parametersOf(dataProvider) },
            careerProjects = timelineProjects,
            careerProjectsMap = projectsMap,
            analytics = koinInject(),
            toastState = rememberArcaneToastState(),
            onOpenUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        )
        AppNavHost(state = state)
    }
}
```

### 5. Update MainViewController (iOS)

```kotlin
// shared/src/iosMain/.../MainViewController.kt
fun MainViewController() = ComposeUIViewController {
    val state = NavHostState(
        chatViewModel = koinInject { parametersOf(dataProvider) },
        careerProjects = timelineProjects,
        careerProjectsMap = projectsMap,
        analytics = koinInject(),
        toastState = rememberArcaneToastState(),
        onOpenUrl = { url -> UIApplication.sharedApplication.openURL(NSURL(string = url)!!) }
    )

    ArcaneTheme(colors = DEFAULT_THEME.toColors()) {
        AppNavHost(state = state)
    }
}
```

### 6. iOS Back Swipe Gesture

Add edge swipe detection in the Compose layer using `Modifier.pointerInput`:

```kotlin
// shared-ui/src/commonMain/.../navigation/BackGestureHandler.kt
@Composable
expect fun BackHandler(enabled: Boolean, onBack: () -> Unit)

// iosMain implementation uses gesture detection
// androidMain implementation uses androidx.activity.compose.BackHandler
```

## Files to Modify

| File | Change |
|------|--------|
| `libs.versions.toml` | Add navigation-compose dependency |
| `shared-ui/build.gradle.kts` | Add navigation dependency |
| `shared-ui/.../navigation/AppNavHost.kt` | Create (new) |
| `shared-ui/.../navigation/NavHostState.kt` | Create (new) |
| `shared-ui/.../navigation/BackHandler.kt` | Create (new, expect/actual) |
| `MainActivity.kt` | Simplify to use AppNavHost |
| `MainViewController.kt` | Add navigation via AppNavHost |

## Testing

1. Android: Verify navigation works identically to before
2. iOS: Verify navigation works with back button and swipe gesture
3. Both: Verify suggestion chip → project details flow

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Navigation library compatibility | Use stable JetBrains navigation version |
| iOS swipe gesture feel | Tune gesture thresholds, fall back to button-only if needed |
| Data loading race conditions | Keep existing LaunchedEffect patterns for data loading |

## Success Criteria

- [ ] iOS shows all 3 screens (Chat, Timeline, Details)
- [ ] Navigation via suggestion chips works on iOS
- [ ] Back button visible on Timeline and Details screens
- [ ] iOS swipe-to-go-back gesture works
- [ ] Android behavior unchanged
- [ ] Shared navigation code in commonMain
