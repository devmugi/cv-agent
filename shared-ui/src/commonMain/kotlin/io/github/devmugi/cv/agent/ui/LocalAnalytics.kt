package io.github.devmugi.cv.agent.ui

import androidx.compose.runtime.compositionLocalOf
import io.github.devmugi.cv.agent.analytics.Analytics

/**
 * CompositionLocal for providing Analytics to UI components.
 * Defaults to NOOP implementation if not provided.
 */
val LocalAnalytics = compositionLocalOf<Analytics> { Analytics.NOOP }
