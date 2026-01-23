package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

@Composable
fun ContextChip(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(ArcaneTheme.colors.primary)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("context_chip"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "Conversation history",
            tint = ArcaneTheme.colors.surfaceContainerLow,
            modifier = Modifier.size(18.dp)
        )

        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Clear history",
            tint = ArcaneTheme.colors.surfaceContainerLow,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(18.dp)
                .clickable(onClick = onDismiss)
                .testTag("context_chip_dismiss")
        )
    }
}
