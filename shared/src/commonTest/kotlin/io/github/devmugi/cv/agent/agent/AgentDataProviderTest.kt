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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentDataProviderTest {

    private val testPersonalInfo = PersonalInfo(
        name = "Test Name",
        title = "Test Title",
        location = "Test Location",
        email = "test@test.com",
        linkedin = "https://linkedin.com",
        github = "https://github.com",
        portfolio = "https://portfolio.com",
        summary = "Test summary",
        skills = listOf(SkillCategory("Category", listOf("Skill1", "Skill2")))
    )

    private val testProject = CareerProject(
        id = "test-project",
        name = "Test Project",
        slug = "test-project",
        tagline = "Test tagline",
        overview = Overview(
            company = "Test Company",
            client = "Test Client",
            product = "Test Product",
            role = "Test Role",
            period = Period(displayText = "Jan 2020 - Dec 2020")
        ),
        description = Description(
            short = "Short description",
            full = "Full description"
        ),
        technologies = Technologies(
            primary = listOf(Technology(name = "Kotlin"), Technology(name = "Android"))
        )
    )

    @Test
    fun buildsProjectIndex() {
        val provider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val index = provider.getProjectIndex()

        assertEquals(1, index.size)
        assertEquals("test-project", index[0].id)
        assertEquals("Test Project", index[0].name)
        assertEquals("Test Role", index[0].role)
        assertEquals("Jan 2020 - Dec 2020", index[0].period)
        assertEquals("Test tagline", index[0].tagline)
    }

    @Test
    fun extractsCuratedDetailsForFeatured() {
        val provider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = listOf("test-project")
        )

        val details = provider.getCuratedDetails("test-project")

        assertTrue(details != null)
        assertTrue(details!!.contains("Test Company"))
        assertTrue(details.contains("Test Product"))
        assertTrue(details.contains("Short description"))
        assertTrue(details.contains("Kotlin"))
    }

    @Test
    fun returnsNullForNonFeatured() {
        val provider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val details = provider.getCuratedDetails("test-project")

        assertEquals(null, details)
    }
}
