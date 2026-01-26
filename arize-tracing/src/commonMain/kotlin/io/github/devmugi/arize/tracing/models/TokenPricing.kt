package io.github.devmugi.arize.tracing.models

data class TokenPricing(
    val promptPerMillion: Double,
    val completionPerMillion: Double
) {
    fun calculateCost(usage: TokenUsage): Cost {
        val promptCost = (usage.promptTokens / 1_000_000.0) * promptPerMillion
        val completionCost = (usage.completionTokens / 1_000_000.0) * completionPerMillion
        return Cost(
            promptCost = promptCost,
            completionCost = completionCost,
            totalCost = promptCost + completionCost
        )
    }
}

data class Cost(
    val promptCost: Double,
    val completionCost: Double,
    val totalCost: Double
)
