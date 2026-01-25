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
    val contextMode: ProjectContextMode = ProjectContextMode.ALL_PROJECTS
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
     * Get projects to include based on current mode.
     */
    fun getProjectsForMode(): List<CareerProject> = when (contextMode) {
        ProjectContextMode.CURATED -> allProjects.filter { it.id in featuredProjectIds }
        ProjectContextMode.MCDONALDS_JSON_FULL -> allProjects.filter { it.id == "mcdonalds" }
        ProjectContextMode.PERSONAL_INFO_ONLY -> emptyList()
        ProjectContextMode.ALL_PROJECTS, ProjectContextMode.ALL_PROJECTS_JSON_FULL -> allProjects
    }

    fun getFeaturedProjects(): List<CareerProject> = getProjectsForMode()

    fun getAllProjects(): List<CareerProject> = allProjects
}
