package io.github.devmugi.cv.agent.agent

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class SystemPromptResult(
    val prompt: String,
    val version: String,
    val variant: String
)

class SystemPromptBuilder {

    fun build(dataProvider: AgentDataProvider): String =
        buildWithMetadata(dataProvider).prompt

    fun buildWithMetadata(dataProvider: AgentDataProvider): SystemPromptResult {
        val variant = dataProvider.contextMode.name
        val prompt = buildPromptString(dataProvider)
        return SystemPromptResult(
            prompt = prompt,
            version = PROMPT_VERSION,
            variant = variant
        )
    }

    private fun buildPromptString(dataProvider: AgentDataProvider): String = buildString {
        appendLine(getInstructions(dataProvider.contextMode))
        appendLine()
        appendPersonalInfo(dataProvider)
        appendLine()
        appendSkills(dataProvider)
        appendLine()
        appendProjectIndex(dataProvider)
        appendLine()
        appendFeaturedProjects(dataProvider)
        appendLine()
        appendLine(SUGGESTION_INSTRUCTIONS)
    }

    private fun getInstructions(mode: ProjectContextMode): String = when (mode) {
        ProjectContextMode.CURATED -> INSTRUCTIONS_CURATED
        ProjectContextMode.ALL_PROJECTS -> INSTRUCTIONS_ALL_PROJECTS
        ProjectContextMode.PERSONAL_INFO_ONLY -> INSTRUCTIONS_PERSONAL_INFO_ONLY
        ProjectContextMode.MCDONALDS_JSON_FULL -> INSTRUCTIONS_JSON_PROJECTS
        ProjectContextMode.ALL_PROJECTS_JSON_FULL -> INSTRUCTIONS_JSON_PROJECTS
    }

    private fun StringBuilder.appendPersonalInfo(dataProvider: AgentDataProvider) {
        val info = dataProvider.personalInfo
        appendLine("# PERSONAL INFO")
        appendLine("Name: ${info.name}")
        appendLine("Title: ${info.title}")
        appendLine("Location: ${info.location}")
        appendLine("Email: ${info.email}")
        appendLine("LinkedIn: ${info.linkedin}")
        appendLine("GitHub: ${info.github}")
        appendLine()
        appendLine("Summary: ${info.summary}")
    }

    private fun StringBuilder.appendSkills(dataProvider: AgentDataProvider) {
        appendLine("# SKILLS")
        dataProvider.personalInfo.skills.forEach { skill ->
            appendLine("- ${skill.category}: ${skill.items.joinToString(", ")}")
        }
    }

    private fun StringBuilder.appendProjectIndex(dataProvider: AgentDataProvider) {
        appendLine("# PROJECT INDEX")
        appendLine("All projects Denys has worked on:")
        dataProvider.getProjectIndex().forEach { entry ->
            appendLine("- ${entry.id}: \"${entry.name}\" | ${entry.role} | ${entry.period} | ${entry.tagline}")
        }
    }

    private fun StringBuilder.appendFeaturedProjects(dataProvider: AgentDataProvider) {
        if (!dataProvider.shouldIncludeProjectDetails()) {
            appendLine("# PROJECT DETAILS")
            appendLine("(No detailed project information included in this mode)")
            return
        }

        val header = when (dataProvider.contextMode) {
            ProjectContextMode.CURATED -> "# FEATURED PROJECTS (JSON FORMAT)"
            ProjectContextMode.ALL_PROJECTS -> "# ALL PROJECTS (JSON FORMAT)"
            ProjectContextMode.MCDONALDS_JSON_FULL -> "# McDONALD'S PROJECT (JSON FORMAT)"
            ProjectContextMode.ALL_PROJECTS_JSON_FULL -> "# ALL PROJECTS (JSON FORMAT)"
            ProjectContextMode.PERSONAL_INFO_ONLY -> "# PROJECT DETAILS"
        }
        appendLine(header)
        dataProvider.getProjectsForMode().forEach { project ->
            appendLine()
            appendLine("## ${project.name} (${project.id})")
            appendLine("```json")
            appendLine(prettyJson.encodeToString(project))
            appendLine("```")
        }
    }

    companion object {
        const val PROMPT_VERSION = "1.0.0"

        private val prettyJson = Json { prettyPrint = true }

        private val INSTRUCTIONS_CURATED = """
            You are an AI assistant for Denys Honcharenko's portfolio. Answer questions about Denys in third person. Be helpful, professional, and concise.

            You have access to:
            - Personal information and skills
            - A project index with all projects (id, name, role, period, tagline)
            - Full project data in JSON format for 5 featured projects

            For non-featured projects, use the project index information.
            Use the JSON data to answer detailed questions about featured projects.
        """.trimIndent()

        private val INSTRUCTIONS_ALL_PROJECTS = """
            You are an AI assistant for Denys Honcharenko's portfolio. Answer questions about Denys in third person. Be helpful, professional, and concise.

            You have access to:
            - Personal information and skills
            - A project index with all projects (id, name, role, period, tagline)
            - Full project data in JSON format for ALL projects

            Use the JSON data to answer detailed questions about projects.
        """.trimIndent()

        private val INSTRUCTIONS_PERSONAL_INFO_ONLY = """
            You are an AI assistant for Denys Honcharenko's portfolio. Answer questions about Denys in third person. Be helpful, professional, and concise.

            You have access to:
            - Personal information and skills
            - A project index with all projects (id, name, role, period, tagline)

            Note: Detailed project information is not available. Use the project index for basic project info.
        """.trimIndent()

        private val INSTRUCTIONS_JSON_PROJECTS = """
            You are an AI assistant for Denys Honcharenko's portfolio. Answer questions about Denys in third person. Be helpful, professional, and concise.

            You have access to:
            - Personal information and skills
            - A project index with all projects (id, name, role, period, tagline)
            - Full project data in JSON format (contains all fields from the original data)

            Use the JSON data to answer detailed questions about projects.
        """.trimIndent()

        private val SUGGESTION_INSTRUCTIONS = """
            # RESPONSE FORMAT

            End EVERY response with a JSON block suggesting 0-3 related projects the user might want to explore:

            ```json
            {"suggestions": ["project-id-1", "project-id-2"]}
            ```

            Rules:
            - Only suggest projects relevant to the question
            - Use exact project IDs from the project index
            - If no projects are relevant, use: {"suggestions": []}
            - Always include this JSON block, even if empty
        """.trimIndent()
    }
}
