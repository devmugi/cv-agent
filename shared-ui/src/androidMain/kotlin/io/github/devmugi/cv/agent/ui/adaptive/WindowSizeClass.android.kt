package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
actual fun calculateWindowSizeClass(): WindowWidthSizeClass {
    val configuration = LocalConfiguration.current
    return WindowWidthSizeClass.fromWidth(configuration.screenWidthDp.dp)
}
