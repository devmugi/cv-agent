package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun calculateWindowSizeClass(): WindowWidthSizeClass {
    val windowInfo = LocalWindowInfo.current
    val widthDp = (windowInfo.containerSize.width / 2).dp // Approximate conversion
    return WindowWidthSizeClass.fromWidth(widthDp)
}
