package io.github.devmugi.cv.agent.ui.screenshots.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.ui.adaptive.AdaptiveScaffold
import io.github.devmugi.cv.agent.ui.adaptive.WindowWidthSizeClass
import io.github.devmugi.cv.agent.ui.navigation.Route
import io.github.devmugi.cv.agent.ui.screenshots.ScreenshotTest
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * Screenshot tests for [AdaptiveScaffold] with content.
 *
 * Tests the scaffold layout with a placeholder content area to verify
 * correct content positioning relative to navigation components.
 */
class AdaptiveScaffoldScreenshots : ScreenshotTest() {

    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun scaffold_compact_withContent() = snapshot("scaffold_compact_withContent", darkTheme = false) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Compact,
            currentRoute = Route.Chat,
            onNavigate = {},
            content = { ContentPlaceholder() }
        )
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-xhdpi")
    fun scaffold_medium_withContent() = snapshot("scaffold_medium_withContent", darkTheme = false) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Medium,
            currentRoute = Route.Chat,
            onNavigate = {},
            content = { ContentPlaceholder() }
        )
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp-xhdpi")
    fun scaffold_expanded_withContent() = snapshot("scaffold_expanded_withContent", darkTheme = false) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Expanded,
            currentRoute = Route.Chat,
            onNavigate = {},
            content = { ContentPlaceholder() }
        )
    }
}

@Suppress("TestFunctionName")
@androidx.compose.runtime.Composable
private fun ContentPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArcaneTheme.colors.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Content Area",
            style = ArcaneTheme.typography.titleLarge,
            color = ArcaneTheme.colors.text
        )
    }
}
