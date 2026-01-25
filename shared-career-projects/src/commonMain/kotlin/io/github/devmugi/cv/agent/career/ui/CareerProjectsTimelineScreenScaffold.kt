package io.github.devmugi.cv.agent.career.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.ProjectDataTimeline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerProjectsTimelineScreenScaffold(
    projects: List<ProjectDataTimeline>,
    onProjectDetailsClick: (ProjectDataTimeline) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = ArcaneTheme.colors.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Career Timeline",
                        style = ArcaneTheme.typography.headlineLarge,
                        color = ArcaneTheme.colors.text
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ArcaneTheme.colors.text
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArcaneTheme.colors.surfaceContainerLow
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ArcaneSurface(
                    variant = SurfaceVariant.Container,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            ArcaneTheme.colors.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "From System Administrator to Lead Android Developer & AI Agent Engineer",
                            style = ArcaneTheme.typography.bodyLarge,
                            color = ArcaneTheme.colors.text.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "2006 - Present (19 years)",
                            style = ArcaneTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = ArcaneTheme.colors.textSecondary
                        )
                    }
                }
            }
            items(projects, key = { it.id }) { project ->
                CareerProjectTimelineInfo(
                    project = project,
                    onDetailsClick = { onProjectDetailsClick(project) }
                )
            }
        }
    }
}
