package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

@Composable
fun Disclaimer(
    modifier: Modifier = Modifier,
    text: String = "AI responses may contain errors. Please double-check."
) {
    Row(
        modifier = modifier
            .background(
                color = ArcaneTheme.colors.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("disclaimer"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = ArcaneTheme.typography.labelSmall,
            color = ArcaneTheme.colors.text.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
