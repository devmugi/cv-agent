package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.Achievement

private val AmberColor = Color(0xFFFFC107)
private val GreenColor = Color(0xFF4CAF50)

@Composable
fun AchievementCard(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    ArcaneSurface(
        variant = SurfaceVariant.Container,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AmberColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = getAchievementIcon(achievement.icon),
                    contentDescription = null,
                    tint = AmberColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = achievement.title ?: "",
                    style = ArcaneTheme.typography.labelLarge,
                    color = ArcaneTheme.colors.text,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Problem
            achievement.problem?.let { problem ->
                Text(
                    text = "Problem: ",
                    style = ArcaneTheme.typography.bodyMedium,
                    color = AmberColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = problem,
                    style = ArcaneTheme.typography.bodyMedium,
                    color = ArcaneTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Solution
            achievement.solution?.let { solution ->
                Text(
                    text = "Solution: ",
                    style = ArcaneTheme.typography.bodyMedium,
                    color = AmberColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = solution,
                    style = ArcaneTheme.typography.bodyMedium,
                    color = ArcaneTheme.colors.textSecondary
                )
            }

            // Details bullet list
            achievement.details?.takeIf { it.isNotEmpty() }?.let { details ->
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    details.forEach { detail ->
                        BulletListItem(text = detail)
                    }
                }
            }

            // Impact badge
            achievement.impact?.text?.let { impactText ->
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GreenColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = GreenColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = impactText,
                            style = ArcaneTheme.typography.labelMedium,
                            color = GreenColor
                        )
                    }
                }
            }
        }
    }
}

private fun getAchievementIcon(iconName: String?): ImageVector {
    return when {
        iconName?.contains("life-ring") == true -> Icons.Default.Star
        iconName?.contains("network") == true -> Icons.Default.Settings
        iconName?.contains("layer") == true -> Icons.Default.Architecture
        iconName?.contains("lock") == true -> Icons.Default.Build
        iconName?.contains("server") == true -> Icons.Default.Code
        iconName?.contains("robot") == true -> Icons.Default.Star
        iconName?.contains("chart") == true -> Icons.Default.Star
        iconName?.contains("palette") == true -> Icons.Default.Star
        iconName?.contains("project") == true -> Icons.Default.Architecture
        iconName?.contains("video") == true -> Icons.Default.Star
        else -> Icons.Default.Star
    }
}
