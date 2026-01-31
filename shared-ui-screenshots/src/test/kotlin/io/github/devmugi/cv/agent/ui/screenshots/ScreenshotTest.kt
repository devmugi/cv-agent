package io.github.devmugi.cv.agent.ui.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.devmugi.arcane.design.foundation.theme.ArcaneColors
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Base class for screenshot tests using Roborazzi.
 *
 * Provides helpers for capturing screenshots with consistent theming and naming.
 *
 * ## Snapshot File Location
 *
 * Snapshots are stored in: `shared-ui-screenshots/src/test/snapshots/images/`
 *
 * File naming convention: `{TestClassName}_{name}_{theme}.png`
 * - Example: `ChatMessageScreenshotTest_user_short_light.png`
 *
 * ## Workflow
 *
 * 1. **Record new snapshots**: `./gradlew :shared-ui-screenshots:recordRoborazziDebug`
 * 2. **Verify snapshots**: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug`
 * 3. **Compare changes**: `./gradlew :shared-ui-screenshots:compareRoborazziDebug`
 *
 * ## Customization
 *
 * Subclasses can override [roborazziOptions] to customize comparison behavior
 * (e.g., different threshold for specific component tests).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w400dp-h800dp-xhdpi")
abstract class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Roborazzi options for screenshot comparison.
     *
     * Override in subclasses to customize comparison behavior.
     */
    protected open val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(
            changeThreshold = CHANGE_THRESHOLD_PERCENT
        )
    )

    companion object {
        /**
         * Pixel difference tolerance for screenshot comparison.
         * 1% allows for minor anti-aliasing differences across environments.
         */
        const val CHANGE_THRESHOLD_PERCENT = 0.01f

        /**
         * Padding around captured components to prevent edge clipping
         * and provide visual breathing room in snapshot images.
         */
        val SNAPSHOT_PADDING: Dp = 8.dp

        /** Directory path for snapshot images relative to module root. */
        private const val SNAPSHOTS_DIR = "src/test/snapshots/images"
    }

    /**
     * Capture a screenshot with the given name and theme.
     *
     * The snapshot will be saved to [SNAPSHOTS_DIR] with the naming convention:
     * `{TestClassName}_{name}_{light|dark}.png`
     *
     * @param name Test name suffix (e.g., "user_short")
     * @param darkTheme Whether to use dark theme
     * @param content Composable content to capture
     */
    protected fun snapshot(
        name: String,
        darkTheme: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val themeSuffix = if (darkTheme) "dark" else "light"
        val fileName = "$SNAPSHOTS_DIR/${this::class.simpleName}_${name}_$themeSuffix.png"

        val colors = if (darkTheme) ArcaneColors.agent2Dark() else ArcaneColors.agent2Light()

        composeRule.setContent {
            ArcaneTheme(colors = colors) {
                Box(
                    modifier = Modifier
                        .background(ArcaneTheme.colors.surfaceContainerLow)
                        .padding(SNAPSHOT_PADDING)
                ) {
                    content()
                }
            }
        }

        composeRule.onRoot().captureRoboImage(
            filePath = fileName,
            roborazziOptions = roborazziOptions
        )
    }

    /**
     * Capture both light and dark theme variants.
     *
     * Uses a mutable state to switch themes without calling setContent twice,
     * which is not allowed by the Compose test rule.
     *
     * @param name Test name suffix
     * @param content Composable content to capture
     */
    protected fun snapshotBothThemes(
        name: String,
        content: @Composable () -> Unit
    ) {
        var darkTheme by mutableStateOf(false)

        composeRule.setContent {
            val colors = if (darkTheme) ArcaneColors.agent2Dark() else ArcaneColors.agent2Light()
            ArcaneTheme(colors = colors) {
                Box(
                    modifier = Modifier
                        .background(ArcaneTheme.colors.surfaceContainerLow)
                        .padding(SNAPSHOT_PADDING)
                ) {
                    content()
                }
            }
        }

        // Capture light theme
        val lightFileName = "$SNAPSHOTS_DIR/${this::class.simpleName}_${name}_light.png"
        composeRule.onRoot().captureRoboImage(
            filePath = lightFileName,
            roborazziOptions = roborazziOptions
        )

        // Switch to dark theme and capture
        darkTheme = true
        composeRule.waitForIdle()

        val darkFileName = "$SNAPSHOTS_DIR/${this::class.simpleName}_${name}_dark.png"
        composeRule.onRoot().captureRoboImage(
            filePath = darkFileName,
            roborazziOptions = roborazziOptions
        )
    }
}
