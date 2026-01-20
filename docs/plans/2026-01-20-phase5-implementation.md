# Phase 5: Business Logic Test Coverage Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add 18 new unit tests to maximize business logic coverage for ChatViewModel, GroqApiClient, and ReferenceExtractor.

**Architecture:** Extend existing test files with new test cases. Enhance test doubles (FakeGroqApiClient, mock repository) to support new scenarios. Follow TDD - tests should pass immediately since we're testing existing implementation.

**Tech Stack:** Kotlin Test, Ktor MockEngine, Coroutines Test

---

## Task 1: ChatViewModel - Retry Tests (2 tests)

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Add test for retry resending last message**

Add this test after `onSuggestionClickedSendsMessage`:

```kotlin
@Test
fun retryResendsLastUserMessage() = runTest {
    fakeApiClient.shouldFail = GroqApiException.NetworkError("First attempt failed")
    viewModel.sendMessage("Original question")
    advanceUntilIdle()

    // Clear the error and fix the API
    fakeApiClient.shouldFail = null
    fakeApiClient.responseChunks = listOf("Success response")
    viewModel.retry()
    advanceUntilIdle()

    val messages = viewModel.state.value.messages
    val userMessages = messages.filter { it.role == MessageRole.USER }
    assertEquals(2, userMessages.size)
    assertTrue(userMessages.all { it.content == "Original question" })
}
```

**Step 2: Run test to verify it passes**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ChatViewModelTest.retryResendsLastUserMessage"`

Expected: PASS

**Step 3: Add test for retry with no previous message**

Add this test:

```kotlin
@Test
fun retryDoesNothingWhenNoPreviousMessage() = runTest {
    val initialState = viewModel.state.value
    viewModel.retry()
    advanceUntilIdle()

    assertEquals(initialState.messages, viewModel.state.value.messages)
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ChatViewModelTest.retryDoesNothingWhenNoPreviousMessage"`

Expected: PASS

**Step 5: Commit**

```bash
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt
git commit -m "test(agent): add ChatViewModel retry tests"
```

---

## Task 2: ChatViewModel - Error Mapping Tests (3 tests)

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Add rate limit error mapping test**

```kotlin
@Test
fun rateLimitErrorMappedToChatErrorRateLimit() = runTest {
    fakeApiClient.shouldFail = GroqApiException.RateLimitError(retryAfter = 30)
    viewModel.sendMessage("Hi")
    advanceUntilIdle()

    assertTrue(viewModel.state.value.error is ChatError.RateLimit)
}
```

**Step 2: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ChatViewModelTest.rateLimitErrorMappedToChatErrorRateLimit"`

Expected: PASS

**Step 3: Add auth error mapping test**

```kotlin
@Test
fun authErrorMappedToChatErrorApi() = runTest {
    fakeApiClient.shouldFail = GroqApiException.AuthError(code = 401)
    viewModel.sendMessage("Hi")
    advanceUntilIdle()

    val error = viewModel.state.value.error
    assertTrue(error is ChatError.Api)
    assertEquals("Authentication failed", (error as ChatError.Api).message)
}
```

**Step 4: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ChatViewModelTest.authErrorMappedToChatErrorApi"`

Expected: PASS

**Step 5: Add API error mapping test**

```kotlin
@Test
fun apiErrorMappedToChatErrorApi() = runTest {
    fakeApiClient.shouldFail = GroqApiException.ApiError(code = 500, message = "Server error")
    viewModel.sendMessage("Hi")
    advanceUntilIdle()

    val error = viewModel.state.value.error
    assertTrue(error is ChatError.Api)
    assertEquals("Server error", (error as ChatError.Api).message)
}
```

**Step 6: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ChatViewModelTest.apiErrorMappedToChatErrorApi"`

Expected: PASS

**Step 7: Commit**

```bash
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt
git commit -m "test(agent): add ChatViewModel error mapping tests"
```

---

## Task 3: ChatViewModel - Streaming & History Tests (3 tests)

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Enhance FakeGroqApiClient to support progressive chunks**

Update the `FakeGroqApiClient` class to add delay between chunks:

```kotlin
class FakeGroqApiClient : GroqApiClient(
    httpClient = HttpClient {},
    apiKey = "fake"
) {
    var responseChunks: List<String> = listOf("Test response")
    var shouldFail: GroqApiException? = null
    var delayResponse = false
    var delayBetweenChunks = false
    var capturedMessages: List<ChatMessage> = emptyList()

    override suspend fun streamChatCompletion(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (GroqApiException) -> Unit
    ) {
        capturedMessages = messages
        if (delayResponse) {
            kotlinx.coroutines.delay(100)
        }
        shouldFail?.let {
            onError(it)
            return
        }
        responseChunks.forEach { chunk ->
            if (delayBetweenChunks) {
                kotlinx.coroutines.delay(10)
            }
            onChunk(chunk)
        }
        onComplete()
    }
}
```

