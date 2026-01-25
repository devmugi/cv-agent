package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.chat.components.blocks.MessageBlockRenderer
import io.github.devmugi.arcane.chat.models.MessageBlock
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.arcane.design.foundation.tokens.ArcaneSpacing

private val UserMessageShape = RoundedCornerShape(
    topStart = 12.dp,
    topEnd = 4.dp,
    bottomStart = 12.dp,
    bottomEnd = 12.dp
)

/**
 * Renders a user message block with outlined border style.
 * Uses border instead of filled background for a cleaner, modern look.
 */
@Composable
fun OutlinedUserMessageBlock(
    blocks: List<MessageBlock>,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp,
    borderColor: Color = ArcaneTheme.colors.primary.copy(alpha = 0.4f),
    borderWidth: Dp = 1.dp
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(UserMessageShape)
                .border(borderWidth, borderColor, UserMessageShape)
                .padding(
                    horizontal = ArcaneSpacing.Small,
                    vertical = ArcaneSpacing.XSmall
                ),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(ArcaneSpacing.XSmall)
        ) {
            blocks.forEach { block ->
                MessageBlockRenderer(block)
            }
        }
    }
}
