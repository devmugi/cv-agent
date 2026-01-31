package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.SuggestionChip
import org.junit.Test

class SuggestionChipScreenshots : ScreenshotTest() {

    @Test
    fun chip_short() = snapshot("short") {
        SuggestionChip(
            text = "Skills",
            onClick = {}
        )
    }

    @Test
    fun chip_medium() = snapshot("medium") {
        SuggestionChip(
            text = "Tell me about your experience",
            onClick = {}
        )
    }

    @Test
    fun chip_long() = snapshot("long") {
        SuggestionChip(
            text = "What challenging projects have you worked on recently?",
            onClick = {}
        )
    }
}
