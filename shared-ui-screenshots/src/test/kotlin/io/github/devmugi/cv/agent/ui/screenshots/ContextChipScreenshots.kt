package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.ContextChip
import org.junit.Test

class ContextChipScreenshots : ScreenshotTest() {

    @Test
    fun contextChip_default() = snapshotBothThemes("default") {
        ContextChip(
            onDismiss = {}
        )
    }
}
