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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

            composable(
                route = Routes.DETAILS,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")
                val project = projectId?.let { careerProjectsMap[it] }

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
