package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.Team
import io.github.devmugi.cv.agent.career.models.Trainer
import io.github.devmugi.cv.agent.career.theme.CareerColors
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

private val LinkedInBlue = Color(0xFF0A66C2)

@Composable
fun TeamStructureSection(
    team: Team,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.Groups,
            title = "Team Structure"
        )

        Spacer(modifier = Modifier.height(12.dp))

        ArcaneSurface(
            variant = SurfaceVariant.Container,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val hasStructure = !team.structure.isNullOrEmpty()
                val hasMethodology = team.methodology != null
                val hasTotalSize = team.totalSize != null

                // Total size
                team.totalSize?.let { size ->
                    Text(
                        text = "Total Size: ~$size people",
                        style = ArcaneTheme.typography.labelLarge,
                        color = ArcaneTheme.colors.text,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Table header and rows - only show if structure data exists
                if (hasStructure) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TEAM",
                            style = ArcaneTheme.typography.labelSmall,
                            color = ArcaneTheme.colors.textSecondary,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "SIZE",
                            style = ArcaneTheme.typography.labelSmall,
                            color = ArcaneTheme.colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "NOTES",
                            style = ArcaneTheme.typography.labelSmall,
                            color = ArcaneTheme.colors.textSecondary,
                            modifier = Modifier.weight(2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ArcaneTheme.colors.surfaceContainerLow)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Team rows
                    team.structure?.forEach { structure ->
                        val sizeValue = when (val size = structure.size) {
                            is JsonPrimitive -> size.intOrNull?.toString() ?: size.content
                            else -> size?.toString() ?: ""
                        }

                        val textColor = if (structure.myRole == true) CareerColors.Amber else ArcaneTheme.colors.text

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = structure.team ?: "",
                                style = ArcaneTheme.typography.bodyMedium,
                                color = textColor,
                                fontWeight = if (structure.myRole == true) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = sizeValue,
                                style = ArcaneTheme.typography.bodyMedium,
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = structure.notes ?: "",
                                style = ArcaneTheme.typography.bodySmall,
                                color = if (structure.myRole == true) textColor else ArcaneTheme.colors.textSecondary,
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }

                // Methodology
                team.methodology?.let { methodology ->
                    if (hasStructure || hasTotalSize) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = ArcaneTheme.colors.surfaceContainerLow)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        text = "Methodology: $methodology",
                        style = ArcaneTheme.typography.bodyMedium,
                        color = ArcaneTheme.colors.textSecondary
                    )
                }

                // Trainers/Lectors
                team.trainers?.takeIf { it.isNotEmpty() }?.let { trainers ->
                    val hasContentAbove = hasStructure || hasMethodology || hasTotalSize
                    if (hasContentAbove) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = ArcaneTheme.colors.surfaceContainerLow)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = "Trainers & Lectors",
                        style = ArcaneTheme.typography.labelLarge,
                        color = ArcaneTheme.colors.text,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    trainers.forEach { trainer ->
                        TrainerRow(
                            trainer = trainer,
                            isOwner = trainer.name == "Denys Honcharenko",
                            onLinkClick = onLinkClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainerRow(
    trainer: Trainer,
    isOwner: Boolean,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trainer.name ?: "",
                style = ArcaneTheme.typography.bodyMedium,
                color = if (isOwner) CareerColors.Amber else ArcaneTheme.colors.text,
                fontWeight = if (isOwner) FontWeight.SemiBold else FontWeight.Normal
            )
            trainer.note?.let { note ->
                Text(
                    text = note,
                    style = ArcaneTheme.typography.labelSmall,
                    color = ArcaneTheme.colors.textSecondary
                )
            }
        }

        // Social links
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            trainer.github?.let { url ->
                Text(
                    text = "GitHub",
                    style = ArcaneTheme.typography.labelSmall,
                    color = ArcaneTheme.colors.textSecondary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onLinkClick(url) }
                )
            }
            trainer.linkedin?.let { url ->
                Text(
                    text = "LinkedIn",
                    style = ArcaneTheme.typography.labelSmall,
                    color = LinkedInBlue,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onLinkClick(url) }
                )
            }
        }
    }
}
