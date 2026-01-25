package io.github.devmugi.cv.agent.eval.config

import io.github.devmugi.cv.agent.agent.ProjectContextMode
import kotlinx.serialization.Serializable

/**
 * Configuration for an evaluation run.
 */
@Serializable
data class EvalConfig(
    val promptVariant: PromptVariant = PromptVariant.BASELINE,
    val dataFormat: DataFormat = DataFormat.TEXT,
    val projectMode: ProjectContextMode = ProjectContextMode.CURATED,
    val model: String = "llama-3.3-70b-versatile",
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024,
    val questionSet: QuestionSet = QuestionSet.SIMPLE,
    val delayMs: Long = 5_000,
    val phoenixEndpoint: String = "http://localhost:6006/v1/traces",
    val reportsDir: String = "eval/reports"
)

/**
 * Prompt instruction variants to test.
 */
enum class PromptVariant {
    /** Current production prompt */
    BASELINE,

    /** Minimal, concise instructions */
    PERSONA_CONCISE,

    /** Recruiter-focused language */
    PERSONA_RECRUITER,

    /** Detailed instructions with response format guidance */
    PERSONA_DETAILED,

    /** Allow first person responses */
    ROLE_FIRST_PERSON,

    /** Tiered response with explicit boundaries for off-topic deflection */
    PERSONA_BOUNDED
}

/**
 * Data formatting options for CV data in the prompt.
 */
enum class DataFormat {
    /** Plain text format (current) */
    TEXT,

    /** Structured JSON format */
    JSON,

    /** Markdown with headers */
    MARKDOWN
}

/**
 * Question sets for evaluation.
 */
enum class QuestionSet {
    /** Simple questions Q1-Q10 */
    SIMPLE,

    /** Multi-turn conversations Conv1-Conv10 */
    CONVERSATIONS,

    /** McDonald's project deep-dive questions MCD1-MCD20 */
    MCDONALDS,

    /** 5 questions per project (65 total) */
    ALL_PROJECTS,

    /** General profile questions about skills/experience (10) */
    PROFILE,

    /** Off-topic and malicious questions to test boundaries (10) */
    MALICIOUS,

    /** All questions and conversations */
    ALL
}
