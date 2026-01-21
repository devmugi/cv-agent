package io.github.devmugi.cv.agent.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun CVAgentTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CVAgentColorScheme,
        typography = CVAgentTypography,
        shapes = CVAgentShapes,
        content = content
    )
}
