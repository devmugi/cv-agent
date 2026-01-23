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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.Standout

private val AmberColor = Color(0xFFFFC107)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StandoutSection(
    standout: Standout,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.Star,
            title = standout.title ?: "Why This Project Stands Out"
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            standout.items?.forEach { item ->
                StandoutCard(
                    icon = getStandoutIcon(item.icon),
                    title = item.title ?: "",
                    description = item.description ?: ""
                )
            }
        }
    }
}

@Composable
private fun StandoutCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    ArcaneSurface(
        variant = SurfaceVariant.Container,
        modifier = modifier.border(1.dp, ArcaneTheme.colors.surfaceContainerLow, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AmberColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = ArcaneTheme.typography.labelLarge,
                color = AmberColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = ArcaneTheme.typography.bodySmall,
                color = ArcaneTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getStandoutIcon(iconName: String?): ImageVector {
    return when {
        iconName?.contains("heart") == true -> Icons.Default.Favorite
        iconName?.contains("user") == true -> Icons.Default.Person
        iconName?.contains("code") == true -> Icons.Default.Code
        iconName?.contains("life-ring") == true -> Icons.Default.Star
        iconName?.contains("robot") == true -> Icons.Default.SmartToy
        else -> Icons.Default.Star
    }
}
