package io.github.devmugi.cv.agent.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
import io.github.devmugi.cv.agent.career.models.ProjectDataTimeline
import io.github.devmugi.cv.agent.career.ui.CareerProjectsTimelineScreenScaffold

@Composable
fun CareerProjectsTimelineScreen(
    projects: List<ProjectDataTimeline>,
    onProjectClick: (ProjectDataTimeline) -> Unit,
    onBackClick: () -> Unit,
    analytics: Analytics = Analytics.NOOP,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        analytics.logEvent(
            AnalyticsEvent.Navigation.ScreenView(
                screenName = AnalyticsEvent.Navigation.Screen.CAREER_TIMELINE
            )
        )
    }

    CareerProjectsTimelineScreenScaffold(
        projects = projects,
        onProjectDetailsClick = { project ->
            analytics.logEvent(
                AnalyticsEvent.Navigation.ProjectSelected(
                    projectId = project.id,
                    source = AnalyticsEvent.Navigation.SelectionSource.TIMELINE
                )
            )
            onProjectClick(project)
        },
        onBackClick = {
            analytics.logEvent(
                AnalyticsEvent.Navigation.BackNavigation(
                    fromScreen = "career_timeline",
                    toScreen = "chat",
                    method = AnalyticsEvent.Navigation.NavigationMethod.BUTTON
                )
            )
            onBackClick()
        },
        modifier = modifier
    )
}
