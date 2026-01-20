package io.github.devmugi.cv.agent.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Brand Colors (Portfolio Theme)
val DarkNavy = Color(0xFF1A1D2E)
val DarkBlue = Color(0xFF16213E)
val DarkSurface = Color(0xFF1E2746)
val Gold = Color(0xFFF5A623)
val GoldBright = Color(0xFFFFC947)
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFB0B3C1)
val ErrorContainer = Color(0xFF3D2936)
val OnErrorContainer = Color(0xFFFFB4AB)
val ReferenceChipBg = Color(0xFF2A3A5C)
val CodeBlockBg = Color(0xFF252A3D)

// Material3 Dark Color Scheme
val CVAgentColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = DarkNavy,
    primaryContainer = Gold.copy(alpha = 0.12f),
    onPrimaryContainer = GoldBright,

    secondary = LightGray,
    onSecondary = DarkNavy,

    background = DarkNavy,
    onBackground = White,

    surface = DarkSurface,
    onSurface = White,

    surfaceVariant = DarkBlue,
    onSurfaceVariant = LightGray,

    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)
