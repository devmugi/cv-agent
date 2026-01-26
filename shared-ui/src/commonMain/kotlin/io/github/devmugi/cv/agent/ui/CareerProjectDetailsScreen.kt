package io.github.devmugi.cv.agent.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.ui.CareerProjectDetailsScreenScaffold

@Composable
fun CareerProjectDetailsScreen(
    project: CareerProject,
    onBackClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    analytics: Analytics = Analytics.NOOP,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        analytics.logEvent(
            AnalyticsEvent.Navigation.ScreenView(
                screenName = AnalyticsEvent.Navigation.Screen.PROJECT_DETAILS
            )
        )
    }

    CareerProjectDetailsScreenScaffold(
        project = project,
        onBackClick = {
            analytics.logEvent(
                AnalyticsEvent.Navigation.BackNavigation(
                    fromScreen = "project_details",
                    toScreen = "career_timeline",
                    method = AnalyticsEvent.Navigation.NavigationMethod.BUTTON
                )
            )
            onBackClick()
        },
        onLinkClick = { url ->
            analytics.logEvent(
                AnalyticsEvent.Link.ProjectLinkClicked(
                    projectId = project.id,
                    linkType = "external",
                    url = url
                )
            )
            onLinkClick(url)
        },
        modifier = modifier
    )
}
