package io.github.devmugi.cv.agent.ui.util

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun isKeyboardVisible(): Boolean = WindowInsets.isImeVisible