**Step 2: Add streaming content update test**

```kotlin
@Test
fun streamingUpdatesContentProgressively() = runTest {
    fakeApiClient.responseChunks = listOf("Hello", " ", "World")
    fakeApiClient.delayBetweenChunks = true
    viewModel.sendMessage("Hi")

    // Advance partially to catch streaming state
    testDispatcher.scheduler.advanceTimeBy(15)
    assertTrue(viewModel.state.value.isStreaming)
    assertTrue(viewModel.state.value.streamingContent.isNotEmpty())

    advanceUntilIdle()
    assertFalse(viewModel.state.value.isStreaming)
    assertEquals("", viewModel.state.value.streamingContent)
}
```

**Step 3: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ChatViewModelTest.streamingUpdatesContentProgressively"`

Expected: PASS

**Step 4: Add message history truncation test**

```kotlin
@Test
fun messageHistoryTruncatedToMaxHistory() = runTest {
    // Send 12 messages to exceed MAX_HISTORY of 10
    repeat(12) { i ->
        fakeApiClient.responseChunks = listOf("Response $i")
        viewModel.sendMessage("Message $i")
        advanceUntilIdle()
    }

    // The last API call should only include the last 10 messages (not all 24)
    // capturedMessages includes system prompt + conversation messages
    val conversationMessages = fakeApiClient.capturedMessages.filter { it.role != "system" }
    assertTrue(conversationMessages.size <= 10, "Expected at most 10 messages, got ${conversationMessages.size}")
}
```

**Step 5: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ChatViewModelTest.messageHistoryTruncatedToMaxHistory"`

Expected: PASS

**Step 6: Add system prompt inclusion test**

Create a new viewModel with cvDataProvider that returns data:

```kotlin
@Test
fun systemPromptIncludedWhenCVDataAvailable() = runTest {
    val viewModelWithData = ChatViewModel(
        apiClient = fakeApiClient,
        repository = fakeRepository,
        promptBuilder = SystemPromptBuilder(),
        referenceExtractor = ReferenceExtractor(fakeRepository),
        cvDataProvider = { minimalCVData }
    )

    viewModelWithData.sendMessage("Hi")
    advanceUntilIdle()

    val systemMessage = fakeApiClient.capturedMessages.find { it.role == "system" }
    assertNotNull(systemMessage)
    assertTrue(systemMessage.content.contains("Test")) // Contains name from minimalCVData
}
```

**Step 7: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ChatViewModelTest.systemPromptIncludedWhenCVDataAvailable"`

Expected: PASS

**Step 8: Commit**

```bash
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt
git commit -m "test(agent): add ChatViewModel streaming and history tests"
```

---

## Task 4: GroqApiClient - Stream Parsing Edge Cases (3 tests)

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/GroqApiClientTest.kt`

**Step 1: Add test for malformed SSE chunk handling**

```kotlin
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
```

**Step 2: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.api.GroqApiClientTest.handlesMalformedSSEChunkGracefully"`

Expected: PASS

**Step 3: Add test for empty lines in SSE stream**

```kotlin
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
```

**Step 4: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.api.GroqApiClientTest.handlesEmptyLinesInSSEStream"`

Expected: PASS

**Step 5: Add test for empty delta content**

```kotlin
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
```

**Step 6: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.api.GroqApiClientTest.skipsChunksWithEmptyDeltaContent"`

Expected: PASS

**Step 7: Commit**

```bash
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/GroqApiClientTest.kt
git commit -m "test(api): add GroqApiClient stream parsing edge case tests"
```

---

## Task 5: GroqApiClient - Error Handling (2 tests)

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/GroqApiClientTest.kt`

**Step 1: Add test for Forbidden (403) response**

```kotlin
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
```

**Step 2: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.api.GroqApiClientTest.handlesForbiddenAsAuthError"`

Expected: PASS

**Step 3: Add test for network exception**

Add a helper to create a throwing mock client:

```kotlin
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
fun handlesNetworkException() = runTest {
    val client = GroqApiClient(
        createThrowingMockClient(java.io.IOException("Connection refused")),
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
```

**Step 4: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.api.GroqApiClientTest.handlesNetworkException"`

Expected: PASS

**Step 5: Commit**

```bash
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/api/GroqApiClientTest.kt
git commit -m "test(api): add GroqApiClient error handling tests"
```

---

