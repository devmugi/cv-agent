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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.Lifecycle

private val AmberColor = Color(0xFFFFC107)
private val GrayColor = Color(0xFF9E9E9E)
private val BlueColor = Color(0xFF2196F3)

@Composable
fun LifecycleTimeline(
    lifecycle: Lifecycle,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.History,
            title = "Product Lifecycle"
        )

        Spacer(modifier = Modifier.height(12.dp))

        ArcaneSurface(
            variant = SurfaceVariant.Container,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                lifecycle.events?.forEachIndexed { index, event ->
                    val isLast = index == (lifecycle.events?.lastIndex ?: 0)
                    val dotColor = when (event.status) {
                        "discontinued" -> GrayColor
                        else -> AmberColor
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(40.dp)
                                        .background(ArcaneTheme.colors.surfaceContainerLow)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.displayDate ?: event.date ?: "",
                                style = ArcaneTheme.typography.labelLarge,
                                color = ArcaneTheme.colors.text,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = event.event ?: "",
                                style = ArcaneTheme.typography.bodySmall,
                                color = ArcaneTheme.colors.textSecondary
                            )
                            if (!isLast) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }

                // Discontinuation note
                lifecycle.discontinuationReason?.let { reason ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BlueColor.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BlueColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Note: ",
                                    style = ArcaneTheme.typography.labelMedium,
                                    color = BlueColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = reason,
                                    style = ArcaneTheme.typography.bodySmall,
                                    color = ArcaneTheme.colors.text
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
