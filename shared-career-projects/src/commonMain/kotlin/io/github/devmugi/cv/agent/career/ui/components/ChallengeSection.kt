package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ArcaneTheme.colors.surfaceContainer)
        ) {
            // Left accent border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .matchParentSize()
                    .background(AmberColor)
            )

            Column(
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                // Context
                challenge.context?.let { context ->
                    Row {
                        Text(
                            text = "Context: ",
                            style = ArcaneTheme.typography.bodyMedium,
                            color = AmberColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = context,
                            style = ArcaneTheme.typography.bodyMedium,
                            color = ArcaneTheme.colors.text
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Response
                challenge.response?.let { response ->
                    Row {
                        Text(
                            text = "Response: ",
                            style = ArcaneTheme.typography.bodyMedium,
                            color = AmberColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = response,
                            style = ArcaneTheme.typography.bodyMedium,
                            color = ArcaneTheme.colors.text
                        )
                    }
                }

                // Details bullet list
                challenge.details?.takeIf { it.isNotEmpty() }?.let { details ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        details.forEach { detail ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = ArcaneTheme.colors.textSecondary,
                                    modifier = Modifier
                                        .size(6.dp)
                                        .padding(top = 6.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = detail,
                                    style = ArcaneTheme.typography.bodySmall,
                                    color = ArcaneTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
