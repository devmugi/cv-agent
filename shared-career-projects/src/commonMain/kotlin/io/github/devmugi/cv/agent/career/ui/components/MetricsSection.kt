package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricsSection(
    metrics: List<MetricItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.ShowChart,
            title = "Metrics & Impact"
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metrics.forEach { metric ->
                MetricCard(
                    icon = getMetricIcon(metric.icon),
                    value = metric.value ?: "",
                    label = metric.label ?: "",
                    highlight = metric.highlight == true
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    value: String,
    label: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (highlight) AmberColor else ArcaneTheme.colors.surfaceContainerLow

    ArcaneSurface(
        variant = SurfaceVariant.Container,
        modifier = modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AmberColor,
                modifier = Modifier.size(28.dp)
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

private fun getMetricIcon(iconName: String?): ImageVector {
    return when {
        iconName?.contains("user-shield") == true -> Icons.Default.Shield
        iconName?.contains("calendar") == true -> Icons.Default.CalendarMonth
        iconName?.contains("percent") == true -> Icons.Default.Percent
        iconName?.contains("server") == true -> Icons.Default.Code
        else -> Icons.Default.ShowChart
    }
}
