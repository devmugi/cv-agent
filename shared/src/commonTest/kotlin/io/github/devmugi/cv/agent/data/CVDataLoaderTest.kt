package io.github.devmugi.cv.agent.data

import io.github.devmugi.cv.agent.data.repository.CVDataLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CVDataLoaderTest {

    private val loader = CVDataLoader()

    private val validMinimalJson = """
        {
            "personalInfo": {
                "name": "Test Name",
                "location": "Test Location",
                "email": "test@test.com",
                "phone": "+1234567890",
                "linkedin": "https://linkedin.com/test",
                "github": "https://github.com/test",
                "portfolio": "https://test.com"
            },
            "summary": "Test summary",
            "skills": [],
            "experience": [],
            "projects": [],
            "achievements": [],
            "education": {
                "degree": "Test Degree",
                "field": "Test Field",
                "institution": "Test University"
            }
        }
    """.trimIndent()

    @Test
    fun `loads valid JSON successfully`() {
        val result = loader.load(validMinimalJson)

        assertEquals("Test Name", result.personalInfo.name)
        assertEquals("Test summary", result.summary)
        assertEquals("Test Degree", result.education.degree)
    }

    @Test
    fun `handles missing optional fields`() {
        val jsonWithOptionals = """
            {
                "personalInfo": {
                    "name": "Test",
                    "location": "Location",
                    "email": "test@test.com",
                    "phone": "123",
                    "linkedin": "https://linkedin.com",
                    "github": "https://github.com",
                    "portfolio": "https://portfolio.com"
                },
                "summary": "Summary",
                "skills": [{
                    "id": "skills.test",
                    "category": "Test Category",
                    "skills": ["Skill1"]
                }],
                "experience": [],
                "projects": [{
                    "id": "project.test",
                    "name": "Test Project",
                    "type": "Open Source",
                    "description": "Description",
                    "technologies": ["Kotlin"]
                }],
                "achievements": [{
                    "id": "achievement.test",
                    "title": "Test Achievement",
                    "year": "2024",
                    "description": "Description"
                }],
                "education": {
                    "degree": "Degree",
                    "field": "Field",
                    "institution": "Institution"
                }
            }
        """.trimIndent()

        val result = loader.load(jsonWithOptionals)

        assertEquals(null, result.skills[0].level)
        assertEquals(null, result.projects[0].links)
        assertEquals(null, result.achievements[0].organization)
    }

    @Test
    fun `throws on invalid JSON`() {
        val invalidJson = "{ invalid json }"

        assertFailsWith<Exception> {
            loader.load(invalidJson)
        }
    }

    @Test
    fun `throws on missing required fields`() {
        val missingFields = """
            {
                "personalInfo": {
                    "name": "Test"
                }
            }
        """.trimIndent()

        assertFailsWith<Exception> {
            loader.load(missingFields)
        }
    }
}
