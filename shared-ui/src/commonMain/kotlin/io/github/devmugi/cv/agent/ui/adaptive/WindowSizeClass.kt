package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size class breakpoints following Material Design 3 guidelines.
 */
@Immutable
enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded;

    companion object {
        private val CompactMaxWidth = 600.dp
        private val MediumMaxWidth = 840.dp

        fun fromWidth(width: Dp): WindowWidthSizeClass = when {
            width < CompactMaxWidth -> Compact
            width < MediumMaxWidth -> Medium
            else -> Expanded
        }
    }
}

@Composable
expect fun calculateWindowSizeClass(): WindowWidthSizeClass
