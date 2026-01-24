package io.github.devmugi.cv.agent.api

import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.api.models.ChatRequest
import io.github.devmugi.cv.agent.api.models.StreamChunk
import io.github.devmugi.cv.agent.api.tracing.AgentTracer
import io.github.devmugi.cv.agent.api.tracing.TracingSpan
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.json.Json

open class GroqApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val tracer: AgentTracer = AgentTracer.NOOP,
    private val rateLimiter: RateLimiter = RateLimiter.NOOP
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "GroqApiClient"
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        const val MODEL = "llama-3.3-70b-versatile"
        private const val DEFAULT_TEMPERATURE = 0.7
        private const val DEFAULT_MAX_TOKENS = 1024
    }

    open suspend fun streamChatCompletion(
        messages: List<ChatMessage>,
        systemPrompt: String = "",
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (GroqApiException) -> Unit
    ) {
        Logger.d(TAG) { "Starting chat completion - messages: ${messages.size}" }
        val span = tracer.startLlmSpan(
            model = MODEL,
            systemPrompt = systemPrompt,
            messages = messages,
            temperature = DEFAULT_TEMPERATURE,
            maxTokens = DEFAULT_MAX_TOKENS
        )

        try {
            // Wait for rate limiter before making request
            rateLimiter.acquire()

            // Prepend system prompt as first message if provided
            val allMessages = if (systemPrompt.isNotEmpty()) {
                listOf(ChatMessage(role = "system", content = systemPrompt)) + messages
            } else {
                messages
            }

            val response: HttpResponse = httpClient.post(BASE_URL) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(ChatRequest(model = MODEL, messages = allMessages))
            }

            Logger.d(TAG) { "Response status: ${response.status}" }
            when (response.status) {
                HttpStatusCode.OK -> parseStream(response, span, onChunk, onComplete)
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                    val error = GroqApiException.AuthError(response.status.value)
                    Logger.w(TAG) { "Auth error: ${response.status.value}" }
                    span.error(error)
                    onError(error)
                }
                HttpStatusCode.TooManyRequests -> {
                    val retryAfter = response.headers["Retry-After"]?.toIntOrNull()
                    rateLimiter.reportRateLimited(retryAfter)
                    val error = GroqApiException.RateLimitError(retryAfter)
                    Logger.w(TAG) { "Rate limit exceeded, retry-after: $retryAfter" }
                    span.error(error)
                    onError(error)
                }
                else -> {
                    val error = GroqApiException.ApiError(response.status.value, "API error")
                    Logger.w(TAG) { "API error: ${response.status.value}" }
                    span.error(error)
                    onError(error)
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Request failed: ${e.message}" }
            val error = if (e is GroqApiException) e else GroqApiException.NetworkError(e.message ?: "Unknown error")
            span.error(error)
            onError(error)
        }
    }

    private suspend fun parseStream(
        response: HttpResponse,
        span: TracingSpan,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        val fullResponse = StringBuilder()
        val channel = response.bodyAsChannel()

        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") {
                    Logger.d(TAG) { "Stream completed - response length: ${fullResponse.length}" }
                    span.complete(fullResponse.toString())
                    onComplete()
                    break
                }
                try {
                    val chunk = json.decodeFromString(StreamChunk.serializer(), data)
                    chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                        fullResponse.append(content)
                        span.addResponseChunk(content)
                        onChunk(content)
                    }
                } catch (_: Exception) {
                    // Skip malformed chunks
                }
            }
        }
    }
}
