package io.github.devmugi.cv.agent.eval.report

import io.github.devmugi.cv.agent.eval.config.EvalConfig
import kotlinx.serialization.Serializable

/**
 * Result of a single question evaluation.
 */
@Serializable
data class QuestionResult(
    val questionId: String,
    val questionText: String,
    val category: String,
    val response: String,
    val latencyMs: Long,
    val ttftMs: Long? = null,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val cachedTokens: Int = 0,
    val cacheHitRate: Double = 0.0, // cachedTokens / promptTokens
    val suggestions: List<String> = emptyList(),
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * Result of a multi-turn conversation evaluation.
 */
@Serializable
data class ConversationResult(
    val conversationId: String,
    val description: String,
    val turns: List<TurnResult>,
    val totalLatencyMs: Long,
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * Result of a single turn in a conversation.
 */
@Serializable
data class TurnResult(
    val turnNumber: Int,
    val userMessage: String,
    val response: String,
    val latencyMs: Long,
    val ttftMs: Long? = null,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val cachedTokens: Int = 0,
    val cacheHitRate: Double = 0.0,
    val suggestions: List<String> = emptyList()
)

/**
 * Summary statistics for an evaluation run.
 */
@Serializable
data class RunSummary(
    val totalQuestions: Int,
    val totalConversations: Int,
    val successCount: Int,
    val errorCount: Int,
    val avgLatencyMs: Double,
    val avgTtftMs: Double? = null,
    val totalPromptTokens: Int,
    val totalCompletionTokens: Int,
    val totalTokens: Int,
    val totalCachedTokens: Int = 0,
    val avgCacheHitRate: Double = 0.0,
    val p50LatencyMs: Long,
    val p95LatencyMs: Long
)

/**
 * Complete evaluation report for a single run.
 */
@Serializable
data class EvalReport(
    val runId: String,
    val timestamp: String,
    val config: EvalConfig,
    val summary: RunSummary,
    val questionResults: List<QuestionResult>,
    val conversationResults: List<ConversationResult>
)

/**
 * Comparison between two evaluation runs.
 */
@Serializable
data class ComparisonResult(
    val baselineRunId: String,
    val variantRunId: String,
    val baselineConfig: EvalConfig,
    val variantConfig: EvalConfig,
    val timestamp: String,
    val summaryComparison: SummaryComparison,
    val questionComparisons: List<QuestionComparison>
)

/**
 * Summary-level comparison metrics.
 */
@Serializable
data class SummaryComparison(
    val latencyDeltaPercent: Double,
    val ttftDeltaPercent: Double?,
    val promptTokensDeltaPercent: Double,
    val completionTokensDeltaPercent: Double,
    val successRateDelta: Double,
    val baselineSummary: RunSummary,
    val variantSummary: RunSummary
)

/**
 * Per-question comparison between baseline and variant.
 */
@Serializable
data class QuestionComparison(
    val questionId: String,
    val questionText: String,
    val category: String,
    val baselineLatencyMs: Long,
    val variantLatencyMs: Long,
    val latencyDeltaPercent: Double,
    val baselineTokens: Int,
    val variantTokens: Int,
    val tokensDeltaPercent: Double,
    val baselineSuggestions: List<String>,
    val variantSuggestions: List<String>,
    val bothSucceeded: Boolean
)
