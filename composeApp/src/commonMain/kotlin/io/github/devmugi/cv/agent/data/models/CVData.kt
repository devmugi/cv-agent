package io.github.devmugi.cv.agent.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CVData(
    val personalInfo: PersonalInfo,
    val summary: String,
    val skills: List<SkillCategory>,
    val experience: List<WorkExperience>,
    val projects: List<Project>,
    val achievements: List<Achievement>,
    val education: Education
)

@Serializable
data class PersonalInfo(
    val name: String,
    val location: String,
    val email: String,
    val phone: String,
    val linkedin: String,
    val github: String,
    val portfolio: String
)

@Serializable
data class SkillCategory(
    val id: String,
    val category: String,
    val level: String? = null,
    val skills: List<String>
)

@Serializable
data class WorkExperience(
    val id: String,
    val title: String,
    val company: String,
    val period: String,
    val description: String,
    val highlights: List<String>,
    val technologies: List<String>,
    val featured: Boolean = false
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val type: String,
    val description: String,
    val technologies: List<String>,
    val links: ProjectLinks? = null,
    val featured: Boolean = false
)

@Serializable
data class ProjectLinks(
    val demo: String? = null,
    val source: String? = null,
    val playStore: String? = null
)

@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val organization: String? = null,
    val year: String,
    val description: String
)

@Serializable
data class Education(
    val degree: String,
    val field: String,
    val institution: String
)
