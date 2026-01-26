package io.github.devmugi.cv.agent.api

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroqAudioClientTest {

    @BeforeTest
    fun setup() {
        Logger.setMinSeverity(Severity.Assert)
    }

    private fun createMockClient(
        response: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = response,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun transcribeReturnsTextOnSuccess() = runTest {
        val jsonResponse = """{"text": "Hello world"}"""
        val client = GroqAudioClient(createMockClient(jsonResponse), "test-key")

        val result = client.transcribe(ByteArray(100), "test.m4a")

        assertTrue(result.isSuccess)
        assertEquals("Hello world", result.getOrNull())
    }

    @Test
    fun transcribeReturnsRateLimitErrorOn429() = runTest {
        val client = GroqAudioClient(
            createMockClient("Rate limited", HttpStatusCode.TooManyRequests),
            "test-key"
        )

        val result = client.transcribe(ByteArray(100), "test.m4a")

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("rate", ignoreCase = true) == true,
            "Expected rate limit error"
        )
    }

    @Test
    fun transcribeReturnsApiErrorOnOtherFailures() = runTest {
        val client = GroqAudioClient(
            createMockClient("""{"error": {"message": "Invalid audio"}}""", HttpStatusCode.BadRequest),
            "test-key"
        )

        val result = client.transcribe(ByteArray(100), "test.m4a")

        assertTrue(result.isFailure)
    }
}
