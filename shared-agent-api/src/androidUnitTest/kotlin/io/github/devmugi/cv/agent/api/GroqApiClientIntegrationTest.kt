package io.github.devmugi.cv.agent.api

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.api.tracing.OpenTelemetryAgentTracer
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Integration tests that make real Groq API calls with OpenTelemetry tracing.
 * Traces are sent to Phoenix at localhost:6006.
 *
 * **NOTE:** These tests are EXCLUDED from regular test runs to avoid rate limits.
 *
 * Prerequisites:
 * 1. Start Phoenix: `phoenix serve`
 * 2. Set GROQ_API_KEY in environment or local.properties
 *
 * Run integration tests:
 * ```
 * ./gradlew :shared-agent-api:evaluationTests
 * ```
 *
 * Run with custom delay (default 2000ms):
 * ```
 * GROQ_TEST_DELAY_MS=3000 ./gradlew :shared-agent-api:evaluationTests
 * ```
 *
 * View traces: http://localhost:6006
 */
@Suppress("FunctionNaming", "MagicNumber")
class GroqApiClientIntegrationTest {

    companion object {
        /**
         * Delay between tests to avoid hitting Groq rate limits.
         * Configure via GROQ_TEST_DELAY_MS environment variable.
         * Default: 2000ms (2 seconds). Set to 0 to disable.
         */
        private val TEST_DELAY_MS = System.getenv("GROQ_TEST_DELAY_MS")?.toLongOrNull() ?: 2000L
    }

    private lateinit var tracer: OpenTelemetryAgentTracer
    private lateinit var apiClient: GroqApiClient
    private var apiKey: String = ""

    @Before
    fun setup() {
        // Rate limit protection: delay between tests
        if (TEST_DELAY_MS > 0) {
            Thread.sleep(TEST_DELAY_MS)
        }

        // Disable logging in tests to avoid android.util.Log dependency
        Logger.setMinSeverity(Severity.Assert)

        // Try environment variable first, then local.properties
        apiKey = System.getenv("GROQ_API_KEY") ?: loadApiKeyFromProperties() ?: ""

        // Skip tests if no API key available
        assumeTrue("GROQ_API_KEY not set - skipping integration tests", apiKey.isNotEmpty())

        // Create tracer pointing to Phoenix (HTTP OTLP endpoint)
        tracer = OpenTelemetryAgentTracer.create(
            endpoint = "http://localhost:6006/v1/traces",
            serviceName = "cv-agent-integration-test"
        )

        // Create real HTTP client with OkHttp engine
        val httpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Create API client with tracing enabled
        apiClient = GroqApiClient(
            httpClient = httpClient,
            apiKey = apiKey,
            tracer = tracer
        )
    }

    @After
    fun tearDown() {
        if (::tracer.isInitialized) {
            // Flush traces to Phoenix before test ends
            tracer.flush()
            // Give some time for traces to be sent
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
            null
        }
    }

    @Test
    fun `simple chat completion with tracing`() = runBlocking {
        val messages = listOf(
            ChatMessage(role = "user", content = "Say hello in exactly one word")
        )

        val latch = CountDownLatch(1)
        var response = ""
        var error: GroqApiException? = null

        apiClient.streamChatCompletion(
            messages = messages,
            systemPrompt = "You are a helpful assistant. Be extremely brief.",
            onChunk = { response += it },
            onComplete = { latch.countDown() },
            onError = {
                error = it
                latch.countDown()
            }
        )

        assertTrue(latch.await(30, TimeUnit.SECONDS), "API call timed out")

        if (error != null) {
            fail("API error: ${error!!.message}")
        }

        assertNotNull(response)
        assertTrue(response.isNotEmpty(), "Response should not be empty")
        println("Response: $response")
        // Trace now visible in Phoenix at localhost:6006
    }

    @Test
    fun `chat with CV context - concise prompt`() = runBlocking {
        val systemPrompt = """
            You are an AI assistant for Denys Honcharenko's portfolio.
            Answer questions about Denys in third person.
            Be extremely brief - one sentence max.

            Skills: Kotlin, Android, KMP, Compose Multiplatform
            Projects: GeoSatis (GPS tracking), McDonald's (mobile ordering)
        """.trimIndent()

        val messages = listOf(
            ChatMessage(role = "user", content = "What does Denys specialize in?")
        )

        val latch = CountDownLatch(1)
        var response = ""

        apiClient.streamChatCompletion(
            messages = messages,
            systemPrompt = systemPrompt,
            onChunk = { response += it },
            onComplete = { latch.countDown() },
            onError = { latch.countDown() }
        )

        assertTrue(latch.await(30, TimeUnit.SECONDS), "API call timed out")
        assertTrue(response.isNotEmpty())
        println("Concise response: $response")
    }

    @Test
    fun `chat with CV context - detailed prompt`() = runBlocking {
        val systemPrompt = """
            You are an AI assistant for Denys Honcharenko's portfolio.
            Answer questions about Denys in third person.
            Provide comprehensive, detailed answers with specific examples.

            Skills: Kotlin, Android, KMP, Compose Multiplatform, Jetpack Compose
            Projects:
            - GeoSatis: GPS tracking for law enforcement
            - McDonald's: Mobile ordering platform
            - Adidas GMR: Smart insole integration
        """.trimIndent()

        val messages = listOf(
            ChatMessage(role = "user", content = "What does Denys specialize in?")
        )

        val latch = CountDownLatch(1)
        var response = ""

        apiClient.streamChatCompletion(
            messages = messages,
            systemPrompt = systemPrompt,
            onChunk = { response += it },
            onComplete = { latch.countDown() },
            onError = { latch.countDown() }
        )

        assertTrue(latch.await(30, TimeUnit.SECONDS), "API call timed out")
        assertTrue(response.isNotEmpty())
        println("Detailed response: $response")
    }
}
