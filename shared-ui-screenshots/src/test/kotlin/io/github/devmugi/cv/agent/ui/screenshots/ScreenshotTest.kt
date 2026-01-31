package io.github.devmugi.cv.agent.ui.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
 * Base class for screenshot tests.
 *
 * Provides helpers for capturing screenshots with consistent theming and naming.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w400dp-h800dp-xhdpi")
abstract class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(
            changeThreshold = 0.01f // 1% pixel difference tolerance
        )
    )

    /**
     * Capture a screenshot with the given name and theme.
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
        val fileName = "src/test/snapshots/images/${this::class.simpleName}_${name}_$themeSuffix.png"

        val colors = if (darkTheme) ArcaneColors.agent2Dark() else ArcaneColors.agent2Light()

        composeRule.setContent {
            ArcaneTheme(colors = colors) {
                Box(
                    modifier = Modifier
                        .background(ArcaneTheme.colors.surfaceContainerLow)
                        .padding(8.dp)
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
     * @param name Test name suffix
     * @param content Composable content to capture
     */
    protected fun snapshotBothThemes(
        name: String,
        content: @Composable () -> Unit
    ) {
        snapshot(name, darkTheme = false, content = content)
        snapshot(name, darkTheme = true, content = content)
    }
}
