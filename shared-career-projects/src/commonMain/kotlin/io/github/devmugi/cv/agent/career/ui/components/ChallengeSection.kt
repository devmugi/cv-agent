package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import io.github.devmugi.cv.agent.career.models.Challenge

private val AmberColor = Color(0xFFFFC107)

@Composable
fun ChallengeSection(
    challenge: Challenge,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.Warning,
            title = "The Challenge"
        )

        Spacer(modifier = Modifier.height(12.dp))

        ArcaneSurface(
            variant = SurfaceVariant.Container,
            modifier = Modifier
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
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Challenge Overview",
                        style = ArcaneTheme.typography.labelLarge,
                        color = ArcaneTheme.colors.text,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Context
                challenge.context?.let { context ->
                    Text(
                        text = "Context:",
                        style = ArcaneTheme.typography.bodyMedium,
                        color = AmberColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = context,
                        style = ArcaneTheme.typography.bodyMedium,
                        color = ArcaneTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Response
                challenge.response?.let { response ->
                    Text(
                        text = "Response:",
                        style = ArcaneTheme.typography.bodyMedium,
                        color = AmberColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = response,
                        style = ArcaneTheme.typography.bodyMedium,
                        color = ArcaneTheme.colors.textSecondary
                    )
                }

                // Details bullet list
                challenge.details?.takeIf { it.isNotEmpty() }?.let { details ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        details.forEach { detail ->
                            BulletListItem(text = detail)
                        }
                    }
                }
            }
        }
    }
}
