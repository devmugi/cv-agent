package io.github.devmugi.cv.agent.eval.runner

import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ProjectContextMode
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.api.tracing.OpenTelemetryAgentTracer
import io.github.devmugi.cv.agent.api.tracing.PromptMetadata
import io.github.devmugi.cv.agent.career.data.CareerProjectDataLoader
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.eval.config.DataFormat
import io.github.devmugi.cv.agent.eval.config.EvalConfig
import io.github.devmugi.cv.agent.eval.config.QuestionSet
import io.github.devmugi.cv.agent.eval.prompts.PromptVariants
import io.github.devmugi.cv.agent.eval.questions.Conversation
import io.github.devmugi.cv.agent.eval.questions.Conversations
import io.github.devmugi.cv.agent.eval.questions.Question
import io.github.devmugi.cv.agent.eval.questions.SimpleQuestions
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.util.Properties
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Orchestrates evaluation runs against the CV Agent.
 */
class EvalRunner(private val config: EvalConfig) {

    companion object {
        private const val TAG = "EvalRunner"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val projectLoader = CareerProjectDataLoader()

    private lateinit var tracer: OpenTelemetryAgentTracer
    private lateinit var apiClient: GroqApiClient
    private lateinit var personalInfo: PersonalInfo
    private lateinit var projects: List<CareerProject>

    private val runId = UUID.randomUUID().toString().take(8)
    private var questionCount = 0
    private var successCount = 0
    private var errorCount = 0

    /**
     * Initialize the evaluation runner.
     */
    fun initialize(): Boolean {
        val apiKey = loadApiKey()
        if (apiKey.isNullOrEmpty()) {
            Logger.e(TAG) { "GROQ_API_KEY not set. Set it in environment or local.properties" }
            return false
        }

        Logger.i(TAG) { "Initializing EvalRunner with config:" }
        Logger.i(TAG) { "  Variant: ${config.promptVariant}" }
        Logger.i(TAG) { "  Model: ${config.model}" }
        Logger.i(TAG) { "  Project Mode: ${config.projectMode}" }
        Logger.i(TAG) { "  Data Format: ${config.dataFormat}" }
        Logger.i(TAG) { "  Question Set: ${config.questionSet}" }
        Logger.i(TAG) { "  Run ID: $runId" }

        tracer = OpenTelemetryAgentTracer.create(
            endpoint = config.phoenixEndpoint,
            serviceName = "cv-agent-eval-$runId"
        )

        val httpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Note: GroqApiClient uses hardcoded model (llama-3.3-70b-versatile)
        // To test different models, modify GroqApiClient.MODEL constant
        apiClient = GroqApiClient(
            httpClient = httpClient,
            apiKey = apiKey,
            tracer = tracer
        )

        loadTestData()
        return true
    }

    /**
     * Run the evaluation.
     */
    fun run(): EvalResult {
        Logger.i(TAG) { "\n========== STARTING EVALUATION RUN: $runId ==========" }

        val questions = getQuestions()
        val conversations = getConversations()

        Logger.i(TAG) { "Running ${questions.size} questions and ${conversations.size} conversations" }

        // Run simple questions
        questions.forEach { question ->
            runQuestion(question)
            if (config.delayMs > 0) {
                Thread.sleep(config.delayMs)
            }
        }

        // Run conversations
        conversations.forEach { conversation ->
            runConversation(conversation)
            if (config.delayMs > 0) {
                Thread.sleep(config.delayMs)
            }
        }

        // Flush traces and print summary
        tracer.flush()
        Thread.sleep(2000) // Wait for traces to be sent

        val result = EvalResult(
            runId = runId,
            config = config,
            totalQuestions = questionCount,
            successCount = successCount,
            errorCount = errorCount
        )

        printSummary()
        return result
    }

    private fun getQuestions(): List<Question> {
        return when (config.questionSet) {
            QuestionSet.SIMPLE, QuestionSet.ALL -> SimpleQuestions.questions
            QuestionSet.CONVERSATIONS -> emptyList()
        }
    }

    private fun getConversations(): List<Conversation> {
        return when (config.questionSet) {
            QuestionSet.CONVERSATIONS, QuestionSet.ALL -> Conversations.conversations
            QuestionSet.SIMPLE -> emptyList()
        }
    }

    private fun runQuestion(question: Question) {
        questionCount++
        Logger.i(TAG) { "\n--- ${question.id}: ${question.category} ---" }
        Logger.i(TAG) { "Q: ${question.text}" }

        try {
            val systemPrompt = buildSystemPrompt()
            val messages = listOf(ChatMessage(role = "user", content = question.text))

            val latch = CountDownLatch(1)
            var response = ""
            var error: Throwable? = null

            runBlocking {
                apiClient.streamChatCompletion(
                    messages = messages,
                    systemPrompt = systemPrompt,
                    sessionId = "$runId-${question.id}",
                    turnNumber = 1,
                    promptMetadata = PromptMetadata(
                        version = "eval-1.0",
                        variant = "${config.promptVariant}-${config.projectMode}"
                    ),
                    onChunk = { response += it },
                    onComplete = { latch.countDown() },
                    onError = { e ->
                        error = e
                        latch.countDown()
                    }
                )
            }

            if (!latch.await(60, TimeUnit.SECONDS)) {
                Logger.e(TAG) { "Timeout waiting for response" }
                errorCount++
                return
            }

            if (error != null) {
                Logger.e(TAG) { "Error: ${error?.message}" }
                errorCount++
                return
            }

            Logger.i(TAG) { "A: ${response.take(500)}${if (response.length > 500) "..." else ""}" }
            successCount++
        } catch (e: Exception) {
            Logger.e(TAG) { "Exception: ${e.message}" }
            errorCount++
        }
    }

    private fun runConversation(conversation: Conversation) {
        questionCount++
        Logger.i(TAG) { "\n=== ${conversation.id}: ${conversation.description} ===" }

        try {
            val systemPrompt = buildSystemPrompt()
            val messages = mutableListOf<ChatMessage>()
            val sessionId = "$runId-${conversation.id}"

            conversation.turns.forEachIndexed { index, turn ->
                messages.add(ChatMessage(role = "user", content = turn))
                Logger.i(TAG) { "Turn ${index + 1} - User: $turn" }

                val latch = CountDownLatch(1)
                var response = ""
                var error: Throwable? = null

                runBlocking {
                    apiClient.streamChatCompletion(
                        messages = messages.toList(),
                        systemPrompt = systemPrompt,
                        sessionId = sessionId,
                        turnNumber = index + 1,
                        promptMetadata = PromptMetadata(
                            version = "eval-1.0",
                            variant = "${config.promptVariant}-${config.projectMode}"
                        ),
                        onChunk = { response += it },
                        onComplete = { latch.countDown() },
                        onError = { e ->
                            error = e
                            latch.countDown()
                        }
                    )
                }

                if (!latch.await(60, TimeUnit.SECONDS)) {
                    Logger.e(TAG) { "Timeout on turn ${index + 1}" }
                    errorCount++
                    return
                }

                if (error != null) {
                    Logger.e(TAG) { "Error on turn ${index + 1}: ${error?.message}" }
                    errorCount++
                    return
                }

                Logger.i(TAG) { "Turn ${index + 1} - Assistant: ${response.take(300)}..." }
                messages.add(ChatMessage(role = "assistant", content = response))

                // Delay between turns
                if (index < conversation.turns.lastIndex && config.delayMs > 0) {
                    Thread.sleep(config.delayMs / 2)
                }
            }

            successCount++
        } catch (e: Exception) {
            Logger.e(TAG) { "Exception in conversation: ${e.message}" }
            errorCount++
        }
    }

    private fun buildSystemPrompt(): String {
        val dataProvider = createDataProvider()
        val hasFeaturedOnly = config.projectMode == ProjectContextMode.CURATED

        val instructions = PromptVariants.getInstructions(config.promptVariant, hasFeaturedOnly)
        val personalInfoSection = formatPersonalInfo()
        val skillsSection = formatSkills()
        val projectIndex = formatProjectIndex()
        val projectDetails = formatProjectDetails(dataProvider)
        val suggestionInstructions = PromptVariants.SUGGESTION_INSTRUCTIONS

        return buildString {
            appendLine(instructions)
            appendLine()
            appendLine(personalInfoSection)
            appendLine()
            appendLine(skillsSection)
            appendLine()
            appendLine(projectIndex)
            appendLine()
            appendLine(projectDetails)
            appendLine()
            appendLine(suggestionInstructions)
        }
    }

    private fun formatPersonalInfo(): String {
        return when (config.dataFormat) {
            DataFormat.TEXT -> buildString {
                appendLine("# PERSONAL INFO")
                appendLine("Name: ${personalInfo.name}")
                appendLine("Title: ${personalInfo.title}")
                appendLine("Location: ${personalInfo.location}")
                appendLine("Email: ${personalInfo.email}")
                appendLine("LinkedIn: ${personalInfo.linkedin}")
                appendLine("GitHub: ${personalInfo.github}")
                appendLine()
                appendLine("Summary: ${personalInfo.summary}")
            }

            DataFormat.JSON -> buildString {
                appendLine("# PERSONAL INFO")
                appendLine("```json")
                appendLine(json.encodeToString(PersonalInfo.serializer(), personalInfo))
                appendLine("```")
            }

            DataFormat.MARKDOWN -> buildString {
                appendLine("# Personal Information")
                appendLine()
                appendLine("| Field | Value |")
                appendLine("|-------|-------|")
                appendLine("| **Name** | ${personalInfo.name} |")
                appendLine("| **Title** | ${personalInfo.title} |")
                appendLine("| **Location** | ${personalInfo.location} |")
                appendLine("| **Email** | ${personalInfo.email} |")
                appendLine("| **LinkedIn** | ${personalInfo.linkedin} |")
                appendLine("| **GitHub** | ${personalInfo.github} |")
                appendLine()
                appendLine("## Summary")
                appendLine(personalInfo.summary)
            }
        }
    }

    private fun formatSkills(): String {
        return when (config.dataFormat) {
            DataFormat.TEXT -> buildString {
                appendLine("# SKILLS")
                personalInfo.skills.forEach { category ->
                    appendLine("${category.category}: ${category.items.joinToString(", ")}")
                }
            }

            DataFormat.JSON -> buildString {
                appendLine("# SKILLS")
                appendLine("```json")
                val skillsMap = personalInfo.skills.associate { it.category to it.items }
                appendLine(
                    json.encodeToString(
                        kotlinx.serialization.serializer<Map<String, List<String>>>(),
                        skillsMap
                    )
                )
                appendLine("```")
            }

            DataFormat.MARKDOWN -> buildString {
                appendLine("# Skills")
                appendLine()
                personalInfo.skills.forEach { category ->
                    appendLine("## ${category.category}")
                    category.items.forEach { item ->
                        appendLine("- $item")
                    }
                    appendLine()
                }
            }
        }
    }

    private fun formatProjectIndex(): String {
        return buildString {
            appendLine("# PROJECT INDEX")
            appendLine("All projects Denys has worked on:")
            projects.forEach { project ->
                val role = project.overview?.role ?: "Developer"
                val period = project.overview?.period?.displayText ?: ""
                val tagline = project.tagline ?: ""
                appendLine("- ${project.id}: \"${project.name}\" | $role | $period | $tagline")
            }
        }
    }

    private fun formatProjectDetails(dataProvider: AgentDataProvider): String {
        val featuredProjects = dataProvider.getFeaturedProjects()

        return buildString {
            appendLine("# PROJECT DETAILS")
            appendLine()

            featuredProjects.forEach { project ->
                appendLine("## ${project.name} (${project.id})")
                appendLine()

                project.overview?.let { overview ->
                    overview.company?.let { appendLine("Company: $it") }
                    overview.role?.let { appendLine("Role: $it") }
                    overview.period?.displayText?.let { appendLine("Period: $it") }
                }

                project.description?.let { desc ->
                    appendLine()
                    desc.short?.let { appendLine(it) }
                    desc.full?.let { appendLine(it) }
                }

                project.technologies?.primary?.let { techs ->
                    if (techs.isNotEmpty()) {
                        appendLine()
                        append("Technologies: ")
                        appendLine(techs.mapNotNull { it.name }.joinToString(", "))
                    }
                }

                project.standout?.let { standout ->
                    appendLine()
                    appendLine("What stands out:")
                    standout.items?.forEach { item ->
                        appendLine("- ${item.title}: ${item.description}")
                    }
                }

                appendLine()
            }
        }
    }

    private fun createDataProvider(): AgentDataProvider {
        return AgentDataProvider(
            personalInfo = personalInfo,
            allProjects = projects,
            featuredProjectIds = AgentDataProvider.FEATURED_PROJECT_IDS,
            contextMode = config.projectMode
        )
    }

    @Suppress("SwallowedException")
    private fun loadApiKey(): String? {
        // First try environment variable
        System.getenv("GROQ_API_KEY")?.let { return it }

        // Then try local.properties
        return try {
            val propsFile = File("local.properties")
            if (propsFile.exists()) {
                Properties().apply { load(propsFile.inputStream()) }
                    .getProperty("GROQ_API_KEY")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadTestData() {
        val resourcesDir = File("shared-career-projects/src/commonMain/composeResources/files")

        // Load personal info
        val personalInfoJson = File(resourcesDir, "personal_info.json").readText()
        personalInfo = json.decodeFromString<PersonalInfo>(personalInfoJson)

        // Load all projects
        val projectsDir = File(resourcesDir, "projects")
        projects = projectsDir.listFiles { f -> f.name.endsWith("_details_data.json") }
            ?.map { file ->
                projectLoader.loadCareerProject(file.readText())
            } ?: emptyList()

        Logger.i(TAG) { "Loaded ${projects.size} projects" }
    }

    private fun printSummary() {
        Logger.i(TAG) { "\n========== EVALUATION SUMMARY ==========" }
        Logger.i(TAG) { "Run ID: $runId" }
        Logger.i(TAG) { "Config:" }
        Logger.i(TAG) { "  Variant: ${config.promptVariant}" }
        Logger.i(TAG) { "  Model: ${config.model}" }
        Logger.i(TAG) { "  Project Mode: ${config.projectMode}" }
        Logger.i(TAG) { "  Data Format: ${config.dataFormat}" }
        Logger.i(TAG) { "" }
        Logger.i(TAG) { "Results:" }
        Logger.i(TAG) { "  Total: $questionCount" }
        Logger.i(TAG) { "  Success: $successCount" }
        Logger.i(TAG) { "  Errors: $errorCount" }
        Logger.i(TAG) { "" }
        Logger.i(TAG) { "View traces at: http://localhost:6006" }
        Logger.i(TAG) { "Filter by service: cv-agent-eval-$runId" }
        Logger.i(TAG) { "==========================================" }
    }
}

/**
 * Result of an evaluation run.
 */
data class EvalResult(
    val runId: String,
    val config: EvalConfig,
    val totalQuestions: Int,
    val successCount: Int,
    val errorCount: Int
)
