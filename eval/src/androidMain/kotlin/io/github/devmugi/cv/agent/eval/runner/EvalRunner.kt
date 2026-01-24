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
import io.github.devmugi.cv.agent.eval.report.ConversationResult
import io.github.devmugi.cv.agent.eval.report.EvalReport
import io.github.devmugi.cv.agent.eval.report.QuestionResult
import io.github.devmugi.cv.agent.eval.report.ReportWriter
import io.github.devmugi.cv.agent.eval.report.RunSummary
import io.github.devmugi.cv.agent.eval.report.TurnResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.time.Instant
import java.util.Properties
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Orchestrates evaluation runs against the CV Agent.
 */
@Suppress("TooManyFunctions", "LargeClass")
class EvalRunner(private val config: EvalConfig) {

    companion object {
        private const val TAG = "EvalRunner"
        private val SUGGESTION_REGEX = """\{"suggestions":\s*\[([^\]]*)\]\}""".toRegex()
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val projectLoader = CareerProjectDataLoader()

    private lateinit var tracer: OpenTelemetryAgentTracer
    private lateinit var apiClient: GroqApiClient
    private lateinit var personalInfo: PersonalInfo
    private lateinit var projects: List<CareerProject>
    private lateinit var reportWriter: ReportWriter

    private val runId = UUID.randomUUID().toString().take(8)
    private val questionResults = mutableListOf<QuestionResult>()
    private val conversationResults = mutableListOf<ConversationResult>()

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
        Logger.i(TAG) { "  Delay: ${config.delayMs}ms" }
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

        // Initialize report writer (use project root for relative paths)
        val reportsDir = if (File(config.reportsDir).isAbsolute) {
            File(config.reportsDir)
        } else {
            File(findProjectRoot(), config.reportsDir)
        }
        reportWriter = ReportWriter(reportsDir)

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
            val result = runQuestion(question)
            questionResults.add(result)
            if (config.delayMs > 0) {
                Thread.sleep(config.delayMs)
            }
        }

        // Run conversations
        conversations.forEach { conversation ->
            val result = runConversation(conversation)
            conversationResults.add(result)
            if (config.delayMs > 0) {
                Thread.sleep(config.delayMs)
            }
        }

        // Flush traces
        tracer.flush()
        Thread.sleep(2000) // Wait for traces to be sent

        // Generate report
        val report = buildReport()
        val (jsonFile, mdFile) = reportWriter.writeReports(report)

        printSummary(report, jsonFile, mdFile)

        return EvalResult(
            runId = runId,
            config = config,
            totalQuestions = questionResults.size + conversationResults.size,
            successCount = questionResults.count { it.success } + conversationResults.count { it.success },
            errorCount = questionResults.count { !it.success } + conversationResults.count { !it.success },
            reportPath = jsonFile.absolutePath
        )
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

