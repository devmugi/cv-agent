package io.github.devmugi.cv.agent.api

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class GroqAudioClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "GroqAudioClient"
        private const val BASE_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
        private const val LOG_PREVIEW_LENGTH = 50
        const val DEFAULT_MODEL = "whisper-large-v3-turbo"
    }

    @Serializable
    private data class TranscriptionResponse(val text: String)

    @Serializable
    private data class ErrorResponse(val error: ErrorDetail? = null)

    @Serializable
    private data class ErrorDetail(val message: String? = null)

    suspend fun transcribe(
        audioData: ByteArray,
        fileName: String,
        language: String = "en"
    ): Result<String> {
        Logger.d(TAG) { "Transcribing audio: ${audioData.size} bytes, file: $fileName" }

        return try {
            val response = httpClient.submitFormWithBinaryData(
                url = BASE_URL,
                formData = formData {
                    append("file", audioData, Headers.build {
                        append(HttpHeaders.ContentType, "audio/mp4")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                    append("model", model)
                    append("response_format", "json")
                    append("language", language)
                }
            ) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.bodyAsText()
                    val transcription = json.decodeFromString<TranscriptionResponse>(body)
                    Logger.d(TAG) { "Transcription successful: ${transcription.text.take(LOG_PREVIEW_LENGTH)}..." }
                    Result.success(transcription.text)
                }
                HttpStatusCode.TooManyRequests -> {
                    Logger.w(TAG) { "Rate limit exceeded" }
                    Result.failure(Exception("Rate limit exceeded, please try again"))
                }
                HttpStatusCode.PayloadTooLarge -> {
                    Logger.w(TAG) { "File too large" }
                    Result.failure(Exception("Audio file exceeds size limit"))
                }
                else -> {
                    val body = response.bodyAsText()
                    val errorMessage = try {
                        json.decodeFromString<ErrorResponse>(body).error?.message
                    } catch (_: Exception) {
                        null
                    } ?: "Transcription failed: ${response.status}"
                    Logger.w(TAG) { "API error: $errorMessage" }
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Transcription request failed" }
            Result.failure(e)
        }
    }
}
