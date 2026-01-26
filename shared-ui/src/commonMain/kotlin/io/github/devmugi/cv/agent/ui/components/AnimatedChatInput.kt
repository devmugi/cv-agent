package io.github.devmugi.cv.agent.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.devmugi.arcane.design.foundation.tokens.ArcaneRadius
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.chat.components.input.ArcaneAgentChatInput
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

@Composable
fun AnimatedChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String = "",
    enabled: Boolean = true,
    isRecording: Boolean = false,
    isTranscribing: Boolean = false,
    onRecordingStart: () -> Unit = {},
    onRecordingStop: () -> Unit = {},
    onVoiceToTextClick: (() -> Unit)? = null,
    onAudioRecordClick: (() -> Unit)? = null,
    activeItemsContent: (@Composable RowScope.() -> Unit)? = null,
    onInputFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val widthFraction by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.9f,
        animationSpec = tween(durationMillis = 200),
        label = "inputWidthAnimation"
    )

    val inputShape = ArcaneRadius.Large

    LaunchedEffect(isFocused) {
        onInputFocusChanged(isFocused)
    }

    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .background(
                color = ArcaneTheme.colors.surfaceContainerLow,
                shape = inputShape
            ),
        contentAlignment = Alignment.Center
    ) {
        ArcaneAgentChatInput(
            value = value,
            onValueChange = onValueChange,
            onSend = onSend,
            placeholder = placeholder,
            enabled = enabled && !isRecording && !isTranscribing,
            onVoiceToTextClick = onVoiceToTextClick,
            onAudioRecordClick = onAudioRecordClick,
            activeItemsContent = activeItemsContent,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused || focusState.hasFocus
                }
        )
    }
}
