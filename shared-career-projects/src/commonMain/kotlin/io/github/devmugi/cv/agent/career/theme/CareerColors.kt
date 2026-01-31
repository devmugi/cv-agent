package io.github.devmugi.cv.agent.career.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Centralized color tokens for career/CV UI components.
 * Use these instead of defining colors locally in components.
 */
@Immutable
object CareerColors {
    /** Primary amber - main accent color */
    val Amber = Color(0xFFFFC107)

    /** Light amber - for text on dark backgrounds */
    val AmberLight = Color(0xFFFFD54F)

    /** Dark amber - for emphasis */
    val AmberDark = Color(0xFFFFA000)

    /** Warm background tint */
    val Background = Color(0xFFFFF8E1)

    /** Dark background for contrast */
    val BackgroundDark = Color(0xFF3E2723)
}
