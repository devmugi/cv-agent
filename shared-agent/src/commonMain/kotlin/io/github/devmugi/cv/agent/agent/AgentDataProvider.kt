package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.PersonalInfo

/**
 * Determines how much project detail is included in the LLM system prompt.
 */
enum class ProjectContextMode {
    /** Include full details for 5 featured projects only (lower token cost) */
    CURATED,

    /** Include full details for ALL projects (better coverage, higher token cost) */
    ALL_PROJECTS,

    /** Personal info + skills + project index only (no project details) */
    PERSONAL_INFO_ONLY,

    /** McDonald's project as raw JSON (for deep evaluation) */
    MCDONALDS_JSON_FULL,

    /** All projects as raw JSON (maximum context) */
    ALL_PROJECTS_JSON_FULL
}

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
    private val featuredProjectIds: List<String>,
    val contextMode: ProjectContextMode = ProjectContextMode.CURATED
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

    /**
     * Whether to include detailed project information in the prompt.
     */
    fun shouldIncludeProjectDetails(): Boolean = when (contextMode) {
        ProjectContextMode.PERSONAL_INFO_ONLY -> false
        else -> true
    }

    /**
     * Whether the mode uses raw JSON format for projects.
     */
    fun isJsonMode(): Boolean = contextMode in listOf(
        ProjectContextMode.MCDONALDS_JSON_FULL,
        ProjectContextMode.ALL_PROJECTS_JSON_FULL
    )

    /**
     * Get projects to include based on current mode.
     */
    fun getProjectsForMode(): List<CareerProject> = when (contextMode) {
        ProjectContextMode.CURATED -> allProjects.filter { it.id in featuredProjectIds }
        ProjectContextMode.MCDONALDS_JSON_FULL -> allProjects.filter { it.id == "mcdonalds" }
        ProjectContextMode.PERSONAL_INFO_ONLY -> emptyList()
        ProjectContextMode.ALL_PROJECTS, ProjectContextMode.ALL_PROJECTS_JSON_FULL -> allProjects
    }

    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    fun getCuratedDetails(projectId: String): String? {
        val shouldInclude = when (contextMode) {
            ProjectContextMode.CURATED -> projectId in featuredProjectIds
            ProjectContextMode.ALL_PROJECTS -> true
            ProjectContextMode.PERSONAL_INFO_ONLY -> false
            ProjectContextMode.MCDONALDS_JSON_FULL -> projectId == "mcdonalds"
            ProjectContextMode.ALL_PROJECTS_JSON_FULL -> true
        }
        if (!shouldInclude) return null
        val project = allProjects.find { it.id == projectId } ?: return null
        return formatProjectDetails(project)
    }

    private fun formatProjectDetails(project: CareerProject): String = buildString {
        appendOverview(project)
        appendDescription(project)
        appendTechnologies(project)
        appendChallenge(project)
    }

    private fun StringBuilder.appendOverview(project: CareerProject) {
        project.overview?.let { overview ->
            overview.company?.let { appendLine("Company: $it") }
            overview.client?.let { appendLine("Client: $it") }
            overview.product?.let { appendLine("Product: $it") }
            overview.role?.let { appendLine("Role: $it") }
            overview.period?.displayText?.let { appendLine("Period: $it") }
        }
    }

    private fun StringBuilder.appendDescription(project: CareerProject) {
        appendLine()
        project.description?.short?.let { appendLine(it) }
        project.description?.full?.let { appendLine(it) }
    }

    private fun StringBuilder.appendTechnologies(project: CareerProject) {
        project.technologies?.primary?.let { techs ->
            if (techs.isNotEmpty()) {
                appendLine()
                append("Technologies: ")
                appendLine(techs.mapNotNull { it.name }.joinToString(", "))
            }
        }
    }

    private fun StringBuilder.appendChallenge(project: CareerProject) {
        project.challenge?.let { challenge ->
            appendLine()
            challenge.context?.let { appendLine("Challenge: $it") }
            challenge.response?.let { appendLine("Response: $it") }
        }
    }

    fun getFeaturedProjects(): List<CareerProject> = getProjectsForMode()

    fun getAllProjects(): List<CareerProject> = allProjects
}
