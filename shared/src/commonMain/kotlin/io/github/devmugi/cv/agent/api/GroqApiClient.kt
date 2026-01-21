package io.github.devmugi.cv.agent.api

import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.api.models.ChatRequest
import io.github.devmugi.cv.agent.api.models.StreamChunk
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
    private val apiKey: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        const val MODEL = "llama-3.3-70b-versatile"
    }

    open suspend fun streamChatCompletion(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (GroqApiException) -> Unit
    ) {
        try {
            val response: HttpResponse = httpClient.post(BASE_URL) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(ChatRequest(model = MODEL, messages = messages))
            }

            when (response.status) {
                HttpStatusCode.OK -> parseStream(response, onChunk, onComplete)
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                    onError(GroqApiException.AuthError(response.status.value))
                HttpStatusCode.TooManyRequests ->
                    onError(GroqApiException.RateLimitError(null))
                else ->
                    onError(GroqApiException.ApiError(response.status.value, "API error"))
            }
        } catch (e: Exception) {
            if (e is GroqApiException) {
                onError(e)
            } else {
                onError(GroqApiException.NetworkError(e.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun parseStream(
        response: HttpResponse,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        val channel = response.bodyAsChannel()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") {
                    onComplete()
                    break
                }
                try {
                    val chunk = json.decodeFromString(StreamChunk.serializer(), data)
                    chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                        onChunk(content)
                    }
                } catch (_: Exception) {
                    // Skip malformed chunks
                }
            }
        }
    }
}
