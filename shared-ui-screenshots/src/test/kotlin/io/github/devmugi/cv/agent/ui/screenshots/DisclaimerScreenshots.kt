package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.Disclaimer
import org.junit.Test

class DisclaimerScreenshots : ScreenshotTest() {

    @Test
    fun disclaimer_default() = snapshotBothThemes("default") {
        Disclaimer()
    }
}
