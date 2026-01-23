package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.Team
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

private val AmberColor = Color(0xFFFFC107)

@Composable
fun TeamStructureSection(
    team: Team,
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

                // Table header
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

                    val textColor = if (structure.myRole == true) AmberColor else ArcaneTheme.colors.text

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

                // Methodology
                team.methodology?.let { methodology ->
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = ArcaneTheme.colors.surfaceContainerLow)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Methodology: $methodology",
                        style = ArcaneTheme.typography.bodyMedium,
                        color = ArcaneTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
