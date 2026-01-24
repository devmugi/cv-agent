package io.github.devmugi.cv.agent.eval.report

import co.touchlab.kermit.Logger
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Compares two evaluation reports and generates comparison output.
 */
class ReportComparator(private val reportsDir: File) {

    companion object {
        private const val TAG = "ReportComparator"
        private const val COMPARISONS_DIR = "comparisons"
    }

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private val comparisonsDir: File by lazy {
        File(reportsDir, COMPARISONS_DIR).also { it.mkdirs() }
    }

    /**
     * Load an evaluation report from a JSON file.
     */
    fun loadReport(runId: String): EvalReport? {
        val files = reportsDir.listFiles { f -> f.name.contains(runId) && f.extension == "json" }
        val file = files?.firstOrNull()

        return if (file != null && file.exists()) {
            try {
                json.decodeFromString<EvalReport>(file.readText())
            } catch (e: Exception) {
                Logger.e(TAG) { "Failed to load report $runId: ${e.message}" }
                null
            }
        } else {
            Logger.e(TAG) { "Report not found for runId: $runId" }
            null
        }
    }

    /**
     * Compare two evaluation reports.
     */
    fun compare(baseline: EvalReport, variant: EvalReport): ComparisonResult {
        val questionComparisons = buildQuestionComparisons(baseline, variant)
        val summaryComparison = buildSummaryComparison(baseline.summary, variant.summary)

        return ComparisonResult(
            baselineRunId = baseline.runId,
            variantRunId = variant.runId,
            baselineConfig = baseline.config,
            variantConfig = variant.config,
            timestamp = Instant.now().toString(),
            summaryComparison = summaryComparison,
            questionComparisons = questionComparisons
        )
    }

    /**
     * Write comparison result to JSON and Markdown files.
     */
    fun writeComparison(comparison: ComparisonResult): Pair<File, File> {
        val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
            .format(java.time.LocalDateTime.now())
        val baseVariant = comparison.baselineConfig.promptVariant.name.lowercase()
        val varVariant = comparison.variantConfig.promptVariant.name.lowercase()

        // Write JSON
        val jsonFilename = "${timestamp}_${baseVariant}_vs_$varVariant.json"
        val jsonFile = File(comparisonsDir, jsonFilename)
        jsonFile.writeText(json.encodeToString(comparison))
        Logger.i(TAG) { "Wrote comparison JSON: ${jsonFile.absolutePath}" }

        // Write Markdown
        val mdFilename = "${timestamp}_${baseVariant}_vs_$varVariant.md"
        val mdFile = File(comparisonsDir, mdFilename)
        mdFile.writeText(buildComparisonMarkdown(comparison))
        Logger.i(TAG) { "Wrote comparison Markdown: ${mdFile.absolutePath}" }

        return Pair(jsonFile, mdFile)
    }

    private fun buildSummaryComparison(baseline: RunSummary, variant: RunSummary): SummaryComparison {
        return SummaryComparison(
            latencyDeltaPercent = calculateDeltaPercent(baseline.avgLatencyMs, variant.avgLatencyMs),
            ttftDeltaPercent = if (baseline.avgTtftMs != null && variant.avgTtftMs != null) {
                calculateDeltaPercent(baseline.avgTtftMs, variant.avgTtftMs)
            } else null,
            promptTokensDeltaPercent = calculateDeltaPercent(
                baseline.totalPromptTokens.toDouble(),
                variant.totalPromptTokens.toDouble()
            ),
            completionTokensDeltaPercent = calculateDeltaPercent(
                baseline.totalCompletionTokens.toDouble(),
                variant.totalCompletionTokens.toDouble()
            ),
            successRateDelta = calculateSuccessRateDelta(baseline, variant),
            baselineSummary = baseline,
            variantSummary = variant
        )
    }

    private fun buildQuestionComparisons(
        baseline: EvalReport,
        variant: EvalReport
    ): List<QuestionComparison> {
        val variantMap = variant.questionResults.associateBy { it.questionId }

        return baseline.questionResults.mapNotNull { baseQ ->
            val varQ = variantMap[baseQ.questionId] ?: return@mapNotNull null

            QuestionComparison(
                questionId = baseQ.questionId,
                questionText = baseQ.questionText,
                category = baseQ.category,
                baselineLatencyMs = baseQ.latencyMs,
                variantLatencyMs = varQ.latencyMs,
                latencyDeltaPercent = calculateDeltaPercent(
                    baseQ.latencyMs.toDouble(),
                    varQ.latencyMs.toDouble()
                ),
                baselineTokens = baseQ.totalTokens,
                variantTokens = varQ.totalTokens,
                tokensDeltaPercent = calculateDeltaPercent(
                    baseQ.totalTokens.toDouble(),
                    varQ.totalTokens.toDouble()
                ),
                baselineSuggestions = baseQ.suggestions,
                variantSuggestions = varQ.suggestions,
                bothSucceeded = baseQ.success && varQ.success
            )
        }
    }

    private fun calculateDeltaPercent(baseline: Double, variant: Double): Double {
        return if (baseline != 0.0) {
            ((variant - baseline) / baseline) * 100
        } else {
            0.0
        }
    }

