package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.models.Education
import io.github.devmugi.cv.agent.data.models.PersonalInfo
import io.github.devmugi.cv.agent.data.models.SkillCategory
import io.github.devmugi.cv.agent.data.models.WorkExperience
import io.github.devmugi.cv.agent.data.models.Project
import io.github.devmugi.cv.agent.data.models.Achievement
import kotlin.test.Test
import kotlin.test.assertTrue

class SystemPromptBuilderTest {

    private val testCVData = CVData(
        personalInfo = PersonalInfo(
            name = "Denys Honcharenko",
            location = "Lausanne, Switzerland",
            email = "test@example.com",
            phone = "+1234567890",
            linkedin = "linkedin.com/in/test",
            github = "github.com/test",
            portfolio = "portfolio.test"
        ),
        summary = "Experienced Software Engineer",
        skills = listOf(
            SkillCategory("skills.ai-dev", "AI Development", "Advanced", listOf("Claude API", "MCP"))
        ),
        experience = listOf(
            WorkExperience(
                "experience.geosatis", "Senior Dev", "GEOSATIS", "2023-Present",
                "Built apps", listOf("Led team"), listOf("Kotlin"), true
            )
        ),
        projects = listOf(
            Project("project.mtg", "MTG App", "Mobile", "Card app", listOf("KMP"), null, true)
        ),
        achievements = listOf(
            Achievement("achievement.claude", "Claude Power User", "Anthropic", "2024", "Certified")
        ),
        education = Education("BSc", "Computer Science", "University")
    )

    private val builder = SystemPromptBuilder()

    @Test
    fun promptContainsPersonaInstructions() {
        val prompt = builder.build(testCVData)
        assertTrue(prompt.contains("Denys Honcharenko"))
        assertTrue(prompt.contains("third person"))
    }

    @Test
    fun promptContainsReferenceFormatInstructions() {
        val prompt = builder.build(testCVData)
        assertTrue(prompt.contains("[Experience:"))
        assertTrue(prompt.contains("[Project:"))
    }

    @Test
    fun promptContainsAllCVSections() {
        val prompt = builder.build(testCVData)
        assertTrue(prompt.contains("PERSONAL INFO"))
        assertTrue(prompt.contains("SKILLS"))
        assertTrue(prompt.contains("WORK EXPERIENCE"))
        assertTrue(prompt.contains("PROJECTS"))
        assertTrue(prompt.contains("ACHIEVEMENTS"))
        assertTrue(prompt.contains("EDUCATION"))
    }

    @Test
    fun promptContainsActualCVData() {
        val prompt = builder.build(testCVData)
        assertTrue(prompt.contains("GEOSATIS"))
        assertTrue(prompt.contains("experience.geosatis"))
        assertTrue(prompt.contains("AI Development"))
        assertTrue(prompt.contains("Claude Power User"))
    }
}
