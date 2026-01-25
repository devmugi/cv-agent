package io.github.devmugi.cv.agent.eval

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.github.devmugi.cv.agent.agent.ProjectContextMode
import io.github.devmugi.cv.agent.eval.config.DataFormat
import io.github.devmugi.cv.agent.eval.config.EvalConfig
import io.github.devmugi.cv.agent.eval.config.PromptVariant
import io.github.devmugi.cv.agent.eval.config.QuestionSet
import io.github.devmugi.cv.agent.eval.runner.EvalRunner
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Test runner for CV Agent evaluation.
 *
 * Run with:
 * ```
 * ./gradlew :eval:eval
 * ```
 *
 * Configure via environment variables:
 * - EVAL_VARIANT: BASELINE, PERSONA_CONCISE, PERSONA_RECRUITER, PERSONA_DETAILED, ROLE_FIRST_PERSON
 * - EVAL_MODEL: llama-3.3-70b-versatile, llama-3.1-8b-instant, etc.
 * - EVAL_PROJECT_MODE: CURATED, ALL_PROJECTS
 * - EVAL_FORMAT: TEXT, JSON, MARKDOWN
 * - EVAL_QUESTIONS: SIMPLE, CONVERSATIONS, ALL
 * - EVAL_DELAY_MS: delay between questions (default 10000)
 *
 * Or via Gradle properties:
 * ```
 * ./gradlew :eval:eval -PevalVariant=PERSONA_CONCISE -PevalModel=llama-3.1-8b-instant
 * ```
 */
@Suppress("FunctionNaming")
class EvalRunnerTest {

