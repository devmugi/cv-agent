package io.github.devmugi.cv.agent.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CVReference(
    val id: String,
    val type: String,
    val label: String
)
