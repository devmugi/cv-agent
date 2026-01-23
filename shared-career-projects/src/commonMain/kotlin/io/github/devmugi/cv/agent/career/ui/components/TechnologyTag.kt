package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

enum class TechnologyCategory {
    PRIMARY,
    SECONDARY,
    TOOL
}

@Composable
fun TechnologyTag(
    text: String,
    modifier: Modifier = Modifier,
    category: TechnologyCategory = TechnologyCategory.TOOL
) {
    val (backgroundColor, borderColor, textColor) = when (category) {
        TechnologyCategory.PRIMARY -> Triple(
            Color(0xFF3D3520), // Warm amber background
            Color(0xFFFFC107), // Amber border
            Color(0xFFFFD54F)  // Bright amber text
        )
        TechnologyCategory.SECONDARY -> Triple(
            Color(0xFF1E3A5F), // Blue background
            Color(0xFF64B5F6), // Blue border
            Color(0xFF90CAF9)  // Light blue text
        )
        TechnologyCategory.TOOL -> Triple(
            ArcaneTheme.colors.surfaceContainerLow,
            ArcaneTheme.colors.textSecondary.copy(alpha = 0.4f),
            ArcaneTheme.colors.text
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text,
            style = ArcaneTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
