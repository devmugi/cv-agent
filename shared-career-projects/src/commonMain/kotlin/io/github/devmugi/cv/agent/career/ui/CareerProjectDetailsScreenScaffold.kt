package io.github.devmugi.cv.agent.career.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.theme.CareerColors
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.ui.components.AchievementCard
import io.github.devmugi.cv.agent.career.ui.components.ChallengeSection
import io.github.devmugi.cv.agent.career.ui.components.CoursesSection
import io.github.devmugi.cv.agent.career.ui.components.DescriptionSection
import io.github.devmugi.cv.agent.career.ui.components.LifecycleTimeline
import io.github.devmugi.cv.agent.career.ui.components.LinksSection
import io.github.devmugi.cv.agent.career.ui.components.MetricsSection
import io.github.devmugi.cv.agent.career.ui.components.OverviewSection
import io.github.devmugi.cv.agent.career.ui.components.ProjectGradientHeader
import io.github.devmugi.cv.agent.career.ui.components.QuickStatsRow
import io.github.devmugi.cv.agent.career.ui.components.SectionHeader
import io.github.devmugi.cv.agent.career.ui.components.StandoutSection
import io.github.devmugi.cv.agent.career.ui.components.TeamStructureSection
import io.github.devmugi.cv.agent.career.ui.components.TechnologiesSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerProjectDetailsScreenScaffold(
    project: CareerProject,
    onBackClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    val showToolbarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                lazyListState.firstVisibleItemScrollOffset > 200
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = ArcaneTheme.colors.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showToolbarTitle,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(150))
                    ) {
                        Column {
                            Text(
                                text = project.name,
                                style = ArcaneTheme.typography.titleMedium,
                                color = ArcaneTheme.colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            project.overview?.role?.let { role ->
                                Text(
                                    text = role,
                                    style = ArcaneTheme.typography.bodySmall,
                                    color = CareerColors.Amber,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
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
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Header
            item {
                ProjectGradientHeader(
                    name = project.name,
                    role = project.overview?.role,
                    period = project.overview?.period?.displayText,
                    gradientColors = project.hero?.gradientColors,
                    featured = project.meta?.featured == true,
                    onClick = {} // Already on details screen
                )
            }

            // Quick Stats Row
            item {
                project.metrics?.quickStats?.let { stats ->
                    QuickStatsRow(
                        stats = stats,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Overview Section
            item {
                project.overview?.let { overview ->
                    OverviewSection(
                        overview = overview,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Description Section
            item {
                project.description?.let { description ->
                    DescriptionSection(
                        description = description,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Courses Section (only shown if courses data exists, e.g., Android School)
            item {
                project.courses?.takeIf { it.isNotEmpty() }?.let { courses ->
                    CoursesSection(
                        courses = courses,
                        onLinkClick = onLinkClick,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Challenge Section
            item {
                project.challenge?.let { challenge ->
                    ChallengeSection(
                        challenge = challenge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Achievements Section
            item {
                project.achievements?.takeIf { it.isNotEmpty() }?.let { achievements ->
                    SectionHeader(
                        icon = Icons.Default.EmojiEvents,
                        title = "Key Achievements",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            project.achievements?.forEach { achievement ->
                item {
                    AchievementCard(
                        achievement = achievement,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Standout Section
            item {
                project.standout?.let { standout ->
                    StandoutSection(
                        standout = standout,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Team Structure Section
            item {
                project.team?.let { team ->
                    TeamStructureSection(
                        team = team,
                        onLinkClick = onLinkClick,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Metrics Section
            item {
                project.metrics?.launch?.let { metrics ->
                    MetricsSection(
                        metrics = metrics,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Technologies Section
            item {
                project.technologies?.let { technologies ->
                    TechnologiesSection(
                        technologies = technologies,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Lifecycle Timeline
            item {
                project.lifecycle?.let { lifecycle ->
                    LifecycleTimeline(
                        lifecycle = lifecycle,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Links Section
            item {
                project.links?.takeIf { it.isNotEmpty() }?.let { links ->
                    LinksSection(
                        links = links,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
