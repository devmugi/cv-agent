package io.github.devmugi.cv.agent.ui.screenshots.adaptive

import io.github.devmugi.cv.agent.ui.adaptive.AdaptiveScaffold
import io.github.devmugi.cv.agent.ui.adaptive.WindowWidthSizeClass
import io.github.devmugi.cv.agent.ui.navigation.Route
import io.github.devmugi.cv.agent.ui.screenshots.ScreenshotTest
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * Screenshot tests for adaptive navigation components.
 *
 * Tests the three navigation variants provided by [AdaptiveScaffold]:
 * - Compact: Bottom navigation bar
 * - Medium: Navigation rail
 * - Expanded: Permanent navigation drawer
 *
 * Each variant is tested with different selected states and themes.
 */
class AdaptiveNavigationScreenshots : ScreenshotTest() {

    // ========== Compact (Bottom Bar) Tests ==========

    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun bottomBar_chatSelected() = snapshot("bottomBar_chatSelected", darkTheme = false) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Compact,
            currentRoute = Route.Chat,
            onNavigate = {},
            content = {}
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun bottomBar_careerSelected() = snapshot("bottomBar_careerSelected", darkTheme = false) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Compact,
            currentRoute = Route.CareerTimeline,
            onNavigate = {},
            content = {}
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun bottomBar_chatSelected_dark() = snapshot("bottomBar_chatSelected", darkTheme = true) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Compact,
            currentRoute = Route.Chat,
            onNavigate = {},
            content = {}
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun bottomBar_projectDetails() = snapshot("bottomBar_projectDetails", darkTheme = false) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Compact,
            currentRoute = Route.ProjectDetails("test-project"),
            onNavigate = {},
            content = {}
        )
    }

    // ========== Medium (Navigation Rail) Tests ==========

    @Test
    @Config(qualifiers = "w700dp-h900dp-xhdpi")
    fun navigationRail_chatSelected() = snapshot("navigationRail_chatSelected", darkTheme = false) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Medium,
            currentRoute = Route.Chat,
            onNavigate = {},
            content = {}
        )
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-xhdpi")
    fun navigationRail_careerSelected() = snapshot("navigationRail_careerSelected", darkTheme = false) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Medium,
            currentRoute = Route.CareerTimeline,
            onNavigate = {},
            content = {}
        )
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-xhdpi")
    fun navigationRail_chatSelected_dark() = snapshot("navigationRail_chatSelected", darkTheme = true) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Medium,
            currentRoute = Route.Chat,
            onNavigate = {},
            content = {}
        )
    }

    // ========== Expanded (Navigation Drawer) Tests ==========

    @Test
    @Config(qualifiers = "w1000dp-h800dp-xhdpi")
    fun navigationDrawer_chatSelected() = snapshot("navigationDrawer_chatSelected", darkTheme = false) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Expanded,
            currentRoute = Route.Chat,
            onNavigate = {},
            content = {}
        )
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp-xhdpi")
    fun navigationDrawer_careerSelected() = snapshot("navigationDrawer_careerSelected", darkTheme = false) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Expanded,
            currentRoute = Route.CareerTimeline,
            onNavigate = {},
            content = {}
        )
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp-xhdpi")
    fun navigationDrawer_chatSelected_dark() = snapshot("navigationDrawer_chatSelected", darkTheme = true) {
        AdaptiveScaffold(
            windowSizeClass = WindowWidthSizeClass.Expanded,
            currentRoute = Route.Chat,
            onNavigate = {},
            content = {}
        )
    }
}
