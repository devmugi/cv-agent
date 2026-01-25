package io.github.devmugi.cv.agent.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SuggestionExtractorTest {

    private val extractor = SuggestionExtractor()

    @Test
    fun extractsSuggestionsFromJsonBlock() {
        val input = """
            Denys has extensive experience with BLE and IoT.

            ```json
            {"suggestions": ["adidas-gmr", "geosatis"]}
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertEquals("Denys has extensive experience with BLE and IoT.", result.cleanedContent.trim())
        assertEquals(listOf("adidas-gmr", "geosatis"), result.suggestions)
    }

    @Test
    fun handlesEmptySuggestions() {
        val input = """
            Just a regular response.

            ```json
            {"suggestions": []}
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertEquals("Just a regular response.", result.cleanedContent.trim())
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun handlesNoJsonBlock() {
        val input = "Response without any JSON block."

        val result = extractor.extract(input)

        assertEquals(input, result.cleanedContent)
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun handlesMalformedJson() {
        val input = """
            Some text.

            ```json
            {not valid json}
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun handlesSingleSuggestion() {
        val input = """
            Response text.

            ```json
            {"suggestions": ["mcdonalds"]}
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertEquals(listOf("mcdonalds"), result.suggestions)
    }

    @Test
    fun handlesWhitespaceVariations() {
        val input = """
            Response text.

            ```json
            {
              "suggestions": [
                "adidas-gmr",
                "geosatis"
              ]
            }
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertEquals(listOf("adidas-gmr", "geosatis"), result.suggestions)
    }

    @Test
    fun extractsRawJsonOnSameLine() {
        val input = """Some text about the project. {"suggestions": ["mcdonalds", "adidas-gmr"]}"""

        val result = extractor.extract(input)

        assertEquals("Some text about the project.", result.cleanedContent.trim())
        assertEquals(listOf("mcdonalds", "adidas-gmr"), result.suggestions)
    }

    @Test
    fun extractsRawJsonOnNewLine() {
        val input = """
            Some text about the project.
            {"suggestions": ["mcdonalds"]}
        """.trimIndent()

        val result = extractor.extract(input)

        assertEquals("Some text about the project.", result.cleanedContent.trim())
        assertEquals(listOf("mcdonalds"), result.suggestions)
    }

    @Test
    fun extractsRawJsonWithSpacesInArray() {
        val input = """Response text. {"suggestions": [ "a", "b" ]}"""

        val result = extractor.extract(input)

        assertEquals("Response text.", result.cleanedContent.trim())
        assertEquals(listOf("a", "b"), result.suggestions)
    }

    @Test
    fun prefersCodeBlockOverRawJson() {
        val input = """
            Text with raw json {"suggestions": ["wrong"]}

            ```json
            {"suggestions": ["correct"]}
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertEquals(listOf("correct"), result.suggestions)
    }

    @Test
    fun handlesEmptyRawJsonSuggestions() {
        val input = """Some text. {"suggestions": []}"""

        val result = extractor.extract(input)

        assertEquals("Some text.", result.cleanedContent.trim())
        assertTrue(result.suggestions.isEmpty())
    }
}
