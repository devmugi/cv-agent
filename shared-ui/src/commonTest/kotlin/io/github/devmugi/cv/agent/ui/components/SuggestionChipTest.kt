package io.github.devmugi.cv.agent.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SuggestionChipTest {

    @Test
    fun suggestionChipCallbackReceivesCorrectText() {
        var clickedText: String? = null
        val testText = "What's Denys's experience?"

        // Simulate click callback
        val callback: (String) -> Unit = { clickedText = it }
        simulateSuggestionChipClick(testText, callback)

        assertEquals(testText, clickedText)
    }

    @Test
    fun suggestionChipTextIsDisplayed() {
        val text = "Tell me about skills"
        val displayText = getSuggestionChipDisplayText(text)
        assertEquals(text, displayText)
    }
}
