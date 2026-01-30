package io.github.devmugi.cv.agent.ui.util

import androidx.compose.runtime.Composable

/**
 * Returns whether the software keyboard is currently visible.
 * Platform-specific implementations differ:
 * - Android: Uses WindowInsets.isImeVisible
 * - iOS: Returns false (keyboard detection not implemented)
 */
@Composable
expect fun isKeyboardVisible(): Boolean
