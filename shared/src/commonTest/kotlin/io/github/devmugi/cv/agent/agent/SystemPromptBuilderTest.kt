package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.Description
import io.github.devmugi.cv.agent.career.models.Overview
import io.github.devmugi.cv.agent.career.models.Period
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.career.models.SkillCategory
import io.github.devmugi.cv.agent.career.models.Technologies
import io.github.devmugi.cv.agent.career.models.Technology
import kotlin.test.Test
import kotlin.test.assertTrue

class SystemPromptBuilderTest {

    private val testPersonalInfo = PersonalInfo(
        name = "Test Name",
        title = "Test Title",
        location = "Test Location",
        email = "test@test.com",
        linkedin = "https://linkedin.com",
        github = "https://github.com",
        portfolio = "https://portfolio.com",
        summary = "Test summary",
        skills = listOf(SkillCategory("Languages", listOf("Kotlin", "Java")))
    )

    private val testProject = CareerProject(
        id = "test-project",
        name = "Test Project",
        slug = "test-project",
        tagline = "Test tagline",
        overview = Overview(
            company = "Test Company",
            role = "Test Role",
            period = Period(displayText = "2020")
        ),
        description = Description(short = "Short desc", full = "Full desc"),
        technologies = Technologies(primary = listOf(Technology(name = "Kotlin")))
    )

    private val builder = SystemPromptBuilder()

    @Test
    fun includesPersonalInfo() {
        val dataProvider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val prompt = builder.build(dataProvider)

        assertTrue(prompt.contains("Test Name"))
        assertTrue(prompt.contains("Test Title"))
        assertTrue(prompt.contains("Test Location"))
    }

    @Test
    fun includesSkills() {
        val dataProvider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val prompt = builder.build(dataProvider)

        assertTrue(prompt.contains("Languages"))
        assertTrue(prompt.contains("Kotlin"))
        assertTrue(prompt.contains("Java"))
    }

    @Test
    fun includesProjectIndex() {
        val dataProvider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val prompt = builder.build(dataProvider)

        assertTrue(prompt.contains("test-project"))
        assertTrue(prompt.contains("Test Project"))
    }

    @Test
    fun includesFeaturedProjectDetails() {
        val dataProvider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = listOf("test-project")
        )

        val prompt = builder.build(dataProvider)

        assertTrue(prompt.contains("Test Company"))
        assertTrue(prompt.contains("Full desc"))
    }

    @Test
    fun includesSuggestionInstructions() {
        val dataProvider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val prompt = builder.build(dataProvider)

        assertTrue(prompt.contains("suggestions"))
        assertTrue(prompt.contains("json"))
    }
}
