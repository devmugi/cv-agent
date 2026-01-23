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
    private val jsonBlockPattern = Regex(
        """```json\s*(\{[\s\S]*?\})\s*```""",
        RegexOption.MULTILINE
    )

    fun extract(content: String): SuggestionResult {
        val match = jsonBlockPattern.find(content)
            ?: return SuggestionResult(content, emptyList())

        val jsonStr = match.groupValues[1]
        val suggestions = try {
            json.decodeFromString<SuggestionsPayload>(jsonStr).suggestions
        } catch (_: Exception) {
            emptyList()
        }

        val cleanedContent = content.replace(match.value, "").trim()
        return SuggestionResult(cleanedContent, suggestions)
    }
}
