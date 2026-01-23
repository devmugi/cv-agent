package io.github.devmugi.cv.agent.career.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonalInfoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun deserializesPersonalInfo() {
        val jsonString = """
        {
          "name": "Test Name",
          "title": "Test Title",
          "location": "Test Location",
          "email": "test@example.com",
          "linkedin": "https://linkedin.com/test",
          "github": "https://github.com/test",
          "portfolio": "https://test.com",
          "summary": "Test summary",
          "skills": [
            { "category": "Category1", "items": ["Skill1", "Skill2"] }
          ]
        }
        """.trimIndent()

        val result = json.decodeFromString<PersonalInfo>(jsonString)

        assertEquals("Test Name", result.name)
        assertEquals("Test Title", result.title)
        assertEquals(1, result.skills.size)
        assertEquals("Category1", result.skills[0].category)
        assertEquals(listOf("Skill1", "Skill2"), result.skills[0].items)
    }
}
