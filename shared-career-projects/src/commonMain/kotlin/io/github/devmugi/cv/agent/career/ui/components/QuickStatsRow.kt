package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.MetricItem

private val AmberColor = Color(0xFFFFC107)

@Composable
fun QuickStatsRow(
    stats: List<MetricItem>,
    modifier: Modifier = Modifier
) {
    val items = stats.take(4)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row: items 0-1
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.take(2).forEach { stat ->
                QuickStatCard(
                    icon = getIconForStat(stat.icon),
                    value = stat.value ?: "",
                    label = stat.label ?: "",
                    highlight = stat.highlight == true,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
        // Second row: items 2-3
        if (items.size > 2) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.drop(2).forEach { stat ->
                    QuickStatCard(
                        icon = getIconForStat(stat.icon),
                        value = stat.value ?: "",
                        label = stat.label ?: "",
                        highlight = stat.highlight == true,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (highlight) AmberColor else ArcaneTheme.colors.surfaceContainerLow

    ArcaneSurface(
        variant = SurfaceVariant.Container,
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AmberColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = ArcaneTheme.typography.headlineLarge,
                color = ArcaneTheme.colors.text,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label.uppercase(),
                style = ArcaneTheme.typography.labelSmall,
                color = ArcaneTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getIconForStat(iconName: String?): ImageVector {
    return when {
        iconName?.contains("calendar") == true -> Icons.Default.CalendarMonth
        iconName?.contains("user-tie") == true -> Icons.Default.Person
        iconName?.contains("user-shield") == true -> Icons.Default.Shield
        iconName?.contains("code-branch") == true -> Icons.Default.Group
        else -> Icons.Default.CalendarMonth
    }
}
