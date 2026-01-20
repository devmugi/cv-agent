package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.models.CVReference

fun getTooltipForReference(reference: CVReference, cvData: CVData?): String? {
    if (cvData == null) return null

    return when (reference.type.lowercase()) {
        "experience" -> {
            cvData.experience.find { it.id == reference.id }?.let { exp ->
                "${exp.title} at ${exp.company} (${exp.period})"
            }
        }
        "skill" -> {
            cvData.skills.find { it.id == reference.id }?.let { skill ->
                "${skill.category}: ${skill.skills.joinToString(", ")}"
            }
        }
        "project" -> {
            cvData.projects.find { it.id == reference.id }?.let { project ->
                "${project.name}: ${project.description}"
            }
        }
        "achievement" -> {
            cvData.achievements.find { it.id == reference.id }?.let { achievement ->
                "${achievement.title} (${achievement.year})"
            }
        }
        "summary" -> cvData.summary.take(100) + if (cvData.summary.length > 100) "..." else ""
        else -> null
    }
}
