# Phase 3: Groq API Integration - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add Groq API streaming chat integration with CV-aware agent logic.

**Architecture:** Ktor HTTP client streams SSE responses from Groq's OpenAI-compatible API. ChatViewModel manages state via StateFlow, uses SystemPromptBuilder to inject full CV context, and ReferenceExtractor to parse `[Type: ID]` references into tappable chips.

**Tech Stack:** Kotlin Multiplatform, Ktor Client (OkHttp/Darwin), Koin DI, kotlinx.serialization, Turbine (testing)

---

## Task 1: Add Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

**Step 1: Add version entries to libs.versions.toml**

Add after line 22 (after `ktlint-gradle`):
```toml
koin = "3.5.6"
koin-compose = "1.1.5"
```

**Step 2: Add library entries to libs.versions.toml**

Add after line 54 (after `turbine`):
```toml
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin-compose" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
```

**Step 3: Update composeApp/build.gradle.kts commonMain.dependencies**

Add after `implementation(libs.multiplatform.markdown.renderer)` (around line 60):
```kotlin
// DI
implementation(libs.koin.core)
implementation(libs.koin.compose)

// Logging
implementation(libs.ktor.client.logging)
```

**Step 4: Update commonTest.dependencies**

Replace the existing commonTest.dependencies block:
```kotlin
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.ktor.client.mock)
    implementation(libs.kotlinx.coroutines.test)
}
```

**Step 5: Update androidMain.dependencies**

Add after `implementation(libs.ktor.client.okhttp)`:
```kotlin
implementation(libs.koin.android)
```

**Step 6: Sync and verify build**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "feat: add Koin DI and Ktor mock dependencies for Phase 3"
```

---

## Task 2: Create API Models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/models/ChatMessage.kt`
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/models/ChatRequest.kt`
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/models/StreamChunk.kt`
- Test: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/models/ApiModelsTest.kt`

**Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/models/ApiModelsTest.kt`:
```kotlin
package io.github.devmugi.cv.agent.api.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun chatMessageSerializesToJson() {
        val message = ChatMessage(role = "user", content = "Hello")
        val serialized = json.encodeToString(ChatMessage.serializer(), message)
        assertEquals("""{"role":"user","content":"Hello"}""", serialized)
    }

    @Test
    fun chatRequestSerializesWithDefaults() {
        val request = ChatRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(ChatMessage("user", "Hi"))
        )
        val serialized = json.encodeToString(ChatRequest.serializer(), request)
        assert(serialized.contains(""""stream":true"""))
        assert(serialized.contains(""""temperature":0.7"""))
        assert(serialized.contains(""""max_tokens":1024"""))
    }

    @Test
    fun streamChunkParsesContentDelta() {
        val chunkJson = """{"choices":[{"delta":{"content":"Hello"},"index":0}]}"""
        val chunk = json.decodeFromString(StreamChunk.serializer(), chunkJson)
        assertEquals("Hello", chunk.choices.first().delta.content)
    }

    @Test
    fun streamChunkHandlesEmptyDelta() {
        val chunkJson = """{"choices":[{"delta":{},"index":0}]}"""
        val chunk = json.decodeFromString(StreamChunk.serializer(), chunkJson)
        assertEquals(null, chunk.choices.first().delta.content)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "*.ApiModelsTest"`
Expected: FAIL - unresolved reference ChatMessage, ChatRequest, StreamChunk

**Step 3: Create ChatMessage.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/models/ChatMessage.kt`:
```kotlin
package io.github.devmugi.cv.agent.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)
```

**Step 4: Create ChatRequest.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/models/ChatRequest.kt`:
```kotlin
package io.github.devmugi.cv.agent.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Float = 0.7f,
    @SerialName("max_tokens")
    val maxTokens: Int = 1024
)
```

**Step 5: Create StreamChunk.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/models/StreamChunk.kt`:
```kotlin
package io.github.devmugi.cv.agent.api.models

import kotlinx.serialization.Serializable

@Serializable
data class StreamChunk(
    val choices: List<StreamChoice>
)

@Serializable
data class StreamChoice(
    val delta: StreamDelta,
    val index: Int
)

@Serializable
data class StreamDelta(
    val content: String? = null,
    val role: String? = null
)
```

**Step 6: Run test to verify it passes**

Run: `./gradlew :composeApp:allTests --tests "*.ApiModelsTest"`
Expected: PASS (4 tests)

**Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/models/
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/models/
git commit -m "feat: add Groq API request/response models with tests"
```

