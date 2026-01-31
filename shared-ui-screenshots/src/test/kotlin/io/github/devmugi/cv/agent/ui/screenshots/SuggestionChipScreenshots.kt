package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.SuggestionChip
import org.junit.Test

class SuggestionChipScreenshots : ScreenshotTest() {

    @Test
    fun chip_short() = snapshotBothThemes("short") {
        SuggestionChip(
            text = "Skills",
            onClick = {}
        )
    }

    @Test
    fun chip_medium() = snapshotBothThemes("medium") {
        SuggestionChip(
            text = "Tell me about your experience",
            onClick = {}
        )
    }

    @Test
    fun chip_long() = snapshotBothThemes("long") {
        SuggestionChip(
            text = "What challenging projects have you worked on recently?",
            onClick = {}
        )
    }
}
