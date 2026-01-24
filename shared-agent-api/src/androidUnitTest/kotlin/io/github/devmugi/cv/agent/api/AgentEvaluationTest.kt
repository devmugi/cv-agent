package io.github.devmugi.cv.agent.api

import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ProjectContextMode
import io.github.devmugi.cv.agent.agent.SystemPromptBuilder
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.api.tracing.OpenTelemetryAgentTracer
import io.github.devmugi.cv.agent.career.data.CareerProjectDataLoader
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Evaluation tests comparing CURATED vs ALL_PROJECTS system prompt modes.
 *
 * Runs the same questions against both configurations and logs traces to Phoenix
 * for manual comparison of response quality and token usage.
 *
 * Prerequisites:
 * 1. Start Phoenix: `phoenix serve`
 * 2. Set GROQ_API_KEY in environment or local.properties
 * 3. Run as Android instrumented test OR on emulator/device
 *
 * Run: ./gradlew :shared-agent-api:connectedAndroidTest
 * View traces: http://localhost:6006
 */
@Suppress("FunctionNaming", "MagicNumber", "LargeClass")
class AgentEvaluationTest {

    private lateinit var tracer: OpenTelemetryAgentTracer
    private lateinit var apiClient: GroqApiClient
    private lateinit var promptBuilder: SystemPromptBuilder
    private lateinit var personalInfo: PersonalInfo
    private lateinit var projects: List<io.github.devmugi.cv.agent.career.models.CareerProject>
    private var apiKey: String = ""

    private val json = Json { ignoreUnknownKeys = true }
    private val projectLoader = CareerProjectDataLoader()

    @Before
    fun setup() {
        apiKey = System.getenv("GROQ_API_KEY") ?: loadApiKeyFromProperties() ?: ""
        assumeTrue("GROQ_API_KEY not set - skipping evaluation tests", apiKey.isNotEmpty())

        tracer = OpenTelemetryAgentTracer.create(
            endpoint = "http://localhost:6006/v1/traces",
            serviceName = "cv-agent-evaluation"
        )

        val httpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        apiClient = GroqApiClient(
            httpClient = httpClient,
            apiKey = apiKey,
            tracer = tracer
        )

        promptBuilder = SystemPromptBuilder()
        loadTestData()
    }

    @After
    fun tearDown() {
        if (::tracer.isInitialized) {
            tracer.flush()
            Thread.sleep(1000)
        }
    }

    @Suppress("SwallowedException")
    private fun loadApiKeyFromProperties(): String? {
        return try {
            val propsFile = File("../local.properties")
            if (propsFile.exists()) {
                Properties().apply { load(propsFile.inputStream()) }
                    .getProperty("GROQ_API_KEY")
            } else {
                null
            }
        } catch (e: Exception) {
            // Silently return null if properties file cannot be loaded
            null
        }
    }


    private fun loadTestData() {
        val resourcesDir = File("../shared-career-projects/src/commonMain/composeResources/files")

        // Load personal info
        val personalInfoJson = File(resourcesDir, "personal_info.json").readText()
        personalInfo = json.decodeFromString<PersonalInfo>(personalInfoJson)

        // Load all projects
        val projectsDir = File(resourcesDir, "projects")
        projects = projectsDir.listFiles { f -> f.name.endsWith("_details_data.json") }
            ?.map { file ->
                projectLoader.loadCareerProject(file.readText())
            } ?: emptyList()

        println("Loaded ${projects.size} projects: ${projects.map { it.id }}")
    }

    private fun createDataProvider(mode: ProjectContextMode): AgentDataProvider {
        return AgentDataProvider(
            personalInfo = personalInfo,
            allProjects = projects,
            featuredProjectIds = AgentDataProvider.FEATURED_PROJECT_IDS,
            contextMode = mode
        )
    }

