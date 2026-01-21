package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.domain.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TooltipProviderTest {

    private val testCVData = CVData(
        personalInfo = PersonalInfo(
            name = "Test User",
            location = "Test City",
            email = "test@test.com",
            phone = "123",
            linkedin = "linkedin",
            github = "github",
            portfolio = "portfolio"
        ),
        summary = "Test summary",
        skills = listOf(
            SkillCategory(id = "android", category = "Android", skills = listOf("Kotlin", "Java"))
        ),
        experience = listOf(
            WorkExperience(
                id = "takeaway",
                title = "Senior Developer",
                company = "Takeaway",
                period = "2020-2023",
                description = "Built mobile apps",
                highlights = listOf("Led team"),
                technologies = listOf("Kotlin")
            )
        ),
        projects = listOf(
            Project(
                id = "cv-agent",
                name = "CV Agent",
                type = "Mobile",
                description = "Portfolio app",
                technologies = listOf("KMP")
            )
        ),
        achievements = listOf(
            Achievement(
                id = "award1",
                title = "Best App",
                year = "2022",
                description = "Won award"
            )
        ),
        education = Education(degree = "BSc", field = "CS", institution = "University")
    )

    @Test
    fun experienceTooltipReturnsCompanyAndPeriod() {
        val tooltip = getTooltipForReference(
            CVReference(id = "takeaway", type = "Experience", label = "Takeaway"),
            testCVData
        )
        assertEquals("Senior Developer at Takeaway (2020-2023)", tooltip)
    }

    @Test
    fun skillTooltipReturnsSkillList() {
        val tooltip = getTooltipForReference(
            CVReference(id = "android", type = "Skill", label = "Android"),
            testCVData
        )
        assertEquals("Android: Kotlin, Java", tooltip)
    }

    @Test
    fun unknownReferenceReturnsNull() {
        val tooltip = getTooltipForReference(
            CVReference(id = "unknown", type = "Unknown", label = "X"),
            testCVData
        )
        assertNull(tooltip)
    }

    @Test
    fun projectTooltipReturnsNameAndDescription() {
        val tooltip = getTooltipForReference(
            CVReference(id = "cv-agent", type = "Project", label = "CV Agent"),
            testCVData
        )
        assertEquals("CV Agent: Portfolio app", tooltip)
    }

    @Test
    fun achievementTooltipReturnsTitleAndYear() {
        val tooltip = getTooltipForReference(
            CVReference(id = "award1", type = "Achievement", label = "Best App"),
            testCVData
        )
        assertEquals("Best App (2022)", tooltip)
    }

    @Test
    fun nullCvDataReturnsNull() {
        val tooltip = getTooltipForReference(
            CVReference(id = "any", type = "Experience", label = "Test"),
            null
        )
        assertNull(tooltip)
    }

    @Test
    fun nonExistentIdReturnsNull() {
        val tooltip = getTooltipForReference(
            CVReference(id = "nonexistent", type = "Experience", label = "X"),
            testCVData
        )
        assertNull(tooltip)
    }

    @Test
    fun summaryTooltipReturnsTruncatedText() {
        val longSummary = "A".repeat(150)
        val cvDataWithLongSummary = testCVData.copy(summary = longSummary)
        val tooltip = getTooltipForReference(
            CVReference(id = "summary", type = "Summary", label = ""),
            cvDataWithLongSummary
        )
        assertEquals("A".repeat(100) + "...", tooltip)
    }
}
