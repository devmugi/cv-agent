package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.domain.models.CVData

class SystemPromptBuilder {

    fun build(cvData: CVData): String = buildString {
        appendLine("""
You are an AI assistant for Denys Honcharenko's portfolio. Answer questions about Denys in third person. Be helpful, professional, and concise.

When mentioning specific items from Denys's background, use this format:
[Type: ID] where Type is one of: Experience, Project, Skill, Achievement, Education

Examples: [Experience: experience.geosatis], [Project: project.mtg-deckbuilder]

---
        """.trimIndent())

        appendLine()
        appendLine("PERSONAL INFO:")
        appendLine("Name: ${cvData.personalInfo.name}")
        appendLine("Location: ${cvData.personalInfo.location}")
        appendLine("Email: ${cvData.personalInfo.email}")
        appendLine("LinkedIn: ${cvData.personalInfo.linkedin}")
        appendLine("GitHub: ${cvData.personalInfo.github}")
        appendLine()
        appendLine("Summary: ${cvData.summary}")

        appendLine()
        appendLine("SKILLS:")
        cvData.skills.forEach { skill ->
            appendLine("- ${skill.category} (ID: ${skill.id}): ${skill.skills.joinToString(", ")}")
        }

        appendLine()
        appendLine("WORK EXPERIENCE:")
        cvData.experience.forEach { exp ->
            appendLine("- ${exp.company} (ID: ${exp.id}): ${exp.title}, ${exp.period}")
            appendLine("  ${exp.description}")
            exp.highlights.forEach { highlight ->
                appendLine("  * $highlight")
            }
        }

        appendLine()
        appendLine("PROJECTS:")
        cvData.projects.forEach { project ->
            appendLine("- ${project.name} (ID: ${project.id}): ${project.type}")
            appendLine("  ${project.description}")
        }

        appendLine()
        appendLine("ACHIEVEMENTS:")
        cvData.achievements.forEach { achievement ->
            appendLine("- ${achievement.title} (ID: ${achievement.id}): ${achievement.year}")
            appendLine("  ${achievement.description}")
        }

        appendLine()
        appendLine("EDUCATION:")
        appendLine("- ${cvData.education.degree} in ${cvData.education.field}")
        appendLine("  ${cvData.education.institution}")
    }
}