    private fun runQuestion(question: String, mode: ProjectContextMode): String {
        val dataProvider = createDataProvider(mode)
        val systemPrompt = promptBuilder.build(dataProvider)

        println("\n=== Mode: $mode | Question: $question ===")
        println("System prompt length: ${systemPrompt.length} chars")

        val messages = listOf(ChatMessage(role = "user", content = question))
        val latch = CountDownLatch(1)
        var response = ""

        runBlocking {
            apiClient.streamChatCompletion(
                messages = messages,
                systemPrompt = systemPrompt,
                onChunk = { response += it },
                onComplete = { latch.countDown() },
                onError = { e ->
                    println("Error: ${e.message}")
                    latch.countDown()
                }
            )
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS), "API call timed out")
        println("Response: $response")
        return response
    }

    private fun runConversation(turns: List<String>, mode: ProjectContextMode): List<String> {
        val dataProvider = createDataProvider(mode)
        val systemPrompt = promptBuilder.build(dataProvider)

        println("\n=== Mode: $mode | Conversation ===")
        println("System prompt length: ${systemPrompt.length} chars")

        val messages = mutableListOf<ChatMessage>()
        val responses = mutableListOf<String>()

        turns.forEach { turn ->
            messages.add(ChatMessage(role = "user", content = turn))
            println("User: $turn")

            val latch = CountDownLatch(1)
            var response = ""

            runBlocking {
                apiClient.streamChatCompletion(
                    messages = messages.toList(),
                    systemPrompt = systemPrompt,
                    onChunk = { response += it },
                    onComplete = { latch.countDown() },
                    onError = { latch.countDown() }
                )
            }

            assertTrue(latch.await(60, TimeUnit.SECONDS), "API call timed out")
            println("Assistant: $response\n")

            messages.add(ChatMessage(role = "assistant", content = response))
            responses.add(response)
        }

        return responses
    }

    // ==================== SIMPLE QUESTIONS ====================

    @Test
    fun `Q1 CURATED - personal info recall`() {
        runQuestion("What is Denys's current job title and location?", ProjectContextMode.CURATED)
    }

    @Test
    fun `Q1 ALL_PROJECTS - personal info recall`() {
        runQuestion("What is Denys's current job title and location?", ProjectContextMode.ALL_PROJECTS)
    }

    @Test
    fun `Q2 CURATED - skills extraction`() {
        runQuestion("What programming languages does Denys know?", ProjectContextMode.CURATED)
    }

    @Test
    fun `Q2 ALL_PROJECTS - skills extraction`() {
        runQuestion("What programming languages does Denys know?", ProjectContextMode.ALL_PROJECTS)
    }

    @Test
    fun `Q3 CURATED - featured project detail`() {
        runQuestion("Tell me about the McDonald's project", ProjectContextMode.CURATED)
    }

    @Test
    fun `Q3 ALL_PROJECTS - featured project detail`() {
        runQuestion("Tell me about the McDonald's project", ProjectContextMode.ALL_PROJECTS)
    }

    @Test
    fun `Q4 CURATED - featured project role`() {
        runQuestion("What was Denys's role at GEOSATIS?", ProjectContextMode.CURATED)
    }

    @Test
    fun `Q4 ALL_PROJECTS - featured project role`() {
        runQuestion("What was Denys's role at GEOSATIS?", ProjectContextMode.ALL_PROJECTS)
    }

    @Test
    fun `Q5 CURATED - technology pattern matching`() {
        runQuestion("Has Denys worked with Kotlin Multiplatform?", ProjectContextMode.CURATED)
    }

    @Test
    fun `Q5 ALL_PROJECTS - technology pattern matching`() {
        runQuestion("Has Denys worked with Kotlin Multiplatform?", ProjectContextMode.ALL_PROJECTS)
    }

    @Test
    fun `Q6 CURATED - non-featured project (curated gap)`() {
        runQuestion("What is the Lesara project about?", ProjectContextMode.CURATED)
    }

    @Test
    fun `Q6 ALL_PROJECTS - non-featured project (full coverage)`() {
        runQuestion("What is the Lesara project about?", ProjectContextMode.ALL_PROJECTS)
    }

    @Test
    fun `Q7 CURATED - cross-project aggregation`() {
        runQuestion("List all companies Denys has worked for", ProjectContextMode.CURATED)
    }

    @Test
    fun `Q7 ALL_PROJECTS - cross-project aggregation`() {
        runQuestion("List all companies Denys has worked for", ProjectContextMode.ALL_PROJECTS)
    }

    @Test
    fun `Q8 CURATED - skill and project correlation`() {
        runQuestion("What IoT experience does Denys have?", ProjectContextMode.CURATED)
    }

    @Test
    fun `Q8 ALL_PROJECTS - skill and project correlation`() {
        runQuestion("What IoT experience does Denys have?", ProjectContextMode.ALL_PROJECTS)
    }

    @Test
    fun `Q9 CURATED - featured project technical depth`() {
        runQuestion("Tell me about the Adidas GMR smart insole", ProjectContextMode.CURATED)
    }

    @Test
    fun `Q9 ALL_PROJECTS - featured project technical depth`() {
        runQuestion("Tell me about the Adidas GMR smart insole", ProjectContextMode.ALL_PROJECTS)
    }

    @Test
    fun `Q10 CURATED - teaching and training`() {
        runQuestion("What teaching or training experience does Denys have?", ProjectContextMode.CURATED)
    }

    @Test
    fun `Q10 ALL_PROJECTS - teaching and training`() {
        runQuestion("What teaching or training experience does Denys have?", ProjectContextMode.ALL_PROJECTS)
    }

    // ==================== MULTI-TURN CONVERSATIONS ====================

    @Test
    fun `Conv1 CURATED - project deep dive`() {
        runConversation(
            listOf(
                "What projects has Denys worked on?",
                "Tell me more about the GEOSATIS project",
                "What technologies were used in that project?"
            ),
            ProjectContextMode.CURATED
        )
    }

    @Test
    fun `Conv1 ALL_PROJECTS - project deep dive`() {
        runConversation(
            listOf(
                "What projects has Denys worked on?",
                "Tell me more about the GEOSATIS project",
                "What technologies were used in that project?"
            ),
            ProjectContextMode.ALL_PROJECTS
        )
    }

    @Test
    fun `Conv2 CURATED - technology focus`() {
        runConversation(
            listOf(
                "Does Denys have experience with backend development?",
                "What backend frameworks has he used?",
                "Can you give an example project where he did backend work?"
            ),
            ProjectContextMode.CURATED
        )
    }

    @Test
    fun `Conv2 ALL_PROJECTS - technology focus`() {
        runConversation(
            listOf(
                "Does Denys have experience with backend development?",
                "What backend frameworks has he used?",
                "Can you give an example project where he did backend work?"
            ),
            ProjectContextMode.ALL_PROJECTS
        )
    }

    @Test
    fun `Conv3 CURATED - career progression`() {
        runConversation(
            listOf(
                "How long has Denys been working in software development?",
                "What was his first Android project?",
                "How has his role evolved over time?"
            ),
            ProjectContextMode.CURATED
        )
    }

    @Test
    fun `Conv3 ALL_PROJECTS - career progression`() {
        runConversation(
            listOf(
                "How long has Denys been working in software development?",
                "What was his first Android project?",
                "How has his role evolved over time?"
            ),
            ProjectContextMode.ALL_PROJECTS
        )
    }

    @Test
    fun `Conv4 CURATED - comparison query`() {
        runConversation(
            listOf(
                "Compare the McDonald's and GEOSATIS projects",
                "Which one was longer?",
                "What skills did Denys develop in each?"
            ),
            ProjectContextMode.CURATED
        )
    }

    @Test
    fun `Conv4 ALL_PROJECTS - comparison query`() {
        runConversation(
            listOf(
                "Compare the McDonald's and GEOSATIS projects",
                "Which one was longer?",
                "What skills did Denys develop in each?"
            ),
            ProjectContextMode.ALL_PROJECTS
        )
    }

    @Test
    fun `Conv5 CURATED - non-featured project probe`() {
        runConversation(
            listOf(
                "Has Denys worked in e-commerce?",
                "Tell me about the Lesara project",
                "What was the tech stack?"
            ),
            ProjectContextMode.CURATED
        )
    }

    @Test
    fun `Conv5 ALL_PROJECTS - non-featured project probe`() {
        runConversation(
            listOf(
                "Has Denys worked in e-commerce?",
                "Tell me about the Lesara project",
                "What was the tech stack?"
            ),
            ProjectContextMode.ALL_PROJECTS
        )
    }

    @Test
    fun `Conv6 CURATED - skill verification`() {
        runConversation(
            listOf(
                "Can Denys build Android TV apps?",
                "What project involved Android TV?",
                "What were the challenges in that project?"
            ),
            ProjectContextMode.CURATED
        )
    }

    @Test
    fun `Conv6 ALL_PROJECTS - skill verification`() {
        runConversation(
            listOf(
                "Can Denys build Android TV apps?",
                "What project involved Android TV?",
                "What were the challenges in that project?"
            ),
            ProjectContextMode.ALL_PROJECTS
        )
    }

    @Test
    fun `Conv7 CURATED - team and leadership`() {
        runConversation(
            listOf(
                "Has Denys led teams before?",
                "How large were the teams?",
                "What was his leadership style?"
            ),
            ProjectContextMode.CURATED
        )
    }

    @Test
    fun `Conv7 ALL_PROJECTS - team and leadership`() {
        runConversation(
            listOf(
                "Has Denys led teams before?",
                "How large were the teams?",
                "What was his leadership style?"
            ),
            ProjectContextMode.ALL_PROJECTS
        )
    }

    @Test
    fun `Conv8 CURATED - client work`() {
        runConversation(
            listOf(
                "Has Denys worked with enterprise clients?",
                "Name some of the biggest clients",
                "What was the most challenging client project?"
            ),
            ProjectContextMode.CURATED
        )
    }

    @Test
    fun `Conv8 ALL_PROJECTS - client work`() {
        runConversation(
            listOf(
                "Has Denys worked with enterprise clients?",
                "Name some of the biggest clients",
                "What was the most challenging client project?"
            ),
            ProjectContextMode.ALL_PROJECTS
        )
    }

    @Test
    fun `Conv9 CURATED - education and teaching`() {
        runConversation(
            listOf(
                "Has Denys trained other developers?",
                "What was the Android School program?",
                "What topics did he teach?"
            ),
            ProjectContextMode.CURATED
        )
    }

    @Test
    fun `Conv9 ALL_PROJECTS - education and teaching`() {
        runConversation(
            listOf(
                "Has Denys trained other developers?",
                "What was the Android School program?",
                "What topics did he teach?"
            ),
            ProjectContextMode.ALL_PROJECTS
        )
    }

    @Test
    fun `Conv10 CURATED - recent work`() {
        runConversation(
            listOf(
                "What is Denys working on currently?",
                "How long has he been at GEOSATIS?",
                "What's unique about the victim protection app?"
            ),
            ProjectContextMode.CURATED
        )
    }

    @Test
    fun `Conv10 ALL_PROJECTS - recent work`() {
        runConversation(
            listOf(
                "What is Denys working on currently?",
                "How long has he been at GEOSATIS?",
                "What's unique about the victim protection app?"
            ),
            ProjectContextMode.ALL_PROJECTS
        )
    }
}