    @Suppress("LongMethod")
    private fun runQuestion(question: Question): QuestionResult {
        Logger.i(TAG) { "\n--- ${question.id}: ${question.category} ---" }
        Logger.i(TAG) { "Q: ${question.text}" }

        val startTime = System.currentTimeMillis()
        var ttftMs: Long? = null
        var firstChunkReceived = false

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
                    onChunk = { chunk ->
                        if (!firstChunkReceived) {
                            ttftMs = System.currentTimeMillis() - startTime
                            firstChunkReceived = true
                        }
                        response += chunk
                    },
                    onComplete = { latch.countDown() },
                    onError = { e ->
                        error = e
                        latch.countDown()
                    }
                )
            }

            val latencyMs = System.currentTimeMillis() - startTime

            if (!latch.await(60, TimeUnit.SECONDS)) {
                Logger.e(TAG) { "Timeout waiting for response" }
                return QuestionResult(
                    questionId = question.id,
                    questionText = question.text,
                    category = question.category,
                    response = "",
                    latencyMs = latencyMs,
                    ttftMs = ttftMs,
                    success = false,
                    errorMessage = "Timeout after 60 seconds"
                )
            }

            if (error != null) {
                Logger.e(TAG) { "Error: ${error?.message}" }
                return QuestionResult(
                    questionId = question.id,
                    questionText = question.text,
                    category = question.category,
                    response = response,
                    latencyMs = latencyMs,
                    ttftMs = ttftMs,
                    success = false,
                    errorMessage = error?.message
                )
            }

            val suggestions = extractSuggestions(response)
            Logger.i(TAG) { "A: ${response.take(500)}${if (response.length > 500) "..." else ""}" }
            if (suggestions.isNotEmpty()) {
                Logger.i(TAG) { "Suggestions: $suggestions" }
            }

            return QuestionResult(
                questionId = question.id,
                questionText = question.text,
                category = question.category,
                response = response,
                latencyMs = latencyMs,
                ttftMs = ttftMs,
                suggestions = suggestions,
                success = true
            )
        } catch (e: Exception) {
            Logger.e(TAG) { "Exception: ${e.message}" }
            return QuestionResult(
                questionId = question.id,
                questionText = question.text,
                category = question.category,
                response = "",
                latencyMs = System.currentTimeMillis() - startTime,
                ttftMs = ttftMs,
                success = false,
                errorMessage = e.message
            )
        }
    }

    @Suppress("LongMethod")
    private fun runConversation(conversation: Conversation): ConversationResult {
        Logger.i(TAG) { "\n=== ${conversation.id}: ${conversation.description} ===" }

        val conversationStartTime = System.currentTimeMillis()
        val turnResults = mutableListOf<TurnResult>()

        try {
            val systemPrompt = buildSystemPrompt()
            val messages = mutableListOf<ChatMessage>()
            val sessionId = "$runId-${conversation.id}"

            conversation.turns.forEachIndexed { index, turn ->
                messages.add(ChatMessage(role = "user", content = turn))
                Logger.i(TAG) { "Turn ${index + 1} - User: $turn" }

                val turnStartTime = System.currentTimeMillis()
                var ttftMs: Long? = null
                var firstChunkReceived = false

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
                        onChunk = { chunk ->
                            if (!firstChunkReceived) {
                                ttftMs = System.currentTimeMillis() - turnStartTime
                                firstChunkReceived = true
                            }
                            response += chunk
                        },
                        onComplete = { latch.countDown() },
                        onError = { e ->
                            error = e
                            latch.countDown()
                        }
                    )
                }

                val turnLatencyMs = System.currentTimeMillis() - turnStartTime

                if (!latch.await(60, TimeUnit.SECONDS)) {
                    Logger.e(TAG) { "Timeout on turn ${index + 1}" }
                    return ConversationResult(
                        conversationId = conversation.id,
                        description = conversation.description,
                        turns = turnResults,
                        totalLatencyMs = System.currentTimeMillis() - conversationStartTime,
                        success = false,
                        errorMessage = "Timeout on turn ${index + 1}"
                    )
                }

                if (error != null) {
                    Logger.e(TAG) { "Error on turn ${index + 1}: ${error?.message}" }
                    return ConversationResult(
                        conversationId = conversation.id,
                        description = conversation.description,
                        turns = turnResults,
                        totalLatencyMs = System.currentTimeMillis() - conversationStartTime,
                        success = false,
                        errorMessage = error?.message
                    )
                }

                val suggestions = extractSuggestions(response)
                Logger.i(TAG) { "Turn ${index + 1} - Assistant: ${response.take(300)}..." }

                turnResults.add(
                    TurnResult(
                        turnNumber = index + 1,
                        userMessage = turn,
                        response = response,
                        latencyMs = turnLatencyMs,
                        ttftMs = ttftMs,
                        suggestions = suggestions
                    )
                )

                messages.add(ChatMessage(role = "assistant", content = response))

                // Delay between turns
                if (index < conversation.turns.lastIndex && config.delayMs > 0) {
                    Thread.sleep(config.delayMs / 2)
                }
            }

            return ConversationResult(
                conversationId = conversation.id,
                description = conversation.description,
                turns = turnResults,
                totalLatencyMs = System.currentTimeMillis() - conversationStartTime,
                success = true
            )
        } catch (e: Exception) {
            Logger.e(TAG) { "Exception in conversation: ${e.message}" }
            return ConversationResult(
                conversationId = conversation.id,
                description = conversation.description,
                turns = turnResults,
                totalLatencyMs = System.currentTimeMillis() - conversationStartTime,
                success = false,
                errorMessage = e.message
            )
        }
    }

    private fun extractSuggestions(response: String): List<String> {
        val match = SUGGESTION_REGEX.find(response) ?: return emptyList()
        val suggestionsContent = match.groupValues[1]
        return suggestionsContent
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }

    private fun buildReport(): EvalReport {
        val allLatencies = questionResults.map { it.latencyMs } +
            conversationResults.flatMap { it.turns.map { t -> t.latencyMs } }

        val allTtfts = (questionResults.mapNotNull { it.ttftMs } +
            conversationResults.flatMap { it.turns.mapNotNull { t -> t.ttftMs } })

        val sortedLatencies = allLatencies.sorted()

        val summary = RunSummary(
            totalQuestions = questionResults.size,
            totalConversations = conversationResults.size,
            successCount = questionResults.count { it.success } + conversationResults.count { it.success },
            errorCount = questionResults.count { !it.success } + conversationResults.count { !it.success },
            avgLatencyMs = if (allLatencies.isNotEmpty()) allLatencies.average() else 0.0,
            avgTtftMs = if (allTtfts.isNotEmpty()) allTtfts.average() else null,
            totalPromptTokens = questionResults.sumOf { it.promptTokens } +
                conversationResults.flatMap { it.turns }.sumOf { it.promptTokens },
            totalCompletionTokens = questionResults.sumOf { it.completionTokens } +
                conversationResults.flatMap { it.turns }.sumOf { it.completionTokens },
            totalTokens = questionResults.sumOf { it.totalTokens } +
                conversationResults.flatMap { it.turns }.sumOf { it.totalTokens },
            p50LatencyMs = calculatePercentile(sortedLatencies, 50),
            p95LatencyMs = calculatePercentile(sortedLatencies, 95)
        )

        return EvalReport(
            runId = runId,
            timestamp = Instant.now().toString(),
            config = config,
            summary = summary,
            questionResults = questionResults.toList(),
            conversationResults = conversationResults.toList()
        )
    }

    private fun calculatePercentile(sortedList: List<Long>, percentile: Int): Long {
        if (sortedList.isEmpty()) return 0L
        val index = (percentile / 100.0 * (sortedList.size - 1)).toInt()
        return sortedList[index]
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

    private fun findProjectRoot(): File {
        // Try to find project root by looking for settings.gradle.kts
        var dir = File(System.getProperty("user.dir"))
        repeat(10) {
            if (File(dir, "settings.gradle.kts").exists()) {
                return dir
            }
            dir = dir.parentFile ?: return File(System.getProperty("user.dir"))
        }
        return File(System.getProperty("user.dir"))
    }

    private fun loadTestData() {
        val projectRoot = findProjectRoot()
        val resourcesDir = File(projectRoot, "shared-career-projects/src/commonMain/composeResources/files")

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

    private fun printSummary(report: EvalReport, jsonFile: File, mdFile: File) {
        Logger.i(TAG) { "\n========== EVALUATION SUMMARY ==========" }
        Logger.i(TAG) { "Run ID: $runId" }
        Logger.i(TAG) { "Config:" }
        Logger.i(TAG) { "  Variant: ${config.promptVariant}" }
        Logger.i(TAG) { "  Model: ${config.model}" }
        Logger.i(TAG) { "  Project Mode: ${config.projectMode}" }
        Logger.i(TAG) { "  Data Format: ${config.dataFormat}" }
        Logger.i(TAG) { "" }
        Logger.i(TAG) { "Results:" }
        Logger.i(TAG) { "  Questions: ${report.summary.totalQuestions}" }
        Logger.i(TAG) { "  Conversations: ${report.summary.totalConversations}" }
        Logger.i(TAG) { "  Success: ${report.summary.successCount}" }
        Logger.i(TAG) { "  Errors: ${report.summary.errorCount}" }
        Logger.i(TAG) { "" }
        Logger.i(TAG) { "Performance:" }
        Logger.i(TAG) { "  Avg Latency: ${report.summary.avgLatencyMs.toLong()}ms" }
        report.summary.avgTtftMs?.let {
            Logger.i(TAG) { "  Avg TTFT: ${it.toLong()}ms" }
        }
        Logger.i(TAG) { "  P50 Latency: ${report.summary.p50LatencyMs}ms" }
        Logger.i(TAG) { "  P95 Latency: ${report.summary.p95LatencyMs}ms" }
        Logger.i(TAG) { "" }
        Logger.i(TAG) { "Reports:" }
        Logger.i(TAG) { "  JSON: ${jsonFile.absolutePath}" }
        Logger.i(TAG) { "  Markdown: ${mdFile.absolutePath}" }
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
    val errorCount: Int,
    val reportPath: String? = null
)
