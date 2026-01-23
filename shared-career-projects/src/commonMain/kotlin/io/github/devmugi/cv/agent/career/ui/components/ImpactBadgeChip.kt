package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

private val HighlightRed = Color(0xFFE53935)

@Composable
fun ImpactBadgeChip(
    text: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (highlight) {
        HighlightRed.copy(alpha = 0.15f)
    } else {
        ArcaneTheme.colors.surfaceContainerLow
    }

    val contentColor = if (highlight) {
        HighlightRed
    } else {
        ArcaneTheme.colors.text
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = ArcaneTheme.typography.labelMedium,
            color = contentColor
        )
    }
}
