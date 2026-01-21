package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.domain.models.CVReference
import io.github.devmugi.cv.agent.data.repository.CVRepository

data class ExtractionResult(
    val cleanedContent: String,
    val references: List<CVReference>
)

class ReferenceExtractor(
    private val repository: CVRepository
) {
    private val referencePattern = """\[(Experience|Project|Skill|Achievement|Education):\s*([^\]]+)\]""".toRegex()

    fun extract(content: String): ExtractionResult {
        val references = mutableListOf<CVReference>()
        var cleanedContent = content

        referencePattern.findAll(content).forEach { match ->
            val id = match.groupValues[2].trim()
            val resolved = repository.resolveReference(id)
            if (resolved != null) {
                cleanedContent = cleanedContent.replace(match.value, resolved.label)
                if (references.none { it.id == resolved.id }) {
                    references.add(resolved)
                }
            }
        }

        return ExtractionResult(cleanedContent, references)
    }
}
