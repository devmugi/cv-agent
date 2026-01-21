package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.domain.models.CVData
import io.github.devmugi.cv.agent.domain.models.CVReference
import io.github.devmugi.cv.agent.domain.models.ReferenceType

/**
 * Helper function to get tooltip content for a CV reference.
 */
fun getTooltipForReference(reference: CVReference, cvData: CVData?): String {
    if (cvData == null) return reference.displayLabel

    return when (reference.type) {
        ReferenceType.WORK_EXPERIENCE -> {
            cvData.workExperience.find { it.company == reference.id }?.let { exp ->
                "${exp.title} at ${exp.company}\n${exp.period}"
            } ?: reference.displayLabel
        }
        ReferenceType.PROJECT -> {
            cvData.projects.find { it.name == reference.id }?.let { proj ->
                "${proj.name}\n${proj.description}"
            } ?: reference.displayLabel
        }
        ReferenceType.SKILL -> reference.displayLabel
        ReferenceType.ACHIEVEMENT -> {
            cvData.achievements.find { it.title == reference.id }?.let { ach ->
                "${ach.title}\n${ach.description}"
            } ?: reference.displayLabel
        }
        ReferenceType.EDUCATION -> {
            cvData.education.find { it.institution == reference.id }?.let { edu ->
                "${edu.degree} - ${edu.institution}\n${edu.period}"
            } ?: reference.displayLabel
        }
    }
}
