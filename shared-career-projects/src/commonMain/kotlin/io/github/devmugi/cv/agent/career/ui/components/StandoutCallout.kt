package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.TimelineStandoutItem

private val AmberColor = Color(0xFFFFC107)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StandoutCallout(
    title: String,
    items: List<TimelineStandoutItem>,
    modifier: Modifier = Modifier
) {
    ArcaneSurface(
        variant = SurfaceVariant.Container,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = AmberColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = ArcaneTheme.typography.labelLarge,
                    color = AmberColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Items as inline text with bullet separators
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEachIndexed { index, item ->
                    if (index > 0) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = ArcaneTheme.colors.textSecondary,
                            modifier = Modifier.size(6.dp).align(Alignment.CenterVertically)
                        )
                    }
                    item.title?.let { title ->
                        Text(
                            text = title,
                            style = ArcaneTheme.typography.bodyMedium,
                            color = ArcaneTheme.colors.text
                        )
                    }
                }
            }
        }
    }
}
