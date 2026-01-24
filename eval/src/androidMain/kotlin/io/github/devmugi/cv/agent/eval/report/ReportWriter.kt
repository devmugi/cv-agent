package io.github.devmugi.cv.agent.eval.report

import co.touchlab.kermit.Logger
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Writes evaluation reports to JSON and Markdown files.
 */
class ReportWriter(private val reportsDir: File) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    init {
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
            Logger.i(TAG) { "Created reports directory: ${reportsDir.absolutePath}" }
        }
    }

    /**
     * Write a full evaluation report to JSON file.
     * @return The created JSON file
     */
    fun writeJsonReport(report: EvalReport): File {
        val filename = buildFilename(report, "json")
        val file = File(reportsDir, filename)

        val jsonContent = json.encodeToString(report)
        file.writeText(jsonContent)

        Logger.i(TAG) { "Wrote JSON report: ${file.absolutePath}" }
        return file
    }

    /**
     * Write a human-readable Markdown summary.
     * @return The created Markdown file
     */
    fun writeMarkdownReport(report: EvalReport): File {
        val filename = buildFilename(report, "md")
        val file = File(reportsDir, filename)

        val markdown = buildMarkdownReport(report)
        file.writeText(markdown)

        Logger.i(TAG) { "Wrote Markdown report: ${file.absolutePath}" }
        return file
    }

    /**
     * Write both JSON and Markdown reports.
     * @return Pair of (JSON file, Markdown file)
     */
    fun writeReports(report: EvalReport): Pair<File, File> {
        val jsonFile = writeJsonReport(report)
        val mdFile = writeMarkdownReport(report)
        return Pair(jsonFile, mdFile)
    }

    private fun buildFilename(report: EvalReport, extension: String): String {
        val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
        val variant = report.config.promptVariant.name
        val format = report.config.dataFormat.name
        return "${date}_${report.runId}_${variant}_$format.$extension"
    }

    @Suppress("LongMethod")
    private fun buildMarkdownReport(report: EvalReport): String = buildString {
        appendLine("# Eval Run: ${report.runId}")
        appendLine()
        appendLine("**Timestamp:** ${report.timestamp}")
        appendLine("**Config:** ${report.config.promptVariant} | ${report.config.dataFormat} | ${report.config.projectMode}")
        appendLine("**Model:** ${report.config.model}")
        appendLine()

        // Summary section
        appendLine("## Summary")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| Questions | ${report.summary.totalQuestions} |")
        appendLine("| Conversations | ${report.summary.totalConversations} |")
        appendLine("| Success Rate | ${calculateSuccessRate(report.summary)}% |")
        appendLine("| Avg Latency | ${report.summary.avgLatencyMs.toLong()}ms |")
        report.summary.avgTtftMs?.let {
            appendLine("| Avg TTFT | ${it.toLong()}ms |")
        }
        appendLine("| P50 Latency | ${report.summary.p50LatencyMs}ms |")
        appendLine("| P95 Latency | ${report.summary.p95LatencyMs}ms |")
        appendLine("| Total Prompt Tokens | ${formatNumber(report.summary.totalPromptTokens)} |")
        appendLine("| Total Completion Tokens | ${formatNumber(report.summary.totalCompletionTokens)} |")
        appendLine("| Total Tokens | ${formatNumber(report.summary.totalTokens)} |")
        appendLine()

        // Question results
        if (report.questionResults.isNotEmpty()) {
            appendLine("## Question Results")
            appendLine()
            appendLine("| ID | Category | Latency | TTFT | Tokens | Suggestions | Status |")
            appendLine("|----|----------|---------|------|--------|-------------|--------|")
            report.questionResults.forEach { q ->
                val status = if (q.success) "OK" else "ERR"
                val ttft = q.ttftMs?.toString() ?: "-"
                val suggestions = if (q.suggestions.isEmpty()) "-" else q.suggestions.joinToString(", ")
                appendLine("| ${q.questionId} | ${q.category} | ${q.latencyMs}ms | ${ttft}ms | ${q.totalTokens} | $suggestions | $status |")
            }
            appendLine()
        }

        // Conversation results
        if (report.conversationResults.isNotEmpty()) {
            appendLine("## Conversation Results")
            appendLine()
            report.conversationResults.forEach { conv ->
                val status = if (conv.success) "OK" else "ERR"
                appendLine("### ${conv.conversationId}: ${conv.description} ($status)")
                appendLine()
                appendLine("| Turn | Latency | TTFT | Tokens | Suggestions |")
                appendLine("|------|---------|------|--------|-------------|")
                conv.turns.forEach { turn ->
                    val ttft = turn.ttftMs?.toString() ?: "-"
                    val suggestions = if (turn.suggestions.isEmpty()) "-" else turn.suggestions.joinToString(", ")
                    appendLine("| ${turn.turnNumber} | ${turn.latencyMs}ms | ${ttft}ms | ${turn.totalTokens} | $suggestions |")
                }
                appendLine()
                appendLine("**Total Latency:** ${conv.totalLatencyMs}ms")
                appendLine()
            }
        }

        // Detailed responses (collapsed for readability)
        appendLine("## Detailed Responses")
        appendLine()
        appendLine("<details>")
        appendLine("<summary>Click to expand question responses</summary>")
        appendLine()
        report.questionResults.forEach { q ->
            appendLine("### ${q.questionId}: ${q.questionText}")
            appendLine()
            appendLine("**Category:** ${q.category}")
            appendLine()
            appendLine("```")
            appendLine(q.response.take(MAX_RESPONSE_LENGTH))
            if (q.response.length > MAX_RESPONSE_LENGTH) {
                appendLine("... (truncated)")
            }
            appendLine("```")
            appendLine()
        }
        appendLine("</details>")
    }

    private fun calculateSuccessRate(summary: RunSummary): Int {
        val total = summary.totalQuestions + summary.totalConversations
        return if (total > 0) {
            ((summary.successCount.toDouble() / total) * 100).toInt()
        } else {
            100
        }
    }

    private fun formatNumber(n: Int): String {
        return String.format("%,d", n)
    }

    companion object {
        private const val TAG = "ReportWriter"
        private const val MAX_RESPONSE_LENGTH = 1000
    }
}
