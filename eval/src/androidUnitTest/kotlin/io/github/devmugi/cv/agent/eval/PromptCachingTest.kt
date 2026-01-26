package io.github.devmugi.cv.agent.eval

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.github.devmugi.cv.agent.agent.ProjectContextMode
import io.github.devmugi.cv.agent.eval.config.EvalConfig
import io.github.devmugi.cv.agent.eval.config.PromptVariant
import io.github.devmugi.cv.agent.eval.config.QuestionSet
import io.github.devmugi.cv.agent.eval.runner.EvalRunner
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Evaluation test for Groq prompt caching.
 *
 * Tests caching effectiveness by running WELCOME questions twice:
 * - Cold run: First pass (no cache expected)
 * - Warm run: Second pass (cache hits expected)
 *
 * Supported caching models (per Groq docs):
 * - openai/gpt-oss-20b
 * - openai/gpt-oss-120b
 *
 * Run with:
 * ```
 * ./gradlew :eval:testAndroidUnitTest --tests "*PromptCachingTest*" --rerun-tasks
 * ```
 *
 * After running, check Phoenix at http://localhost:6006 for:
 * - cached_tokens in usage
 * - latency comparison between cold/warm runs
 */
@Suppress("FunctionNaming")
class PromptCachingTest {

    companion object {
        val CACHE_MODELS = listOf(
            "openai/gpt-oss-20b",
            "openai/gpt-oss-120b"
        )
        const val INTER_QUESTION_DELAY_MS = 3_000L
        const val INTER_RUN_DELAY_MS = 5_000L
        const val INTER_MODEL_DELAY_MS = 10_000L
    }

