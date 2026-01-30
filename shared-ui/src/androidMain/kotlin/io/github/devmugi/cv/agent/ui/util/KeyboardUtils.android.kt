package io.github.devmugi.cv.agent.ui.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable

@Composable
actual fun isKeyboardVisible(): Boolean = WindowInsets.isImeVisible
