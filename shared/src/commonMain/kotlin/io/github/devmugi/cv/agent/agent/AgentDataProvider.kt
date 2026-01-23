package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.PersonalInfo

data class ProjectIndexEntry(
    val id: String,
    val name: String,
    val role: String,
    val period: String,
    val tagline: String
)

class AgentDataProvider(
    val personalInfo: PersonalInfo,
    private val allProjects: List<CareerProject>,
    private val featuredProjectIds: List<String>
) {
    companion object {
        val FEATURED_PROJECT_IDS = listOf(
            "geosatis",
            "mcdonalds",
            "adidas-gmr",
            "food-network-kitchen",
            "android-school"
        )
    }

    fun getProjectIndex(): List<ProjectIndexEntry> {
        return allProjects.map { project ->
            ProjectIndexEntry(
                id = project.id,
                name = project.name,
                role = project.overview?.role ?: "Engineer",
                period = project.overview?.period?.displayText ?: "",
                tagline = project.tagline ?: project.description?.short ?: ""
            )
        }
    }

    fun getCuratedDetails(projectId: String): String? {
        if (projectId !in featuredProjectIds) return null

        val project = allProjects.find { it.id == projectId } ?: return null

        return buildString {
            // Overview
            project.overview?.let { overview ->
                overview.company?.let { appendLine("Company: $it") }
                overview.client?.let { appendLine("Client: $it") }
                overview.product?.let { appendLine("Product: $it") }
                overview.role?.let { appendLine("Role: $it") }
                overview.period?.displayText?.let { appendLine("Period: $it") }
            }

            // Description
            appendLine()
            project.description?.short?.let { appendLine(it) }
            project.description?.full?.let { appendLine(it) }

            // Technologies
            project.technologies?.primary?.let { techs ->
                if (techs.isNotEmpty()) {
                    appendLine()
                    append("Technologies: ")
                    appendLine(techs.mapNotNull { it.name }.joinToString(", "))
                }
            }

            // Challenge
            project.challenge?.let { challenge ->
                appendLine()
                challenge.context?.let { appendLine("Challenge: $it") }
                challenge.response?.let { appendLine("Response: $it") }
            }
        }
    }

    fun getFeaturedProjects(): List<CareerProject> {
        return allProjects.filter { it.id in featuredProjectIds }
    }
}
