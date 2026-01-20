package io.github.devmugi.cv.agent.data

import io.github.devmugi.cv.agent.data.repository.CVRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class CVRepositoryTest {

    private val repository = CVRepository()

    private val testJson = """
        {
            "personalInfo": {
                "name": "Denys Honcharenko",
                "location": "Genk, Belgium",
                "email": "test@test.com",
                "phone": "+32 123 456 789",
                "linkedin": "https://linkedin.com/in/test",
                "github": "https://github.com/test",
                "portfolio": "https://test.com"
            },
            "summary": "Test summary",
            "skills": [
                {
                    "id": "skills.kmp",
                    "category": "Kotlin Multiplatform",
                    "level": "Specialist",
                    "skills": ["KMM", "Compose"]
                }
            ],
            "experience": [
                {
                    "id": "experience.mcdonalds",
                    "title": "Android Engineer",
                    "company": "McDonald's",
                    "period": "Aug 2021 - Oct 2022",
                    "description": "Test description",
                    "highlights": ["100M+ Downloads"],
                    "technologies": ["KMM", "Kotlin"],
                    "featured": true
                }
            ],
            "projects": [
                {
                    "id": "project.mtg-deckbuilder",
                    "name": "MTG DeckBuilder",
                    "type": "Open Source",
                    "description": "Test description",
                    "technologies": ["Kotlin/JS"],
                    "featured": true
                }
            ],
            "achievements": [
                {
                    "id": "achievement.android-school-creator",
                    "title": "Android School Creator",
                    "organization": "EPAM Systems",
                    "year": "2020-2021",
                    "description": "Test description"
                }
            ],
            "education": {
                "degree": "Master's Degree",
                "field": "Computer Science",
                "institution": "Test University"
            }
        }
    """.trimIndent()

    @Test
    fun `getCVData returns parsed data`() {
        val result = repository.getCVData(testJson)

        assertEquals("Denys Honcharenko", result.personalInfo.name)
        assertEquals(1, result.skills.size)
        assertEquals(1, result.experience.size)
    }

    @Test
    fun `getCVData caches result`() {
        val first = repository.getCVData(testJson)
        val second = repository.getCVData(testJson)

        assertSame(first, second)
    }

    @Test
    fun `findExperienceById returns correct item`() {
        repository.getCVData(testJson)

        val result = repository.findExperienceById("experience.mcdonalds")

        assertNotNull(result)
        assertEquals("McDonald's", result.company)
    }

    @Test
    fun `findExperienceById returns null for unknown id`() {
        repository.getCVData(testJson)

        val result = repository.findExperienceById("experience.unknown")

        assertNull(result)
    }

    @Test
    fun `findProjectById returns correct item`() {
        repository.getCVData(testJson)

        val result = repository.findProjectById("project.mtg-deckbuilder")

        assertNotNull(result)
        assertEquals("MTG DeckBuilder", result.name)
    }

    @Test
    fun `findSkillCategoryById returns correct item`() {
        repository.getCVData(testJson)

        val result = repository.findSkillCategoryById("skills.kmp")

        assertNotNull(result)
        assertEquals("Kotlin Multiplatform", result.category)
    }

    @Test
    fun `findAchievementById returns correct item`() {
        repository.getCVData(testJson)

        val result = repository.findAchievementById("achievement.android-school-creator")

        assertNotNull(result)
        assertEquals("Android School Creator", result.title)
    }

    @Test
    fun `resolveReference creates correct CVReference for experience`() {
        repository.getCVData(testJson)

        val result = repository.resolveReference("experience.mcdonalds")

        assertNotNull(result)
        assertEquals("experience.mcdonalds", result.id)
        assertEquals("experience", result.type)
        assertEquals("McDonald's", result.label)
    }

    @Test
    fun `resolveReference creates correct CVReference for project`() {
        repository.getCVData(testJson)

        val result = repository.resolveReference("project.mtg-deckbuilder")

        assertNotNull(result)
        assertEquals("project", result.type)
        assertEquals("MTG DeckBuilder", result.label)
    }

    @Test
    fun `resolveReference creates correct CVReference for skill`() {
        repository.getCVData(testJson)

        val result = repository.resolveReference("skills.kmp")

        assertNotNull(result)
        assertEquals("skill", result.type)
        assertEquals("Kotlin Multiplatform", result.label)
    }

    @Test
    fun `resolveReference returns null for unknown id`() {
        repository.getCVData(testJson)

        val result = repository.resolveReference("unknown.id")

        assertNull(result)
    }
}
