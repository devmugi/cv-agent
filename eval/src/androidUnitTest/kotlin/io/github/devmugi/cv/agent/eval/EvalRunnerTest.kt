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

        assertTrue(result.errorCount == 0, "Evaluation had ${result.errorCount} errors")
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
}
