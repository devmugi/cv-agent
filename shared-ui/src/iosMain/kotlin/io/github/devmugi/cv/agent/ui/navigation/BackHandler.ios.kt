package io.github.devmugi.cv.agent.ui.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses NavController's built-in back handling
    // Swipe gesture will be added separately via UIKit interop
}
