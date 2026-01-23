package io.github.devmugi.cv.agent.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.ui.CareerProjectsTimelineScreenScaffold

@Composable
fun CareerProjectsTimelineScreen(
    projects: List<CareerProject>,
    onProjectClick: (CareerProject) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CareerProjectsTimelineScreenScaffold(
        projects = projects,
        onProjectDetailsClick = onProjectClick,
        onBackClick = onBackClick,
        modifier = modifier
    )
}
