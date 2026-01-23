package io.github.devmugi.cv.agent.career.data

import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.ProjectDataTimeline
import io.github.devmugi.cv.agent.career.models.toProjectTimelineData
import kotlinx.serialization.json.Json

class CareerProjectDataLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadCareerProject(jsonString: String): CareerProject {
        return json.decodeFromString<CareerProject>(jsonString)
    }

    fun loadProjectTimeline(jsonString: String): ProjectDataTimeline {
        return loadCareerProject(jsonString).toProjectTimelineData()
    }
}
