package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.data.models.CVReference
import io.github.devmugi.cv.agent.data.repository.CVRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReferenceExtractorTest {

    private val mockRepository = object : CVRepository() {
        override fun resolveReference(id: String): CVReference? {
            return when (id) {
                "experience.geosatis" -> CVReference("experience.geosatis", "experience", "GEOSATIS")
                "project.mtg-deckbuilder" -> CVReference("project.mtg-deckbuilder", "project", "MTG DeckBuilder")
                "skills.ai-dev" -> CVReference("skills.ai-dev", "skill", "AI Development")
                "achievement.android-school" -> CVReference("achievement.android-school", "achievement", "Android School Creator")
                "education.masters" -> CVReference("education.masters", "education", "Master's Degree")
                else -> null
            }
        }
    }

    private val extractor = ReferenceExtractor(mockRepository)

    @Test
    fun extractsSingleReference() {
        val input = "Denys worked at [Experience: experience.geosatis] as a developer."
        val result = extractor.extract(input)

        assertEquals("Denys worked at GEOSATIS as a developer.", result.cleanedContent)
        assertEquals(1, result.references.size)
        assertEquals("experience.geosatis", result.references.first().id)
    }

    @Test
    fun extractsMultipleReferences() {
        val input = "He built [Project: project.mtg-deckbuilder] using [Skill: skills.ai-dev] skills."
        val result = extractor.extract(input)

        assertEquals("He built MTG DeckBuilder using AI Development skills.", result.cleanedContent)
        assertEquals(2, result.references.size)
    }

    @Test
    fun handlesUnknownIdGracefully() {
        val input = "Denys worked at [Experience: experience.unknown] company."
        val result = extractor.extract(input)

        assertEquals("Denys worked at [Experience: experience.unknown] company.", result.cleanedContent)
        assertTrue(result.references.isEmpty())
    }

    @Test
    fun handlesMalformedBrackets() {
        val input = "This has [incomplete bracket and normal text."
        val result = extractor.extract(input)

        assertEquals(input, result.cleanedContent)
        assertTrue(result.references.isEmpty())
    }

    @Test
    fun deduplicatesReferences() {
        val input = "[Experience: experience.geosatis] and again [Experience: experience.geosatis]."
        val result = extractor.extract(input)

        assertEquals("GEOSATIS and again GEOSATIS.", result.cleanedContent)
        assertEquals(1, result.references.size)
    }

    @Test
    fun handlesTextWithNoReferences() {
        val input = "Just regular text without any references."
        val result = extractor.extract(input)

        assertEquals(input, result.cleanedContent)
        assertTrue(result.references.isEmpty())
    }

    @Test
    fun extractsAchievementTypeReferences() {
        val input = "He earned [Achievement: achievement.android-school] recognition."
        val result = extractor.extract(input)

        assertEquals("He earned Android School Creator recognition.", result.cleanedContent)
        assertEquals(1, result.references.size)
        assertEquals("achievement.android-school", result.references.first().id)
    }

    @Test
    fun extractsEducationTypeReferences() {
        val input = "Denys holds a [Education: education.masters] in Computer Science."
        val result = extractor.extract(input)

        assertEquals("Denys holds a Master's Degree in Computer Science.", result.cleanedContent)
        assertEquals(1, result.references.size)
        assertEquals("education.masters", result.references.first().id)
    }
}
