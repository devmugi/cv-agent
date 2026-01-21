package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

enum class FeedbackState {
    NONE, LIKED, DISLIKED
}

@Composable
fun MessageActions(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
    feedbackState: FeedbackState = FeedbackState.NONE
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("message_actions"),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionIcon(
            icon = Icons.Default.ContentCopy,
            contentDescription = "Copy",
            onClick = onCopy,
            testTag = "action_copy"
        )
        ActionIcon(
            icon = Icons.Default.Share,
            contentDescription = "Share",
            onClick = onShare,
            testTag = "action_share"
        )
        ActionIcon(
            icon = if (feedbackState == FeedbackState.LIKED) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            contentDescription = "Like",
            onClick = onLike,
            testTag = "action_like",
            isActive = feedbackState == FeedbackState.LIKED
        )
        ActionIcon(
            icon = if (feedbackState == FeedbackState.DISLIKED) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
            contentDescription = "Dislike",
            onClick = onDislike,
            testTag = "action_dislike",
            isActive = feedbackState == FeedbackState.DISLIKED
        )
        ActionIcon(
            icon = Icons.Default.Refresh,
            contentDescription = "Regenerate",
            onClick = onRegenerate,
            testTag = "action_regenerate"
        )
    }
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (isActive) ArcaneTheme.colors.primary else ArcaneTheme.colors.textSecondary,
        modifier = modifier
            .size(20.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 16.dp),
                onClick = onClick
            )
            .testTag(testTag)
    )
}