---

## Task 3: Create GroqApiException

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/GroqApiException.kt`

**Step 1: Create GroqApiException.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/GroqApiException.kt`:
```kotlin
package io.github.devmugi.cv.agent.api

sealed class GroqApiException(message: String) : Exception(message) {
    data class NetworkError(val reason: String) : GroqApiException("Network error: $reason")
    data class AuthError(val code: Int) : GroqApiException("Authentication failed: $code")
    data class RateLimitError(val retryAfter: Int?) : GroqApiException("Rate limit exceeded")
    data class ApiError(val code: Int, override val message: String) : GroqApiException(message)
}
```

**Step 2: Verify build**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/GroqApiException.kt
git commit -m "feat: add GroqApiException sealed class for API error handling"
```

---

## Task 4: Create GroqApiClient with Tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/GroqApiClient.kt`
- Test: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/GroqApiClientTest.kt`

**Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/GroqApiClientTest.kt`:
```kotlin
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
            onError = { throw it }
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
            onError = { error = it }
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
            onError = { error = it }
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
            onError = { error = it }
        )

        assertTrue(error is GroqApiException.ApiError)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "*.GroqApiClientTest"`
Expected: FAIL - unresolved reference GroqApiClient

**Step 3: Create GroqApiClient.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/GroqApiClient.kt`:
```kotlin
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

class GroqApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        const val MODEL = "llama-3.3-70b-versatile"
    }

    suspend fun streamChatCompletion(
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
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :composeApp:allTests --tests "*.GroqApiClientTest"`
Expected: PASS (4 tests)

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/GroqApiClient.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/
git commit -m "feat: add GroqApiClient with SSE streaming and error handling"
```

---

## Task 5: Create Agent Message Model

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/Message.kt`
- Test: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/MessageTest.kt`

**Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/MessageTest.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.data.models.CVReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MessageTest {

    @Test
    fun messageHasUniqueIdByDefault() {
        val msg1 = Message(role = MessageRole.USER, content = "Hello")
        val msg2 = Message(role = MessageRole.USER, content = "Hello")
        assertNotEquals(msg1.id, msg2.id)
    }

    @Test
    fun messageStoresRole() {
        val userMsg = Message(role = MessageRole.USER, content = "Hi")
        val assistantMsg = Message(role = MessageRole.ASSISTANT, content = "Hello")
        assertEquals(MessageRole.USER, userMsg.role)
        assertEquals(MessageRole.ASSISTANT, assistantMsg.role)
    }

    @Test
    fun messageStoresReferences() {
        val ref = CVReference(id = "experience.test", type = "experience", label = "Test")
        val msg = Message(
            role = MessageRole.ASSISTANT,
            content = "Test content",
            references = listOf(ref)
        )
        assertEquals(1, msg.references.size)
        assertEquals("experience.test", msg.references.first().id)
    }

    @Test
    fun messageHasTimestamp() {
        val before = currentTimeMillis()
        val msg = Message(role = MessageRole.USER, content = "Hi")
        val after = currentTimeMillis()
        assertTrue(msg.timestamp in before..after)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "*.MessageTest"`
Expected: FAIL - unresolved reference Message, MessageRole

**Step 3: Create Message.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/Message.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.data.models.CVReference
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

@OptIn(ExperimentalUuidApi::class)
data class Message(
    val id: String = Uuid.random().toString(),
    val role: MessageRole,
    val content: String,
    val references: List<CVReference> = emptyList(),
    val timestamp: Long = currentTimeMillis()
)

expect fun currentTimeMillis(): Long
```

**Step 4: Create platform-specific currentTimeMillis**

Create `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/agent/TimeUtils.android.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
```

Create `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/agent/TimeUtils.ios.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
```

**Step 5: Run test to verify it passes**

Run: `./gradlew :composeApp:allTests --tests "*.MessageTest"`
Expected: PASS (4 tests)

**Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/
git add composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/agent/
git add composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/agent/
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/
git commit -m "feat: add Message model with role, references, and timestamp"
```

---

## Task 6: Create ChatState

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatState.kt`

**Step 1: Create ChatState.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatState.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val error: ChatError? = null,
    val suggestions: List<String> = defaultSuggestions
)

sealed class ChatError {
    data class Network(val message: String) : ChatError()
    data class Api(val message: String) : ChatError()
    data object RateLimit : ChatError()
}

val defaultSuggestions = listOf(
    "What's Denys's experience?",
    "Tell me about his skills",
    "What projects has he worked on?",
    "What are his achievements?"
)
```

**Step 2: Verify build**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatState.kt
git commit -m "feat: add ChatState data class with error handling and suggestions"
```

---

## Task 7: Create ReferenceExtractor with Tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractor.kt`
- Test: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractorTest.kt`

**Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractorTest.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.data.models.CVReference
import io.github.devmugi.cv.agent.data.repository.CVRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReferenceExtractorTest {

    private val mockRepository = object : CVRepository() {
        override fun resolveReference(id: String): CVReference? {
            return when (id) {
                "experience.geosatis" -> CVReference("experience.geosatis", "experience", "GEOSATIS")
                "project.mtg-deckbuilder" -> CVReference("project.mtg-deckbuilder", "project", "MTG DeckBuilder")
                "skills.ai-dev" -> CVReference("skills.ai-dev", "skill", "AI Development")
                else -> null
            }
        }
    }

    private val extractor = ReferenceExtractor(mockRepository)

    @Test
    fun extractsSingleReference() {
        val input = "Denys worked at [Experience: experience.geosatis] as a developer."
        val result = extractor.extract(input)

        assertEquals("Denys worked at GEOSATIS as a developer.", result.cleanedContent)
        assertEquals(1, result.references.size)
        assertEquals("experience.geosatis", result.references.first().id)
    }

    @Test
    fun extractsMultipleReferences() {
        val input = "He built [Project: project.mtg-deckbuilder] using [Skill: skills.ai-dev] skills."
        val result = extractor.extract(input)

        assertEquals("He built MTG DeckBuilder using AI Development skills.", result.cleanedContent)
        assertEquals(2, result.references.size)
    }

    @Test
    fun handlesUnknownIdGracefully() {
        val input = "Denys worked at [Experience: experience.unknown] company."
        val result = extractor.extract(input)

        assertEquals("Denys worked at [Experience: experience.unknown] company.", result.cleanedContent)
        assertTrue(result.references.isEmpty())
    }

    @Test
    fun handlesMalformedBrackets() {
        val input = "This has [incomplete bracket and normal text."
        val result = extractor.extract(input)

        assertEquals(input, result.cleanedContent)
        assertTrue(result.references.isEmpty())
    }

    @Test
    fun deduplicatesReferences() {
        val input = "[Experience: experience.geosatis] and again [Experience: experience.geosatis]."
        val result = extractor.extract(input)

        assertEquals("GEOSATIS and again GEOSATIS.", result.cleanedContent)
        assertEquals(1, result.references.size)
    }

    @Test
    fun handlesTextWithNoReferences() {
        val input = "Just regular text without any references."
        val result = extractor.extract(input)

        assertEquals(input, result.cleanedContent)
        assertTrue(result.references.isEmpty())
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "*.ReferenceExtractorTest"`
Expected: FAIL - unresolved reference ReferenceExtractor

**Step 3: Make CVRepository open for testing**

Modify `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVRepository.kt`:
Change `class CVRepository` to `open class CVRepository` and make `resolveReference` open:
```kotlin
open fun resolveReference(id: String): CVReference? {
```

**Step 4: Create ReferenceExtractor.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractor.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.data.models.CVReference
import io.github.devmugi.cv.agent.data.repository.CVRepository

data class ExtractionResult(
    val cleanedContent: String,
    val references: List<CVReference>
)

class ReferenceExtractor(
    private val repository: CVRepository
) {
    private val referencePattern = """\[(Experience|Project|Skill|Achievement|Education):\s*([^\]]+)\]""".toRegex()

    fun extract(content: String): ExtractionResult {
        val references = mutableListOf<CVReference>()
        var cleanedContent = content

        referencePattern.findAll(content).forEach { match ->
            val id = match.groupValues[2].trim()
            val resolved = repository.resolveReference(id)
            if (resolved != null) {
                cleanedContent = cleanedContent.replace(match.value, resolved.label)
                if (references.none { it.id == resolved.id }) {
                    references.add(resolved)
                }
            }
        }

        return ExtractionResult(cleanedContent, references)
    }
}
```

**Step 5: Run test to verify it passes**

Run: `./gradlew :composeApp:allTests --tests "*.ReferenceExtractorTest"`
Expected: PASS (6 tests)

**Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractor.kt
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVRepository.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractorTest.kt
git commit -m "feat: add ReferenceExtractor to parse [Type: ID] references from text"
```

---

## Task 8: Create SystemPromptBuilder with Tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilder.kt`
- Test: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilderTest.kt`

**Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilderTest.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.models.Education
import io.github.devmugi.cv.agent.data.models.PersonalInfo
import io.github.devmugi.cv.agent.data.models.SkillCategory
import io.github.devmugi.cv.agent.data.models.WorkExperience
import io.github.devmugi.cv.agent.data.models.Project
import io.github.devmugi.cv.agent.data.models.Achievement
import kotlin.test.Test
import kotlin.test.assertTrue

class SystemPromptBuilderTest {

    private val testCVData = CVData(
        personalInfo = PersonalInfo(
            name = "Denys Honcharenko",
            location = "Lausanne, Switzerland",
            email = "test@example.com",
            phone = "+1234567890",
            linkedin = "linkedin.com/in/test",
            github = "github.com/test",
            portfolio = "portfolio.test"
        ),
        summary = "Experienced Software Engineer",
        skills = listOf(
            SkillCategory("skills.ai-dev", "AI Development", "Advanced", listOf("Claude API", "MCP"))
        ),
        experience = listOf(
            WorkExperience(
                "experience.geosatis", "Senior Dev", "GEOSATIS", "2023-Present",
                "Built apps", listOf("Led team"), listOf("Kotlin"), true
            )
        ),
        projects = listOf(
            Project("project.mtg", "MTG App", "Mobile", "Card app", listOf("KMP"), null, true)
        ),
        achievements = listOf(
            Achievement("achievement.claude", "Claude Power User", "Anthropic", "2024", "Certified")
        ),
        education = Education("BSc", "Computer Science", "University")
    )

    private val builder = SystemPromptBuilder()

    @Test
    fun promptContainsPersonaInstructions() {
        val prompt = builder.build(testCVData)
        assertTrue(prompt.contains("Denys Honcharenko"))
        assertTrue(prompt.contains("third person"))
    }

    @Test
    fun promptContainsReferenceFormatInstructions() {
        val prompt = builder.build(testCVData)
        assertTrue(prompt.contains("[Experience:"))
        assertTrue(prompt.contains("[Project:"))
    }

    @Test
    fun promptContainsAllCVSections() {
        val prompt = builder.build(testCVData)
        assertTrue(prompt.contains("PERSONAL INFO"))
        assertTrue(prompt.contains("SKILLS"))
        assertTrue(prompt.contains("WORK EXPERIENCE"))
        assertTrue(prompt.contains("PROJECTS"))
        assertTrue(prompt.contains("ACHIEVEMENTS"))
        assertTrue(prompt.contains("EDUCATION"))
    }

    @Test
    fun promptContainsActualCVData() {
        val prompt = builder.build(testCVData)
        assertTrue(prompt.contains("GEOSATIS"))
        assertTrue(prompt.contains("experience.geosatis"))
        assertTrue(prompt.contains("AI Development"))
        assertTrue(prompt.contains("Claude Power User"))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "*.SystemPromptBuilderTest"`
Expected: FAIL - unresolved reference SystemPromptBuilder

**Step 3: Create SystemPromptBuilder.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilder.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.data.models.CVData

class SystemPromptBuilder {

    fun build(cvData: CVData): String = buildString {
        appendLine("""
You are an AI assistant for Denys Honcharenko's portfolio. Answer questions about Denys in third person. Be helpful, professional, and concise.

When mentioning specific items from Denys's background, use this format:
[Type: ID] where Type is one of: Experience, Project, Skill, Achievement, Education

Examples: [Experience: experience.geosatis], [Project: project.mtg-deckbuilder]

---
        """.trimIndent())

        appendLine()
        appendLine("PERSONAL INFO:")
        appendLine("Name: ${cvData.personalInfo.name}")
        appendLine("Location: ${cvData.personalInfo.location}")
        appendLine("Email: ${cvData.personalInfo.email}")
        appendLine("LinkedIn: ${cvData.personalInfo.linkedin}")
        appendLine("GitHub: ${cvData.personalInfo.github}")
        appendLine()
        appendLine("Summary: ${cvData.summary}")

        appendLine()
        appendLine("SKILLS:")
        cvData.skills.forEach { skill ->
            appendLine("- ${skill.category} (ID: ${skill.id}): ${skill.skills.joinToString(", ")}")
        }

        appendLine()
        appendLine("WORK EXPERIENCE:")
        cvData.experience.forEach { exp ->
            appendLine("- ${exp.company} (ID: ${exp.id}): ${exp.title}, ${exp.period}")
            appendLine("  ${exp.description}")
            exp.highlights.forEach { highlight ->
                appendLine("  * $highlight")
            }
        }

        appendLine()
        appendLine("PROJECTS:")
        cvData.projects.forEach { project ->
            appendLine("- ${project.name} (ID: ${project.id}): ${project.type}")
            appendLine("  ${project.description}")
        }

        appendLine()
        appendLine("ACHIEVEMENTS:")
        cvData.achievements.forEach { achievement ->
            appendLine("- ${achievement.title} (ID: ${achievement.id}): ${achievement.year}")
            appendLine("  ${achievement.description}")
        }

        appendLine()
        appendLine("EDUCATION:")
        appendLine("- ${cvData.education.degree} in ${cvData.education.field}")
        appendLine("  ${cvData.education.institution}")
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :composeApp:allTests --tests "*.SystemPromptBuilderTest"`
Expected: PASS (4 tests)

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilder.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilderTest.kt
git commit -m "feat: add SystemPromptBuilder to generate CV-aware system prompts"
```

---

## Task 9: Create ChatViewModel with Tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt`
- Test: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.GroqApiException
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.models.CVReference
import io.github.devmugi.cv.agent.data.models.Education
import io.github.devmugi.cv.agent.data.models.PersonalInfo
import io.github.devmugi.cv.agent.data.repository.CVRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val minimalCVData = CVData(
        personalInfo = PersonalInfo("Test", "Location", "email", "phone", "linkedin", "github", "portfolio"),
        summary = "Summary",
        skills = emptyList(),
        experience = emptyList(),
        projects = emptyList(),
        achievements = emptyList(),
        education = Education("BSc", "CS", "Uni")
    )

    private lateinit var fakeApiClient: FakeGroqApiClient
    private lateinit var fakeRepository: FakeCVRepository
    private lateinit var viewModel: ChatViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeApiClient = FakeGroqApiClient()
        fakeRepository = FakeCVRepository(minimalCVData)
        viewModel = ChatViewModel(
            apiClient = fakeApiClient,
            repository = fakeRepository,
            promptBuilder = SystemPromptBuilder(),
            referenceExtractor = ReferenceExtractor(fakeRepository)
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateHasSuggestions() {
        assertEquals(defaultSuggestions, viewModel.state.value.suggestions)
        assertTrue(viewModel.state.value.messages.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun sendMessageAddsUserMessage() = runTest {
        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        val messages = viewModel.state.value.messages
        assertTrue(messages.any { it.role == MessageRole.USER && it.content == "Hello" })
    }

    @Test
    fun sendMessageTriggersLoading() = runTest {
        fakeApiClient.delayResponse = true
        viewModel.sendMessage("Hello")

        // Check loading state before API completes
        testDispatcher.scheduler.advanceTimeBy(50)
        assertTrue(viewModel.state.value.isLoading || viewModel.state.value.isStreaming)
    }

    @Test
    fun successfulResponseAddsAssistantMessage() = runTest {
        fakeApiClient.responseChunks = listOf("Hello", " there")
        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        val messages = viewModel.state.value.messages
        val assistantMsg = messages.find { it.role == MessageRole.ASSISTANT }
        assertNotNull(assistantMsg)
        assertEquals("Hello there", assistantMsg.content)
    }

    @Test
    fun errorStateSetsError() = runTest {
        fakeApiClient.shouldFail = GroqApiException.NetworkError("Connection failed")
        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.error is ChatError.Network)
    }

    @Test
    fun clearErrorRemovesError() = runTest {
        fakeApiClient.shouldFail = GroqApiException.NetworkError("Test")
        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        viewModel.clearError()
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun onSuggestionClickedSendsMessage() = runTest {
        val suggestion = "What's Denys's experience?"
        viewModel.onSuggestionClicked(suggestion)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.messages.any { it.content == suggestion })
    }
}

// Test doubles
class FakeGroqApiClient : GroqApiClient(
    httpClient = io.ktor.client.HttpClient {},
    apiKey = "fake"
) {
    var responseChunks: List<String> = listOf("Test response")
    var shouldFail: GroqApiException? = null
    var delayResponse = false

    override suspend fun streamChatCompletion(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (GroqApiException) -> Unit
    ) {
        if (delayResponse) {
            kotlinx.coroutines.delay(100)
        }
        shouldFail?.let {
            onError(it)
            return
        }
        responseChunks.forEach { onChunk(it) }
        onComplete()
    }
}

class FakeCVRepository(private val cvData: CVData) : CVRepository() {
    fun getCVData(): CVData = cvData
    override fun resolveReference(id: String): CVReference? = null
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "*.ChatViewModelTest"`
Expected: FAIL - unresolved reference ChatViewModel

**Step 3: Make GroqApiClient open for testing**

Modify `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/GroqApiClient.kt`:
Change `class GroqApiClient` to `open class GroqApiClient` and make `streamChatCompletion` open:
```kotlin
open suspend fun streamChatCompletion(
```

**Step 4: Create ChatViewModel.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt`:
```kotlin
package io.github.devmugi.cv.agent.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.GroqApiException
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.repository.CVRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val apiClient: GroqApiClient,
    private val repository: CVRepository,
    private val promptBuilder: SystemPromptBuilder,
    private val referenceExtractor: ReferenceExtractor,
    private val cvDataProvider: () -> CVData? = { null }
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var lastUserMessage: String? = null

    companion object {
        private const val MAX_HISTORY = 10
    }

    fun sendMessage(content: String) {
        lastUserMessage = content
        val userMessage = Message(role = MessageRole.USER, content = content)

        _state.update { current ->
            current.copy(
                messages = current.messages + userMessage,
                isLoading = true,
                error = null,
                suggestions = emptyList()
            )
        }

        viewModelScope.launch {
            streamResponse()
        }
    }

    fun onSuggestionClicked(suggestion: String) {
        sendMessage(suggestion)
    }

    fun retry() {
        lastUserMessage?.let { sendMessage(it) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun streamResponse() {
        val cvData = cvDataProvider()
        val systemPrompt = cvData?.let { promptBuilder.build(it) } ?: ""

        val apiMessages = buildApiMessages(systemPrompt)
        var streamedContent = ""

        _state.update { it.copy(isLoading = false, isStreaming = true, streamingContent = "") }

        apiClient.streamChatCompletion(
            messages = apiMessages,
            onChunk = { chunk ->
                streamedContent += chunk
                _state.update { it.copy(streamingContent = streamedContent) }
            },
            onComplete = {
                val extractionResult = referenceExtractor.extract(streamedContent)
                val assistantMessage = Message(
                    role = MessageRole.ASSISTANT,
                    content = extractionResult.cleanedContent,
                    references = extractionResult.references
                )
                _state.update { current ->
                    current.copy(
                        messages = current.messages + assistantMessage,
                        isStreaming = false,
                        streamingContent = ""
                    )
                }
            },
            onError = { exception ->
                val error = when (exception) {
                    is GroqApiException.NetworkError -> ChatError.Network(exception.reason)
                    is GroqApiException.RateLimitError -> ChatError.RateLimit
                    is GroqApiException.AuthError -> ChatError.Api("Authentication failed")
                    is GroqApiException.ApiError -> ChatError.Api(exception.message)
                }
                _state.update { it.copy(isLoading = false, isStreaming = false, error = error) }
            }
        )
    }

    private fun buildApiMessages(systemPrompt: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        if (systemPrompt.isNotEmpty()) {
            messages.add(ChatMessage(role = "system", content = systemPrompt))
        }

        val recentMessages = _state.value.messages.takeLast(MAX_HISTORY)
        recentMessages.forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            messages.add(ChatMessage(role = role, content = msg.content))
        }

        return messages
    }
}
```

**Step 5: Run test to verify it passes**

Run: `./gradlew :composeApp:allTests --tests "*.ChatViewModelTest"`
Expected: PASS (7 tests)

**Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/GroqApiClient.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt
git commit -m "feat: add ChatViewModel with streaming, state management, and error handling"
```

---

## Task 10: Set Up Koin DI Module

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/AppModule.kt`
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/HttpEngineFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/di/HttpEngineFactory.android.kt`
- Create: `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/di/HttpEngineFactory.ios.kt`

**Step 1: Create HttpEngineFactory expect/actual**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/HttpEngineFactory.kt`:
```kotlin
package io.github.devmugi.cv.agent.di

import io.ktor.client.engine.HttpClientEngineFactory

expect val httpEngineFactory: HttpClientEngineFactory<*>
```

Create `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/di/HttpEngineFactory.android.kt`:
```kotlin
package io.github.devmugi.cv.agent.di

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual val httpEngineFactory: HttpClientEngineFactory<*> = OkHttp
```

Create `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/di/HttpEngineFactory.ios.kt`:
```kotlin
package io.github.devmugi.cv.agent.di

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual val httpEngineFactory: HttpClientEngineFactory<*> = Darwin
```

**Step 2: Create AppModule.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/AppModule.kt`:
```kotlin
package io.github.devmugi.cv.agent.di

import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.agent.ReferenceExtractor
import io.github.devmugi.cv.agent.agent.SystemPromptBuilder
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.data.repository.CVDataLoader
import io.github.devmugi.cv.agent.data.repository.CVRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(apiKey: String) = module {
    // HTTP Client
    single {
        HttpClient(httpEngineFactory) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Logging) {
                level = LogLevel.BODY
            }
        }
    }

    // API Layer
    single { GroqApiClient(get(), apiKey) }

    // Data Layer
    single { CVDataLoader() }
    single { CVRepository(get()) }

    // Agent Layer
    single { SystemPromptBuilder() }
    single { ReferenceExtractor(get()) }

    viewModel { params ->
        ChatViewModel(
            apiClient = get(),
            repository = get(),
            promptBuilder = get(),
            referenceExtractor = get(),
            cvDataProvider = params.getOrNull()
        )
    }
}
```

**Step 3: Verify build**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/
git add composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/di/
git add composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/di/
git commit -m "feat: add Koin DI module with platform-specific HTTP engines"
```

---

## Task 11: Configure BuildConfig for API Key

**Files:**
- Modify: `composeApp/build.gradle.kts`
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/BuildConfig.kt`
- Create: `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/BuildConfig.android.kt`
- Create: `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/BuildConfig.ios.kt`

**Step 1: Create expect/actual BuildConfig**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/BuildConfig.kt`:
```kotlin
package io.github.devmugi.cv.agent

expect object BuildConfig {
    val GROQ_API_KEY: String
}
```

Create `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/BuildConfig.android.kt`:
```kotlin
package io.github.devmugi.cv.agent

actual object BuildConfig {
    actual val GROQ_API_KEY: String = io.github.devmugi.cv.agent.AndroidBuildConfig.GROQ_API_KEY
}
```

Create `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/BuildConfig.ios.kt`:
```kotlin
package io.github.devmugi.cv.agent

actual object BuildConfig {
    actual val GROQ_API_KEY: String = getGroqApiKey()
}

private fun getGroqApiKey(): String {
    // Read from Info.plist or environment
    return platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("GROQ_API_KEY") as? String ?: ""
}
```

**Step 2: Update Android build.gradle.kts**

Add to `composeApp/build.gradle.kts` inside the `android { }` block, after `compileOptions`:
```kotlin
buildFeatures {
    buildConfig = true
}

buildTypes {
    all {
        buildConfigField("String", "GROQ_API_KEY", "\"${project.findProperty("GROQ_API_KEY") ?: ""}\"")
    }
}
```

**Step 3: Rename Android BuildConfig reference**

Update `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/BuildConfig.android.kt`:
```kotlin
package io.github.devmugi.cv.agent

actual object BuildConfig {
    actual val GROQ_API_KEY: String = io.github.devmugi.cv.agent.BuildConfig.GROQ_API_KEY
}
```

Note: This creates a naming conflict. Instead, use a different approach:

Create `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/PlatformConfig.android.kt`:
```kotlin
package io.github.devmugi.cv.agent

actual object BuildConfig {
    actual val GROQ_API_KEY: String
        get() = io.github.devmugi.cv.agent.BuildConfig.GROQ_API_KEY
}
```

Actually, let's simplify by using a different naming scheme:

**Step 3 (revised): Use GroqConfig instead**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/GroqConfig.kt`:
```kotlin
package io.github.devmugi.cv.agent

expect object GroqConfig {
    val apiKey: String
}
```

Create `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/GroqConfig.android.kt`:
```kotlin
package io.github.devmugi.cv.agent

actual object GroqConfig {
    actual val apiKey: String
        get() = BuildConfig.GROQ_API_KEY
}
```

Create `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/GroqConfig.ios.kt`:
```kotlin
package io.github.devmugi.cv.agent

import platform.Foundation.NSBundle

actual object GroqConfig {
    actual val apiKey: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("GROQ_API_KEY") as? String ?: ""
}
```

**Step 4: Update AppModule to use GroqConfig**

Modify `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/AppModule.kt`:
Replace `fun appModule(apiKey: String)` with:
```kotlin
val appModule = module {
    // ... same content but use GroqConfig.apiKey
    single { GroqApiClient(get(), GroqConfig.apiKey) }
    // ...
}
```

**Step 5: Add GROQ_API_KEY to local gradle.properties**

Add to `gradle.properties` (git-ignored):
```properties
GROQ_API_KEY=your-api-key-here
```

**Step 6: Verify build**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add composeApp/build.gradle.kts
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/GroqConfig.kt
git add composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/GroqConfig.android.kt
git add composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/GroqConfig.ios.kt
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/AppModule.kt
git commit -m "feat: add BuildConfig for Groq API key injection"
```

---

## Task 12: Run All Tests and Quality Checks

**Step 1: Run all tests**

Run: `./gradlew :composeApp:allTests`
Expected: All tests pass (existing 15 + new ~25 = ~40 tests)

**Step 2: Run quality checks**

Run: `./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL (ktlint + detekt pass)

**Step 3: Fix any issues**

If ktlint fails, run: `./gradlew ktlintFormat`
If detekt fails, address the reported issues.

**Step 4: Final commit**

```bash
git add -A
git commit -m "chore: fix lint and style issues"
```

---

## Task 13: Create Phase 3 Verification Document

**Files:**
- Create: `docs/verification/phase3-completion.md`

**Step 1: Create verification document**

Create `docs/verification/phase3-completion.md`:
```markdown
# Phase 3 Completion Verification

## Checklist

- [ ] Groq API client successfully streams responses
- [ ] ChatViewModel manages conversation state correctly
- [ ] System prompt includes full CV context
- [ ] References extracted and resolved from responses
- [ ] Koin DI wires all components
- [ ] All tests passing with 80%+ coverage
- [ ] Quality gates (detekt, ktlint) passing

## Test Results

```
./gradlew :composeApp:allTests
# Paste output here
```

## Quality Check Results

```
./gradlew qualityCheck
# Paste output here
```

## Files Created

### API Layer
- `api/GroqApiClient.kt` - HTTP client with SSE streaming
- `api/GroqApiException.kt` - Error types
- `api/models/ChatMessage.kt` - API message DTO
- `api/models/ChatRequest.kt` - API request DTO
- `api/models/StreamChunk.kt` - SSE chunk parsing

### Agent Layer
- `agent/ChatViewModel.kt` - State management
- `agent/ChatState.kt` - UI state
- `agent/Message.kt` - Chat message model
- `agent/SystemPromptBuilder.kt` - System prompt generation
- `agent/ReferenceExtractor.kt` - Reference parsing

### DI Layer
- `di/AppModule.kt` - Koin modules
- `di/HttpEngineFactory.kt` - Platform HTTP engines

### Config
- `GroqConfig.kt` - API key provider

## Verification Date

[Date]

## Verified By

[Name]
```

**Step 2: Commit**

```bash
git add docs/verification/phase3-completion.md
git commit -m "docs: add Phase 3 completion verification template"
```

---

## Summary

| Task | Description | Tests |
|------|-------------|-------|
| 1 | Add Dependencies | - |
| 2 | API Models | 4 |
| 3 | GroqApiException | - |
| 4 | GroqApiClient | 4 |
| 5 | Message Model | 4 |
| 6 | ChatState | - |
| 7 | ReferenceExtractor | 6 |
| 8 | SystemPromptBuilder | 4 |
| 9 | ChatViewModel | 7 |
| 10 | Koin DI Module | - |
| 11 | BuildConfig | - |
| 12 | Quality Checks | - |
| 13 | Verification Doc | - |

**Total New Tests:** ~29
**Total Tests After Phase 3:** ~44

**Estimated Commits:** 13
