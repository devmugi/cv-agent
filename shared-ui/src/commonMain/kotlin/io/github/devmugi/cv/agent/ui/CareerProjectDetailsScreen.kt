package io.github.devmugi.cv.agent.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.ui.CareerProjectDetailsScreenScaffold

@Composable
fun CareerProjectDetailsScreen(
    project: CareerProject,
    onBackClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CareerProjectDetailsScreenScaffold(
        project = project,
        onBackClick = onBackClick,
        onLinkClick = onLinkClick,
        modifier = modifier
    )
}