## Task 6: ReferenceExtractor - Additional Type Tests (2 tests)

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractorTest.kt`

**Step 1: Update mock repository to support Achievement and Education**

Update the `mockRepository` at the top of the test class:

```kotlin
private val mockRepository = object : CVRepository() {
    override fun resolveReference(id: String): CVReference? {
        return when (id) {
            "experience.geosatis" -> CVReference("experience.geosatis", "experience", "GEOSATIS")
            "project.mtg-deckbuilder" -> CVReference("project.mtg-deckbuilder", "project", "MTG DeckBuilder")
            "skills.ai-dev" -> CVReference("skills.ai-dev", "skill", "AI Development")
            "achievement.android-school" -> CVReference("achievement.android-school", "achievement", "Android School Creator")
            "education.masters" -> CVReference("education.masters", "education", "Master's Degree")
            else -> null
        }
    }
}
```

**Step 2: Add Achievement type test**

```kotlin
@Test
fun extractsAchievementTypeReferences() {
    val input = "He earned [Achievement: achievement.android-school] recognition."
    val result = extractor.extract(input)

    assertEquals("He earned Android School Creator recognition.", result.cleanedContent)
    assertEquals(1, result.references.size)
    assertEquals("achievement.android-school", result.references.first().id)
}
```

**Step 3: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ReferenceExtractorTest.extractsAchievementTypeReferences"`

Expected: PASS

**Step 4: Add Education type test**

```kotlin
@Test
fun extractsEducationTypeReferences() {
    val input = "Denys holds a [Education: education.masters] in Computer Science."
    val result = extractor.extract(input)

    assertEquals("Denys holds a Master's Degree in Computer Science.", result.cleanedContent)
    assertEquals(1, result.references.size)
    assertEquals("education.masters", result.references.first().id)
}
```

**Step 5: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ReferenceExtractorTest.extractsEducationTypeReferences"`

Expected: PASS

**Step 6: Commit**

```bash
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractorTest.kt
git commit -m "test(agent): add ReferenceExtractor Achievement and Education type tests"
```

---

## Task 7: ReferenceExtractor - Edge Case Tests (3 tests)

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractorTest.kt`

**Step 1: Add whitespace handling test**

```kotlin
@Test
fun handlesWhitespaceAroundIdInBrackets() {
    val input = "Denys worked at [Experience:   experience.geosatis   ] company."
    val result = extractor.extract(input)

    assertEquals("Denys worked at GEOSATIS company.", result.cleanedContent)
    assertEquals(1, result.references.size)
}
```

**Step 2: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ReferenceExtractorTest.handlesWhitespaceAroundIdInBrackets"`

Expected: PASS

**Step 3: Add empty content test**

```kotlin
@Test
fun handlesEmptyContentString() {
    val result = extractor.extract("")

    assertEquals("", result.cleanedContent)
    assertTrue(result.references.isEmpty())
}
```

**Step 4: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ReferenceExtractorTest.handlesEmptyContentString"`

Expected: PASS

**Step 5: Add case sensitivity test**

```kotlin
@Test
fun caseSensitiveTypeMatching() {
    // Lowercase "experience" should NOT match the pattern (which expects "Experience")
    val input = "Denys worked at [experience: experience.geosatis] company."
    val result = extractor.extract(input)

    // Should remain unchanged since pattern is case-sensitive
    assertEquals(input, result.cleanedContent)
    assertTrue(result.references.isEmpty())
}
```

**Step 6: Run test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.agent.ReferenceExtractorTest.caseSensitiveTypeMatching"`

Expected: PASS

**Step 7: Commit**

```bash
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractorTest.kt
git commit -m "test(agent): add ReferenceExtractor edge case tests"
```

---

## Task 8: Final Verification

**Step 1: Run all tests**

Run: `./gradlew allTests`

Expected: BUILD SUCCESSFUL, all tests pass

**Step 2: Verify test count increased**

Run: `./gradlew :composeApp:testDebugUnitTest --info 2>&1 | grep -E "tests completed|tests passed"`

Expected: Test count should show ~18 more tests than before

**Step 3: Final commit with verification docs**

```bash
git add -A
git commit -m "docs: Phase 5 test coverage implementation complete

Added 18 new business logic tests:
- ChatViewModel: 8 tests (retry, error mapping, streaming, history)
- GroqApiClient: 5 tests (malformed data, errors, edge cases)
- ReferenceExtractor: 5 tests (all types, edge cases)

All tests passing on Android and iOS."
```

---

## Success Criteria

- [ ] All 18 new tests implemented and passing
- [ ] No regressions in existing tests
- [ ] `./gradlew allTests` passes
- [ ] 7 commits created (one per task + final)
