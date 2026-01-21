package io.github.devmugi.cv.agent.data.repository

import io.github.devmugi.cv.agent.domain.models.Achievement
import io.github.devmugi.cv.agent.domain.models.CVData
import io.github.devmugi.cv.agent.domain.models.CVReference
import io.github.devmugi.cv.agent.domain.models.Project
import io.github.devmugi.cv.agent.domain.models.SkillCategory
import io.github.devmugi.cv.agent.domain.models.WorkExperience

open class CVRepository(
    private val loader: CVDataLoader = CVDataLoader()
) {
    private var cachedData: CVData? = null

    fun getCVData(jsonString: String): CVData {
        return cachedData ?: loader.load(jsonString).also {
            cachedData = it
        }
    }

    fun findExperienceById(id: String): WorkExperience? {
        return cachedData?.experience?.find { it.id == id }
    }

    fun findProjectById(id: String): Project? {
        return cachedData?.projects?.find { it.id == id }
    }

    fun findSkillCategoryById(id: String): SkillCategory? {
        return cachedData?.skills?.find { it.id == id }
    }

    fun findAchievementById(id: String): Achievement? {
        return cachedData?.achievements?.find { it.id == id }
    }

    open fun resolveReference(id: String): CVReference? {
        val type = id.substringBefore(".")
        return when (type) {
            "experience" -> findExperienceById(id)?.let {
                CVReference(id, "experience", it.company)
            }
            "project" -> findProjectById(id)?.let {
                CVReference(id, "project", it.name)
            }
            "skills" -> findSkillCategoryById(id)?.let {
                CVReference(id, "skill", it.category)
            }
            "achievement" -> findAchievementById(id)?.let {
                CVReference(id, "achievement", it.title)
            }
            else -> null
        }
    }
}
