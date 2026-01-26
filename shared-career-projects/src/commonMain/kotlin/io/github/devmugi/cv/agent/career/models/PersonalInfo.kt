package io.github.devmugi.cv.agent.career.models

import kotlinx.serialization.Serializable

@Serializable
data class PersonalInfo(
    val name: String,
    val title: String,
    val location: String,
    val email: String,
    val linkedin: String,
    val github: String,
    val portfolio: String,
    val summary: String,
    val skills: List<SkillCategory>,
    val agentClarifications: String = ""
)

@Serializable
data class SkillCategory(
    val category: String,
    val items: List<String>
)
