package io.github.devmugi.cv.agent.eval

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.github.devmugi.cv.agent.eval.report.ReportComparator
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertNotNull

/**
 * Test to compare two evaluation runs.
 *
 * Run with:
 * ```
 * ./gradlew :eval:compare -PbaselineRun=abc123 -PvariantRun=def456
 * ```
 */
@Suppress("FunctionNaming")
class EvalCompareTest {

    @Before
    fun setup() {
        Logger.setLogWriters(object : LogWriter() {
            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                println("[$severity][$tag] $message")
                throwable?.printStackTrace()
            }
        })
        Logger.setMinSeverity(Severity.Debug)
    }

    @Test
    fun `compare two evaluation runs`() {
        val baselineRunId = System.getenv("BASELINE_RUN_ID")
        val variantRunId = System.getenv("VARIANT_RUN_ID")
        val reportsDir = System.getenv("EVAL_REPORT_DIR") ?: "eval/reports"

        assumeTrue(
            "BASELINE_RUN_ID not set - usage: ./gradlew :eval:compare -PbaselineRun=ID1 -PvariantRun=ID2",
            !baselineRunId.isNullOrEmpty()
        )
        assumeTrue(
            "VARIANT_RUN_ID not set - usage: ./gradlew :eval:compare -PbaselineRun=ID1 -PvariantRun=ID2",
            !variantRunId.isNullOrEmpty()
        )

        println("=".repeat(60))
        println("Comparing Evaluation Runs")
        println("=".repeat(60))
        println("Baseline: $baselineRunId")
        println("Variant: $variantRunId")
        println("Reports Dir: $reportsDir")
        println("=".repeat(60))

        val comparator = ReportComparator(File(reportsDir))

        val baseline = comparator.loadReport(baselineRunId)
        assertNotNull(baseline, "Could not load baseline report for run: $baselineRunId")

        val variant = comparator.loadReport(variantRunId)
        assertNotNull(variant, "Could not load variant report for run: $variantRunId")

        println("\nLoaded reports:")
        println("  Baseline: ${baseline.config.promptVariant} | ${baseline.summary.totalQuestions} questions")
        println("  Variant: ${variant.config.promptVariant} | ${variant.summary.totalQuestions} questions")

        val comparison = comparator.compare(baseline, variant)
        val (jsonFile, mdFile) = comparator.writeComparison(comparison)

        println()
        println("=".repeat(60))
        println("COMPARISON RESULTS")
        println("=".repeat(60))
        println()
        println("Latency: ${formatDelta(comparison.summaryComparison.latencyDeltaPercent)}")
        comparison.summaryComparison.ttftDeltaPercent?.let {
            println("TTFT: ${formatDelta(it)}")
        }
        println("Prompt Tokens: ${formatDelta(comparison.summaryComparison.promptTokensDeltaPercent)}")
        println("Completion Tokens: ${formatDelta(comparison.summaryComparison.completionTokensDeltaPercent)}")
        println("Success Rate Delta: ${formatDelta(comparison.summaryComparison.successRateDelta)}")
        println()
        println("Reports:")
        println("  JSON: ${jsonFile.absolutePath}")
        println("  Markdown: ${mdFile.absolutePath}")
        println("=".repeat(60))
    }

    private fun formatDelta(value: Double): String {
        val sign = if (value > 0) "+" else ""
        return "$sign${String.format("%.1f", value)}%"
    }
}
