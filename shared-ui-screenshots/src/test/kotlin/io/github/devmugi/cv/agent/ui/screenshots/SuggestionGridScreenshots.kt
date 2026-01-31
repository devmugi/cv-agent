package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.SuggestionChipsGrid
import org.junit.Test

class SuggestionGridScreenshots : ScreenshotTest() {

    @Test
    fun grid_single() = snapshotBothThemes("single") {
        SuggestionChipsGrid(
            suggestions = listOf("Tell me about yourself"),
            onSuggestionClick = {}
        )
    }

    @Test
    fun grid_three() = snapshotBothThemes("three") {
        SuggestionChipsGrid(
            suggestions = listOf("Experience", "Skills", "Projects"),
            onSuggestionClick = {}
        )
    }

    @Test
    fun grid_six() = snapshotBothThemes("six") {
        SuggestionChipsGrid(
            suggestions = listOf(
                "Android", "Kotlin", "Compose",
                "Architecture", "Testing", "CI/CD"
            ),
            onSuggestionClick = {}
        )
    }

    @Test
    fun grid_overflow() = snapshotBothThemes("overflow") {
        SuggestionChipsGrid(
            suggestions = listOf(
                "Tell me about your Android experience",
                "What projects have you worked on?",
                "Describe your testing approach",
                "How do you handle architecture?",
                "What's your experience with Compose?"
            ),
            onSuggestionClick = {}
        )
    }
}
