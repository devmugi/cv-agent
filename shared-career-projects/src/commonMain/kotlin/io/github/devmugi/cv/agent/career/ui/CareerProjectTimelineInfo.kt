package io.github.devmugi.cv.agent.career.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.components.controls.ArcaneButtonStyle
import io.github.devmugi.arcane.design.components.controls.ArcaneTextButton
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.ProjectDataTimeline
import io.github.devmugi.cv.agent.career.ui.components.AchievementsList
import io.github.devmugi.cv.agent.career.ui.components.ImpactBadgeChip
import io.github.devmugi.cv.agent.career.ui.components.ProjectGradientHeader
import io.github.devmugi.cv.agent.career.ui.components.StandoutCallout
import io.github.devmugi.cv.agent.career.ui.components.TechnologyTag

private val AmberColor = Color(0xFFFFC107)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareerProjectTimelineInfo(
    project: ProjectDataTimeline,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ArcaneSurface(
        variant = SurfaceVariant.Container,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Gradient Header (always visible)
            ProjectGradientHeader(
                name = project.name,
                role = project.role?.title,
                period = project.period?.displayText,
                gradientColors = project.hero?.gradientColors,
                featured = project.featured == true
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Company Badge (always visible)
                project.companies?.firstOrNull()?.name?.let { companyName ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ArcaneTheme.colors.surfaceContainerLow)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = companyName,
                            style = ArcaneTheme.typography.labelMedium,
                            color = ArcaneTheme.colors.text
                        )
                    }
                }

                // Description (always visible)
                project.description?.short?.let { desc ->
                    Text(
                        text = desc,
                        style = ArcaneTheme.typography.bodyMedium,
                        color = ArcaneTheme.colors.textSecondary
                    )
                }

                // Impact Badges (always visible)
                project.impactBadges?.takeIf { it.isNotEmpty() }?.let { badges ->
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        badges.forEach { badge ->
                            badge.text?.let { text ->
                                ImpactBadgeChip(
                                    text = text,
                                    highlight = badge.highlight == true
                                )
                            }
                        }
                    }
                }

                // Technology Tags (always visible)
                project.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.take(6).forEach { tag ->
                            tag.text?.let { text ->
                                TechnologyTag(text = text)
                            }
                        }
                    }
                }

                // Expandable Section
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Key Achievements
                        project.achievements?.preview?.takeIf { it.isNotEmpty() }?.let { achievements ->
                            AchievementsList(achievements = achievements)
                        }

                        // Standout Callout
                        project.standout?.let { standout ->
                            standout.items?.takeIf { it.isNotEmpty() }?.let { items ->
                                StandoutCallout(
                                    title = standout.title ?: "Why This Project Stands Out",
                                    items = items
                                )
                            }
                        }

                        // View Full Details Button
                        ArcaneTextButton(
                            text = "View Full Project Details",
                            onClick = onDetailsClick,
                            style = ArcaneButtonStyle.Outlined(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Expand/Collapse Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Click to collapse" else "Click to expand",
                        style = ArcaneTheme.typography.labelMedium,
                        color = ArcaneTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ArcaneTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
