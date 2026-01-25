package io.github.devmugi.cv.agent.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SuggestionsPayload(
    val suggestions: List<String> = emptyList()
)

data class SuggestionResult(
    val cleanedContent: String,
    val suggestions: List<String>
)

class SuggestionExtractor {
    private val json = Json { ignoreUnknownKeys = true }

    // Primary pattern: JSON wrapped in markdown code block
    private val jsonBlockPattern = Regex(
        """```json\s*(\{[\s\S]*?\})\s*```""",
        RegexOption.MULTILINE
    )

    fun extract(content: String): SuggestionResult {
        // Try markdown code block first (preferred format)
        jsonBlockPattern.find(content)?.let { match ->
            return parseAndClean(content, match.value, match.groupValues[1])
        }

        // Fallback: find raw JSON without code block using string search
        findRawSuggestionsJson(content)?.let { (jsonStr, fullMatch) ->
            return parseAndClean(content, fullMatch, jsonStr)
        }

        return SuggestionResult(content, emptyList())
    }

    private fun findRawSuggestionsJson(content: String): Pair<String, String>? {
        val startMarker = """{"suggestions":"""
        val altStartMarker = """{"suggestions" :"""

        var startIdx = content.indexOf(startMarker)
        if (startIdx == -1) startIdx = content.indexOf(altStartMarker)
        if (startIdx == -1) return null

        // Find the closing ]}
        val endMarker = "]}"
        val endIdx = content.indexOf(endMarker, startIdx)
        if (endIdx == -1) return null

        val fullMatch = content.substring(startIdx, endIdx + endMarker.length)
        return fullMatch to fullMatch
    }

    private fun parseAndClean(content: String, fullMatch: String, jsonStr: String): SuggestionResult {
        val suggestions = try {
            json.decodeFromString<SuggestionsPayload>(jsonStr).suggestions
        } catch (_: Exception) {
            emptyList()
        }
        val cleanedContent = content.replace(fullMatch, "").trim()
        return SuggestionResult(cleanedContent, suggestions)
    }
}
