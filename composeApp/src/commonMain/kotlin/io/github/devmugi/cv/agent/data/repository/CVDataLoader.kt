package io.github.devmugi.cv.agent.data.repository

import io.github.devmugi.cv.agent.data.models.CVData
import kotlinx.serialization.json.Json

class CVDataLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun load(jsonString: String): CVData {
        return json.decodeFromString<CVData>(jsonString)
    }
}
