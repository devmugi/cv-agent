package io.github.devmugi.arize.tracing.models

data class PromptTemplate(
    val template: String,
    val version: String? = null,
    val variables: Map<String, Any>? = null
)
