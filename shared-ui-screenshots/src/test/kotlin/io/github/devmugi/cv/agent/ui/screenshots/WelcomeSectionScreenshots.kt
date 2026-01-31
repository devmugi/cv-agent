package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.WelcomeSection
import org.junit.Test

class WelcomeSectionScreenshots : ScreenshotTest() {

    @Test
    fun welcome_default() = snapshotBothThemes("default") {
        WelcomeSection(
            suggestions = listOf(
                "Tell me about yourself",
                "What projects have you worked on?",
                "What are your skills?"
            ),
            onSuggestionClick = {}
        )
    }
}
