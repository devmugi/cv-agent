package io.github.devmugi.cv.agent.agent

class SystemPromptBuilder {

    fun build(dataProvider: AgentDataProvider): String = buildString {
        appendLine(INSTRUCTIONS)
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
        appendLine("# FEATURED PROJECTS (FULL DETAILS)")
        dataProvider.getFeaturedProjects().forEach { project ->
            appendLine()
            appendLine("## ${project.name} (${project.id})")
            dataProvider.getCuratedDetails(project.id)?.let { details ->
                appendLine(details)
            }
        }
    }

    companion object {
        private val INSTRUCTIONS = """
            You are an AI assistant for Denys Honcharenko's portfolio. Answer questions about Denys in third person. Be helpful, professional, and concise.

            You have access to:
            - Personal information and skills
            - A project index with all projects (id, name, role, period, tagline)
            - Full details for 5 featured projects

            For non-featured projects, use the project index information.
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
