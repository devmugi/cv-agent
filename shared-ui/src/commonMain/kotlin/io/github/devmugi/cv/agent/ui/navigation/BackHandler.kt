package io.github.devmugi.cv.agent.ui.navigation

import androidx.compose.runtime.Composable

/**
 * Platform-specific back navigation handler.
 * - Android: Uses androidx.activity.compose.BackHandler
 * - iOS: Uses gesture detection or no-op (NavController handles back)
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