    private fun calculateSuccessRateDelta(baseline: RunSummary, variant: RunSummary): Double {
        val baselineTotal = baseline.totalQuestions + baseline.totalConversations
        val variantTotal = variant.totalQuestions + variant.totalConversations

        val baselineRate = if (baselineTotal > 0) {
            baseline.successCount.toDouble() / baselineTotal * 100
        } else 100.0

        val variantRate = if (variantTotal > 0) {
            variant.successCount.toDouble() / variantTotal * 100
        } else 100.0

        return variantRate - baselineRate
    }

    @Suppress("LongMethod")
    private fun buildComparisonMarkdown(comparison: ComparisonResult): String = buildString {
        val baseVariant = comparison.baselineConfig.promptVariant.name
        val varVariant = comparison.variantConfig.promptVariant.name

        appendLine("# Comparison: $baseVariant vs $varVariant")
        appendLine()
        appendLine("**Generated:** ${comparison.timestamp}")
        appendLine()

        // Summary comparison table
        appendLine("## Summary Comparison")
        appendLine()
        appendLine("| Metric | $baseVariant | $varVariant | Delta |")
        appendLine("|--------|-------------|------------|-------|")

        val sc = comparison.summaryComparison
        appendLine("| Avg Latency | ${sc.baselineSummary.avgLatencyMs.toLong()}ms | ${sc.variantSummary.avgLatencyMs.toLong()}ms | ${formatDelta(sc.latencyDeltaPercent)} |")

        if (sc.ttftDeltaPercent != null) {
            appendLine("| Avg TTFT | ${sc.baselineSummary.avgTtftMs?.toLong()}ms | ${sc.variantSummary.avgTtftMs?.toLong()}ms | ${formatDelta(sc.ttftDeltaPercent)} |")
        }

        appendLine("| P50 Latency | ${sc.baselineSummary.p50LatencyMs}ms | ${sc.variantSummary.p50LatencyMs}ms | - |")
        appendLine("| P95 Latency | ${sc.baselineSummary.p95LatencyMs}ms | ${sc.variantSummary.p95LatencyMs}ms | - |")
        appendLine("| Prompt Tokens | ${formatNumber(sc.baselineSummary.totalPromptTokens)} | ${formatNumber(sc.variantSummary.totalPromptTokens)} | ${formatDelta(sc.promptTokensDeltaPercent)} |")
        appendLine("| Completion Tokens | ${formatNumber(sc.baselineSummary.totalCompletionTokens)} | ${formatNumber(sc.variantSummary.totalCompletionTokens)} | ${formatDelta(sc.completionTokensDeltaPercent)} |")
        appendLine("| Success Rate | ${calculateSuccessRate(sc.baselineSummary)}% | ${calculateSuccessRate(sc.variantSummary)}% | ${formatDelta(sc.successRateDelta, showSign = true)} |")
        appendLine()

        // Per-question comparison
        if (comparison.questionComparisons.isNotEmpty()) {
            appendLine("## Per-Question Comparison")
            appendLine()
            appendLine("| ID | Category | Base Latency | Var Latency | Delta | Base Tokens | Var Tokens | Delta |")
            appendLine("|----|----------|--------------|-------------|-------|-------------|------------|-------|")

            comparison.questionComparisons.forEach { qc ->
                val status = if (qc.bothSucceeded) "" else " :warning:"
                appendLine("| ${qc.questionId}$status | ${qc.category} | ${qc.baselineLatencyMs}ms | ${qc.variantLatencyMs}ms | ${formatDelta(qc.latencyDeltaPercent)} | ${qc.baselineTokens} | ${qc.variantTokens} | ${formatDelta(qc.tokensDeltaPercent)} |")
            }
            appendLine()
        }

        // Key insights
        appendLine("## Key Insights")
        appendLine()

        val latencyImproved = sc.latencyDeltaPercent < 0
        val tokensReduced = sc.promptTokensDeltaPercent < 0

        if (latencyImproved) {
            appendLine("- **Latency improved** by ${formatDelta(sc.latencyDeltaPercent)}")
        } else if (sc.latencyDeltaPercent > 0) {
            appendLine("- **Latency increased** by ${formatDelta(sc.latencyDeltaPercent)}")
        }

        if (tokensReduced) {
            appendLine("- **Token usage reduced** by ${formatDelta(sc.promptTokensDeltaPercent)}")
        } else if (sc.promptTokensDeltaPercent > 0) {
            appendLine("- **Token usage increased** by ${formatDelta(sc.promptTokensDeltaPercent)}")
        }

        if (sc.successRateDelta != 0.0) {
            appendLine("- **Success rate changed** by ${formatDelta(sc.successRateDelta, showSign = true)}")
        }
    }

    private fun formatDelta(value: Double, showSign: Boolean = false): String {
        val sign = when {
            value > 0 -> "+"
            value < 0 -> ""
            else -> ""
        }
        return if (showSign || value != 0.0) {
            "$sign${String.format("%.1f", value)}%"
        } else {
            "0%"
        }
    }

    private fun formatNumber(n: Int): String = String.format("%,d", n)

    private fun calculateSuccessRate(summary: RunSummary): Int {
        val total = summary.totalQuestions + summary.totalConversations
        return if (total > 0) {
            ((summary.successCount.toDouble() / total) * 100).toInt()
        } else {
            100
        }
    }
}