    @Before
    fun setup() {
        // Configure logging
        Logger.setLogWriters(object : LogWriter() {
            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                println("[$severity][$tag] $message")
                throwable?.printStackTrace()
            }
        })
        Logger.setMinSeverity(Severity.Debug)
    }

    @Test
    fun `run evaluation with configured settings`() {
        // Check API key is available
        val apiKey = System.getenv("GROQ_API_KEY")
        assumeTrue("GROQ_API_KEY not set - skipping evaluation", !apiKey.isNullOrEmpty())

        val config = buildConfigFromEnvironment()

        println("=".repeat(60))
        println("CV Agent Evaluation")
        println("=".repeat(60))
        println("Configuration:")
        println("  Variant: ${config.promptVariant}")
        println("  Model: ${config.model}")
        println("  Project Mode: ${config.projectMode}")
        println("  Data Format: ${config.dataFormat}")
        println("  Question Set: ${config.questionSet}")
        println("  Delay: ${config.delayMs}ms")
        println("=".repeat(60))

        val runner = EvalRunner(config)
        assertTrue(runner.initialize(), "Failed to initialize runner")

        val result = runner.run()

        println()
        println("=".repeat(60))
        println("FINAL RESULTS")
        println("=".repeat(60))
        println("Run ID: ${result.runId}")
        println("Total: ${result.totalQuestions}")
        println("Success: ${result.successCount}")
        println("Errors: ${result.errorCount}")
        println()
        result.reportPath?.let {
            println("Report: $it")
        }
        println("View traces: http://localhost:6006")
        println("=".repeat(60))

        // Note: Rate limit errors may occur with Groq free tier
        // The eval still succeeds if reports are generated
        if (result.errorCount > 0) {
            println("WARNING: ${result.errorCount} errors occurred (may be rate limits)")
        }
    }

    private fun buildConfigFromEnvironment(): EvalConfig {
        val variant = System.getenv("EVAL_VARIANT")?.let {
            PromptVariant.valueOf(it)
        } ?: PromptVariant.BASELINE

        val model = System.getenv("EVAL_MODEL")
            ?: "llama-3.3-70b-versatile"

        val projectMode = System.getenv("EVAL_PROJECT_MODE")?.let {
            ProjectContextMode.valueOf(it)
        } ?: ProjectContextMode.CURATED

        val dataFormat = System.getenv("EVAL_FORMAT")?.let {
            DataFormat.valueOf(it)
        } ?: DataFormat.TEXT

        val questionSet = System.getenv("EVAL_QUESTIONS")?.let {
            QuestionSet.valueOf(it)
        } ?: QuestionSet.SIMPLE

        val delayMs = System.getenv("EVAL_DELAY_MS")?.toLongOrNull()
            ?: 2_000L

        val reportsDir = System.getenv("EVAL_REPORT_DIR")
            ?: "eval/reports"

        return EvalConfig(
            promptVariant = variant,
            model = model,
            projectMode = projectMode,
            dataFormat = dataFormat,
            questionSet = questionSet,
            delayMs = delayMs,
            reportsDir = reportsDir
        )
    }

    /**
     * Full matrix test - runs 10 questions per ProjectContextMode.
     * Total: 50 API calls with 5 second intervals (~4-5 minutes).
     *
     * Run with:
     * ```
     * ./gradlew :eval:testAndroidUnitTest --tests "*fullMatrixTest*" --rerun-tasks
     * ```
     */
    @Test
    fun `fullMatrixTest - 10 questions per mode`() {
        val apiKey = System.getenv("GROQ_API_KEY")
        assumeTrue("GROQ_API_KEY not set - skipping evaluation", !apiKey.isNullOrEmpty())

        val modes = listOf(
            ProjectContextMode.CURATED,
            ProjectContextMode.PERSONAL_INFO_ONLY,
            ProjectContextMode.ALL_PROJECTS,
            ProjectContextMode.MCDONALDS_JSON_FULL,
            ProjectContextMode.ALL_PROJECTS_JSON_FULL
        )

        // First 10 McDonald's questions
        val questions = listOf(
            "MCD1" to "What was Denys's role on the McDonald's project?",
            "MCD2" to "How long did Denys work on the McDonald's app?",
            "MCD3" to "What companies were involved in the McDonald's project chain?",
            "MCD4" to "How many users does the McDonald's Global App serve?",
            "MCD5" to "How many countries is the McDonald's app available in?",
            "MCD6" to "How many payment providers did Denys help integrate?",
            "MCD7" to "What feature streams did Denys work on at McDonald's?",
            "MCD8" to "What is MDS in the context of the McDonald's project?",
            "MCD9" to "What payment providers were integrated in the McDonald's app?",
            "MCD10" to "What delivery tracking provider was used in the McDonald's app?"
        )

        // Results: Map<QuestionId, Map<Mode, Response>>
        val results = mutableMapOf<String, MutableMap<ProjectContextMode, String>>()
        questions.forEach { (id, _) -> results[id] = mutableMapOf() }

        println("=".repeat(80))
        println("FULL MATRIX TEST - 10 Questions × 5 Modes = 50 API Calls")
        println("=".repeat(80))

        var callCount = 0
        val totalCalls = modes.size * questions.size

        modes.forEach { mode ->
            println("\n${"=".repeat(60)}")
            println("MODE: $mode")
            println("=".repeat(60))

            val config = EvalConfig(
                projectMode = mode,
                delayMs = 0,
                questionSet = QuestionSet.MCDONALDS
            )

            val runner = EvalRunner(config)
            assertTrue(runner.initialize(), "Failed to initialize runner for $mode")

            questions.forEachIndexed { qIndex, (qId, qText) ->
                callCount++
                println("\n[$callCount/$totalCalls] $qId: $qText")

                val response = runner.askSingleQuestion(qText)
                results[qId]!![mode] = response ?: "ERROR: No response"

                val preview = response?.take(100)?.replace("\n", " ") ?: "ERROR"
                println("Response: $preview...")

                if (qIndex < questions.lastIndex) {
                    Thread.sleep(5000) // 5 second delay between questions
                }
            }

            // Extra delay between modes
            if (mode != modes.last()) {
                println("\nSwitching mode, waiting 10 seconds...")
                Thread.sleep(10000)
            }
        }

        // Print detailed comparison
        println("\n" + "=".repeat(80))
        println("DETAILED RESULTS BY QUESTION")
        println("=".repeat(80))

        questions.forEach { (qId, qText) ->
            println("\n${"─".repeat(80)}")
            println("$qId: $qText")
            println("─".repeat(80))
            modes.forEach { mode ->
                val response = results[qId]!![mode] ?: "N/A"
                val status = if (response.startsWith("ERROR")) "❌" else "✅"
                println("\n$status [$mode]:")
                println(response.take(300))
            }
        }

        // Print summary statistics
        println("\n" + "=".repeat(80))
        println("SUMMARY STATISTICS")
        println("=".repeat(80))

        modes.forEach { mode ->
            val modeResponses = results.values.map { it[mode] ?: "" }
            val successCount = modeResponses.count { !it.startsWith("ERROR") }
            val avgLength = modeResponses.filter { !it.startsWith("ERROR") }
                .map { it.length }.average().toInt()
            println("$mode: $successCount/10 success, avg response length: $avgLength chars")
        }

        println("\n" + "=".repeat(80))
        println("END OF MATRIX TEST")
        println("=".repeat(80))
    }

    /**
     * Quick matrix test - runs 1 question per ProjectContextMode.
     * Total: 5 API calls with 5 second intervals.
     *
     * Run with:
     * ```
     * ./gradlew :eval:testAndroidUnitTest --tests "*quickMatrixTest*" --rerun-tasks
     * ```
     */
    @Test
    fun `quickMatrixTest - 1 question per mode`() {
        val apiKey = System.getenv("GROQ_API_KEY")
        assumeTrue("GROQ_API_KEY not set - skipping evaluation", !apiKey.isNullOrEmpty())

        val modes = listOf(
            ProjectContextMode.CURATED,
            ProjectContextMode.PERSONAL_INFO_ONLY,
            ProjectContextMode.ALL_PROJECTS,
            ProjectContextMode.MCDONALDS_JSON_FULL,
            ProjectContextMode.ALL_PROJECTS_JSON_FULL
        )

        val question = "What was Denys's role on the McDonald's project?"
        val results = mutableMapOf<ProjectContextMode, String>()

        println("=".repeat(60))
        println("Quick Matrix Test - 1 question per mode")
        println("=".repeat(60))

        modes.forEachIndexed { index, mode ->
            println("\n[${ index + 1 }/${modes.size}] Testing mode: $mode")

            val config = EvalConfig(
                projectMode = mode,
                delayMs = 0,
                questionSet = QuestionSet.MCDONALDS
            )

            val runner = EvalRunner(config)
            assertTrue(runner.initialize(), "Failed to initialize runner for $mode")

            val response = runner.askSingleQuestion(question)
            results[mode] = response ?: "ERROR: No response"

            println("Response preview: ${response?.take(100) ?: "ERROR"}...")

            if (index < modes.lastIndex) {
                println("Waiting 5 seconds...")
                Thread.sleep(5000)
            }
        }

        println("\n" + "=".repeat(60))
        println("RESULTS SUMMARY")
        println("=".repeat(60))
        results.forEach { (mode, response) ->
            val status = if (response.startsWith("ERROR")) "❌" else "✅"
            val preview = response.take(80).replace("\n", " ")
            println("$status $mode: $preview...")
        }
        println("=".repeat(60))
    }
}
