package io.github.devmugi.cv.agent.api

import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroqApiClientTest {

    private fun createMockClient(response: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = response,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
                    )
                }
            }
            install(ContentNegotiation) { json() }
        }
    }

    private fun createThrowingMockClient(exception: Exception): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler {
                    throw exception
                }
            }
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun streamsContentFromSuccessfulResponse() = runTest {
        val sseResponse = """data: {"choices":[{"delta":{"content":"Hello"},"index":0}]}

data: {"choices":[{"delta":{"content":" World"},"index":0}]}

data: [DONE]
"""
        val client = GroqApiClient(createMockClient(sseResponse), "test-key")
        val chunks = mutableListOf<String>()
        var completed = false

        client.streamChatCompletion(
            messages = listOf(ChatMessage("user", "Hi")),
            onChunk = { chunks.add(it) },
            onComplete = { completed = true },
            onError = { e -> throw e }
        )

        assertEquals(listOf("Hello", " World"), chunks)
        assertTrue(completed)
    }

    @Test
    fun handlesAuthError() = runTest {
        val client = GroqApiClient(createMockClient("Unauthorized", HttpStatusCode.Unauthorized), "bad-key")
        var error: GroqApiException? = null

        client.streamChatCompletion(
            messages = listOf(ChatMessage("user", "Hi")),
            onChunk = {},
            onComplete = {},
            onError = { e -> error = e }
        )

        assertTrue(error is GroqApiException.AuthError)
    }

    @Test
    fun handlesRateLimitError() = runTest {
        val client = GroqApiClient(createMockClient("Rate limited", HttpStatusCode.TooManyRequests), "test-key")
        var error: GroqApiException? = null

        client.streamChatCompletion(
            messages = listOf(ChatMessage("user", "Hi")),
            onChunk = {},
            onComplete = {},
            onError = { e -> error = e }
        )

        assertTrue(error is GroqApiException.RateLimitError)
    }

    @Test
    fun handlesApiError() = runTest {
        val client = GroqApiClient(
            createMockClient("""{"error":{"message":"Bad request"}}""", HttpStatusCode.BadRequest),
            "test-key"
        )
        var error: GroqApiException? = null

        client.streamChatCompletion(
            messages = listOf(ChatMessage("user", "Hi")),
            onChunk = {},
            onComplete = {},
            onError = { e -> error = e }
        )

        assertTrue(error is GroqApiException.ApiError)
    }

    @Test
    fun handlesMalformedSSEChunkGracefully() = runTest {
        val sseResponse = """data: {"choices":[{"delta":{"content":"Hello"},"index":0}]}

data: {invalid json here}

data: {"choices":[{"delta":{"content":" World"},"index":0}]}

data: [DONE]
"""
        val client = GroqApiClient(createMockClient(sseResponse), "test-key")
        val chunks = mutableListOf<String>()
        var completed = false

        client.streamChatCompletion(
            messages = listOf(ChatMessage("user", "Hi")),
            onChunk = { chunks.add(it) },
            onComplete = { completed = true },
            onError = { e -> throw e }
        )

        // Should skip malformed chunk and continue
        assertEquals(listOf("Hello", " World"), chunks)
        assertTrue(completed)
    }

    @Test
    fun handlesEmptyLinesInSSEStream() = runTest {
        val sseResponse = """

data: {"choices":[{"delta":{"content":"Hello"},"index":0}]}



data: {"choices":[{"delta":{"content":" World"},"index":0}]}

data: [DONE]

"""
        val client = GroqApiClient(createMockClient(sseResponse), "test-key")
        val chunks = mutableListOf<String>()
        var completed = false

        client.streamChatCompletion(
            messages = listOf(ChatMessage("user", "Hi")),
            onChunk = { chunks.add(it) },
            onComplete = { completed = true },
            onError = { e -> throw e }
        )

        assertEquals(listOf("Hello", " World"), chunks)
        assertTrue(completed)
    }

    @Test
    fun skipsChunksWithEmptyDeltaContent() = runTest {
        val sseResponse = """data: {"choices":[{"delta":{"content":"Hello"},"index":0}]}

data: {"choices":[{"delta":{},"index":0}]}

data: {"choices":[{"delta":{"content":null},"index":0}]}

data: {"choices":[{"delta":{"content":" World"},"index":0}]}

data: [DONE]
"""
        val client = GroqApiClient(createMockClient(sseResponse), "test-key")
        val chunks = mutableListOf<String>()

        client.streamChatCompletion(
            messages = listOf(ChatMessage("user", "Hi")),
            onChunk = { chunks.add(it) },
            onComplete = {},
            onError = { e -> throw e }
        )

        assertEquals(listOf("Hello", " World"), chunks)
    }

    @Test
    fun handlesForbiddenAsAuthError() = runTest {
        val client = GroqApiClient(createMockClient("Forbidden", HttpStatusCode.Forbidden), "test-key")
        var error: GroqApiException? = null

        client.streamChatCompletion(
            messages = listOf(ChatMessage("user", "Hi")),
            onChunk = {},
            onComplete = {},
            onError = { e -> error = e }
        )

        assertTrue(error is GroqApiException.AuthError)
        assertEquals(403, (error as GroqApiException.AuthError).code)
    }

    @Test
    fun handlesNetworkException() = runTest {
        val client = GroqApiClient(
            createThrowingMockClient(Exception("Connection refused")),
            "test-key"
        )
        var error: GroqApiException? = null

        client.streamChatCompletion(
            messages = listOf(ChatMessage("user", "Hi")),
            onChunk = {},
            onComplete = {},
            onError = { e -> error = e }
        )

        assertTrue(error is GroqApiException.NetworkError)
    }
}