    @Before
    fun setup() {
        Logger.setLogWriters(object : LogWriter() {
            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                println("[$severity][$tag] $message")
                throwable?.printStackTrace()
            }
        })
        Logger.setMinSeverity(Severity.Info)
    }

    @Test
    fun `welcome questions - prompt caching evaluation`() {
        val apiKey = System.getenv("GROQ_API_KEY")
        assumeTrue("GROQ_API_KEY not set - skipping evaluation", !apiKey.isNullOrEmpty())

        println("=".repeat(70))
        println("GROQ PROMPT CACHING EVALUATION")
        println("=".repeat(70))
        println("Models: ${CACHE_MODELS.joinToString(", ")}")
        println("Question Set: WELCOME (W1-W8)")
        println("Runs per model: 2 (cold + warm)")
        println("=".repeat(70))

        val allResults = mutableMapOf<String, Pair<RunStats, RunStats>>() // model -> (cold, warm)

        CACHE_MODELS.forEachIndexed { modelIndex, model ->
            println("\n${"=".repeat(60)}")
            println("MODEL ${modelIndex + 1}/${CACHE_MODELS.size}: $model")
            println("=".repeat(60))

            val coldStats = runWelcomeQuestions(model, "COLD")

            println("\nWaiting ${INTER_RUN_DELAY_MS / 1000}s before warm run...")
            Thread.sleep(INTER_RUN_DELAY_MS)

            val warmStats = runWelcomeQuestions(model, "WARM")

            allResults[model] = coldStats to warmStats

            if (modelIndex < CACHE_MODELS.lastIndex) {
                println("\nWaiting ${INTER_MODEL_DELAY_MS / 1000}s before next model...")
                Thread.sleep(INTER_MODEL_DELAY_MS)
            }
        }

        printComparisonSummary(allResults)
    }

    @Test
    fun `gpt-oss-20b only - quick caching test`() {
        val apiKey = System.getenv("GROQ_API_KEY")
        assumeTrue("GROQ_API_KEY not set - skipping evaluation", !apiKey.isNullOrEmpty())

        val model = "openai/gpt-oss-20b"
        println("=".repeat(60))
        println("QUICK CACHING TEST: $model")
        println("=".repeat(60))

        val coldStats = runWelcomeQuestions(model, "COLD")
        Thread.sleep(INTER_RUN_DELAY_MS)
        val warmStats = runWelcomeQuestions(model, "WARM")

        printSingleModelComparison(model, coldStats, warmStats)
    }

    @Test
    fun `gpt-oss-120b only - quick caching test`() {
        val apiKey = System.getenv("GROQ_API_KEY")
        assumeTrue("GROQ_API_KEY not set - skipping evaluation", !apiKey.isNullOrEmpty())

        val model = "openai/gpt-oss-120b"
        println("=".repeat(60))
        println("QUICK CACHING TEST: $model")
        println("=".repeat(60))

        val coldStats = runWelcomeQuestions(model, "COLD")
        Thread.sleep(INTER_RUN_DELAY_MS)
        val warmStats = runWelcomeQuestions(model, "WARM")

        printSingleModelComparison(model, coldStats, warmStats)
    }

    private fun runWelcomeQuestions(model: String, runLabel: String): RunStats {
        println("\n--- $runLabel RUN ---")

        val config = EvalConfig(
            model = model,
            questionSet = QuestionSet.WELCOME,
            projectMode = ProjectContextMode.CURATED,
            promptVariant = PromptVariant.BASELINE,
            delayMs = INTER_QUESTION_DELAY_MS
        )

        val runner = EvalRunner(config)
        assertTrue(runner.initialize(), "Failed to initialize for $model")

        val result = runner.run()

        val stats = RunStats(
            runId = result.runId,
            model = model,
            label = runLabel,
            totalQuestions = result.totalQuestions,
            successCount = result.successCount,
            errorCount = result.errorCount,
            reportPath = result.reportPath
        )

        println("$runLabel: ${stats.successCount}/${stats.totalQuestions} success")
        println("Report: ${stats.reportPath}")

        return stats
    }

    private fun printSingleModelComparison(model: String, cold: RunStats, warm: RunStats) {
        println("\n${"=".repeat(60)}")
        println("CACHING RESULTS: $model")
        println("=".repeat(60))
        println()
        println("| Run  | Success | Run ID   | Report |")
        println("|------|---------|----------|--------|")
        println("| COLD | ${cold.successCount}/${cold.totalQuestions} | ${cold.runId} | ${cold.reportPath?.takeLast(40)} |")
        println("| WARM | ${warm.successCount}/${warm.totalQuestions} | ${warm.runId} | ${warm.reportPath?.takeLast(40)} |")
        println()
        println("Check Phoenix for detailed metrics:")
        println("  http://localhost:6006")
        println()
        println("Filter by service names:")
        println("  - cv-agent-eval-${cold.runId} (COLD)")
        println("  - cv-agent-eval-${warm.runId} (WARM)")
        println()
        println("Look for 'cached_tokens' in span attributes.")
        println("WARM run should show high cached_tokens if caching is working.")
        println("=".repeat(60))
    }

    private fun printComparisonSummary(results: Map<String, Pair<RunStats, RunStats>>) {
        println("\n${"=".repeat(70)}")
        println("PROMPT CACHING EVALUATION SUMMARY")
        println("=".repeat(70))
        println()
        println("| Model | Cold Success | Warm Success | Cold Run ID | Warm Run ID |")
        println("|-------|--------------|--------------|-------------|-------------|")

        results.forEach { (model, stats) ->
            val (cold, warm) = stats
            val shortModel = model.removePrefix("openai/")
            println(
                "| $shortModel | ${cold.successCount}/${cold.totalQuestions} | " +
                    "${warm.successCount}/${warm.totalQuestions} | ${cold.runId} | ${warm.runId} |"
            )
        }

        println()
        println("NEXT STEPS:")
        println("-".repeat(70))
        println("1. Open Phoenix: http://localhost:6006")
        println("2. Compare COLD vs WARM runs for each model")
        println("3. Check 'cached_tokens' attribute in LLM spans")
        println("4. Verify WARM runs show: cached_tokens > 0, lower latency")
        println()
        println("Expected behavior:")
        println("  - COLD run: cached_tokens = 0 (no cache)")
        println("  - WARM run: cached_tokens ~= prompt_tokens (high cache hit)")
        println("  - WARM latency < COLD latency (faster due to caching)")
        println()

        println("Phoenix GraphQL query for detailed analysis:")
        println("-".repeat(70))
        println(
            """
curl -s http://localhost:6006/graphql -H "Content-Type: application/json" \
  -d '{"query":"{ projects { edges { node { id name } } } }"}'
            """.trimIndent()
        )
        println()
        println("=".repeat(70))
    }

    data class RunStats(
        val runId: String,
        val model: String,
        val label: String,
        val totalQuestions: Int,
        val successCount: Int,
        val errorCount: Int,
        val reportPath: String?
    )
}
