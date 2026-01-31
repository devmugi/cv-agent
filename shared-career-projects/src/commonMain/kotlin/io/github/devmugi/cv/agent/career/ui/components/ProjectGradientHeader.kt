package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.theme.CareerColors
import io.github.devmugi.cv.agent.career.ui.util.parseHexColor

@Composable
fun ProjectGradientHeader(
    name: String,
    role: String?,
    period: String?,
    gradientColors: List<String>?,
    featured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = gradientColors?.takeIf { it.size >= 2 }?.map { parseHexColor(it) }
        ?: listOf(ArcaneTheme.colors.primary, ArcaneTheme.colors.primary.copy(alpha = 0.7f))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Brush.horizontalGradient(colors))
    ) {
        // Background icon (decorative)
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.1f),
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (featured) {
                FeaturedBadge()
                Spacer(modifier = Modifier.height(48.dp))
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = ArcaneTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    role?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = ArcaneTheme.typography.bodyMedium,
                            color = CareerColors.Amber
                        )
                    }
                }
                period?.let {
                    Text(
                        text = it,
                        style = ArcaneTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
