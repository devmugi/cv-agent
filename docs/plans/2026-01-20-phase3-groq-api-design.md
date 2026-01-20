# Phase 3: Groq API Integration & Agent Logic - Design Document

## Overview

This document describes the design for Phase 3 of the CV Agent app, which adds Groq API integration and conversational agent logic.

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| API Key Management | BuildConfig injection | Simple, secure for portfolio, works with CI/CD |
| Response Style | Streaming (SSE) | Polished UX, text appears word-by-word |
| Model | llama-3.3-70b-versatile | Best quality, 128K context for full CV |
| Persona | Third-person assistant | Professional, honest about being AI |
| Reference Format | Explicit `[Type: ID]` | Predictable, testable, renders as chips |
| Conversation History | Last 10 turns | Balance of context and token usage |
| Error Handling | Graceful with manual retry | User-friendly, simple implementation |
| Welcome Experience | Greeting + suggestion chips | Guides visitors, shows capabilities |
| Dependency Injection | Koin | KMP standard, showcases architecture skills |

## Architecture

### Package Structure

```
io.github.devmugi.cv.agent/
├── api/
│   ├── GroqApiClient.kt          # HTTP client for Groq
│   ├── models/                   # Request/Response DTOs
│   │   ├── ChatRequest.kt
│   │   ├── ChatResponse.kt
│   │   └── StreamChunk.kt
│   └── GroqApiException.kt       # Custom exceptions
├── agent/
│   ├── ChatViewModel.kt          # Main state holder
│   ├── ChatState.kt              # UI state
│   ├── Message.kt                # Chat message model
│   ├── SystemPromptBuilder.kt    # Builds CV-injected prompt
│   └── ReferenceExtractor.kt     # Parses [Type: Label]
└── di/
    └── AppModule.kt              # Koin module definitions
```

### Data Flow

```
User Input → ChatViewModel → GroqApiClient (streaming) →
Parse chunks → Extract references → Update ChatState → UI observes
```

## Component Designs

### 1. Groq API Client

**GroqApiClient.kt:**
```kotlin
class GroqApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    suspend fun streamChatCompletion(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (GroqApiException) -> Unit
    )
}
```

**Implementation Details:**
- **Endpoint:** `POST https://api.groq.com/openai/v1/chat/completions`
- **Streaming:** Uses `stream: true` parameter, Ktor SSE support
- **Headers:** `Authorization: Bearer $apiKey`, `Content-Type: application/json`
- **Model:** `llama-3.3-70b-versatile`

**Request Model (ChatRequest.kt):**
```kotlin
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 1024
)

@Serializable
data class ChatMessage(
    val role: String,  // "system", "user", "assistant"
    val content: String
)
```

**Stream Parsing:**
Each SSE chunk arrives as `data: {"choices":[{"delta":{"content":"text"}}]}`. The client parses each chunk and calls `onChunk(text)` to stream to the UI. Stream ends with `data: [DONE]`.

**Error Handling:**
- Network failure → `GroqApiException.NetworkError`
- 401/403 → `GroqApiException.AuthError`
- 429 rate limit → `GroqApiException.RateLimitError`
- Other API errors → `GroqApiException.ApiError(code, message)`

### 2. ChatViewModel & State Management

**ChatState.kt:**
```kotlin
data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val error: ChatError? = null,
    val suggestions: List<String> = defaultSuggestions
)

sealed class ChatError {
    data class Network(val message: String) : ChatError()
    data class Api(val message: String) : ChatError()
    object RateLimit : ChatError()
}

val defaultSuggestions = listOf(
    "What's Denys's experience?",
    "Tell me about his skills",
    "What projects has he worked on?",
    "What are his achievements?"
)
```

**Message.kt:**
```kotlin
data class Message(
    val id: String = uuid(),
    val role: MessageRole,
    val content: String,
    val references: List<CVReference> = emptyList(),
    val timestamp: Long = currentTimeMillis()
)

enum class MessageRole { USER, ASSISTANT, SYSTEM }
```

**ChatViewModel.kt:**
```kotlin
class ChatViewModel(
    private val apiClient: GroqApiClient,
    private val repository: CVRepository,
    private val promptBuilder: SystemPromptBuilder,
    private val referenceExtractor: ReferenceExtractor
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun sendMessage(content: String)
    fun onSuggestionClicked(suggestion: String)
    fun retry()
    fun clearError()
}
```

The ViewModel keeps last 10 messages for API context but may display more in the UI.

### 3. System Prompt Design

**SystemPromptBuilder.kt:**
```kotlin
class SystemPromptBuilder(
    private val repository: CVRepository
) {
    fun build(): String
}
```

**Prompt Structure:**
```
You are an AI assistant for Denys Honcharenko's portfolio. Answer questions about
Denys in third person. Be helpful, professional, and concise.

When mentioning specific items from Denys's background, use this format:
[Type: ID] where Type is one of: Experience, Project, Skill, Achievement, Education

Examples: [Experience: experience.geosatis], [Project: project.mtg-deckbuilder]

---

PERSONAL INFO:
Name: Denys Honcharenko
Title: Software Engineer
Location: ...
Summary: ...

SKILLS:
- AI Development: Claude API, MCP, Prompt Engineering...
- KMM/CMP: Kotlin Multiplatform, Compose Multiplatform...
[All 8 categories with IDs]

WORK EXPERIENCE:
- GEOSATIS (ID: experience.geosatis): ...
[All 6 experiences]

PROJECTS:
[All 4 projects with IDs]

ACHIEVEMENTS:
[All 6 achievements with IDs]

EDUCATION:
[Education details with ID]
```

The full CV data (~3-4K tokens) fits easily within the 128K context window.

### 4. Reference Extraction

**ReferenceExtractor.kt:**
```kotlin
class ReferenceExtractor(
    private val repository: CVRepository
) {
    fun extract(content: String): ExtractionResult
}

data class ExtractionResult(
    val cleanedContent: String,      // Text with [Type: ID] removed
    val references: List<CVReference> // Resolved references
)
```

**Parsing Logic:**

1. **Regex Pattern:** `\[(Experience|Project|Skill|Achievement|Education):\s*([^\]]+)\]`
2. **Extract matches** from the streamed response
3. **Resolve each ID** using `CVRepository.resolveReference(id)`
4. **Return cleaned text** with bracketed references removed (UI shows chips separately)

**Example:**
```
Input:  "Denys worked at [Experience: experience.geosatis] where he built KMP apps."
Output:
  - cleanedContent: "Denys worked at GEOSATIS where he built KMP apps."
  - references: [CVReference(id="experience.geosatis", type=EXPERIENCE, label="GEOSATIS")]
```

**Edge Cases:**
- Unknown ID → Log warning, skip reference (don't crash)
- Malformed bracket → Leave as-is in text
- Duplicate references → Deduplicate in list

**Integration with Streaming:**
Reference extraction happens after streaming completes. During streaming, raw text (including brackets) displays. On completion, extract references and update the Message with cleaned content and resolved references.

### 5. Koin Dependency Injection

**AppModule.kt:**
```kotlin
val appModule = module {
    // API Layer
    single {
        HttpClient(engineFactory) {
            install(ContentNegotiation) { json() }
            install(Logging) { level = LogLevel.BODY }
        }
    }
    single { GroqApiClient(get(), BuildConfig.GROQ_API_KEY) }

    // Data Layer
    single { CVDataLoader() }
    single { CVRepository(get()) }

    // Agent Layer
    single { SystemPromptBuilder(get()) }
    single { ReferenceExtractor(get()) }
    viewModel { ChatViewModel(get(), get(), get(), get()) }
}
```

**Platform-Specific HTTP Engine:**
```kotlin
// commonMain
expect val engineFactory: HttpClientEngineFactory<*>

// androidMain
actual val engineFactory = OkHttp

// iosMain
actual val engineFactory = Darwin
```

**BuildConfig Setup (build.gradle.kts):**
```kotlin
android {
    buildTypes {
        all {
            buildConfigField("String", "GROQ_API_KEY",
                "\"${project.findProperty("GROQ_API_KEY") ?: ""}\"")
        }
    }
}
```

**gradle.properties (local, git-ignored):**
```
GROQ_API_KEY=your-api-key-here
```

**Initialization (Application/App entry):**
```kotlin
startKoin {
    modules(appModule)
}
```

## Testing Strategy

### Test Files

```
commonTest/
├── api/
│   └── GroqApiClientTest.kt
├── agent/
│   ├── ChatViewModelTest.kt
│   ├── SystemPromptBuilderTest.kt
│   └── ReferenceExtractorTest.kt
```

### GroqApiClientTest.kt (4-5 tests)
- Successful streaming response parsing
- Network error handling
- Auth error (401/403) handling
- Rate limit (429) handling
- Malformed response handling

**Approach:** Use Ktor's `MockEngine` to simulate API responses without network calls.

### ChatViewModelTest.kt (6-8 tests)
- Initial state has welcome suggestions
- sendMessage adds user message and triggers loading
- Successful response updates messages and clears loading
- Error state set on API failure
- retry() clears error and resends last message
- Conversation history limited to 10 turns
- onSuggestionClicked sends as message

**Approach:** Mock `GroqApiClient` with MockK, use Turbine for StateFlow testing.

### SystemPromptBuilderTest.kt (3-4 tests)
- Prompt contains all CV sections
- Prompt includes reference format instructions
- Prompt uses correct persona (third-person)

### ReferenceExtractorTest.kt (5-6 tests)
- Extracts single reference
- Extracts multiple references
- Handles unknown IDs gracefully
- Handles malformed brackets
- Deduplicates references
- Returns clean content without brackets

### Coverage Target
80%+ on `api/` and `agent/` packages.

## Dependencies to Add

```toml
# libs.versions.toml
[versions]
koin = "3.5.6"
koin-compose = "1.1.5"

[libraries]
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin-compose" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
```

## Files to Create

| File | Purpose |
|------|---------|
| `api/GroqApiClient.kt` | HTTP client for Groq API |
| `api/models/ChatRequest.kt` | Request DTO |
| `api/models/ChatMessage.kt` | Message DTO for API |
| `api/models/StreamChunk.kt` | SSE chunk parsing |
| `api/GroqApiException.kt` | Custom exceptions |
| `agent/ChatViewModel.kt` | Main state holder |
| `agent/ChatState.kt` | UI state data class |
| `agent/Message.kt` | Chat message model |
| `agent/SystemPromptBuilder.kt` | System prompt generation |
| `agent/ReferenceExtractor.kt` | Reference parsing |
| `di/AppModule.kt` | Koin modules |
| `di/HttpEngine.kt` | Platform expect/actual |

## Success Criteria

- [ ] Groq API client successfully streams responses
- [ ] ChatViewModel manages conversation state correctly
- [ ] System prompt includes full CV context
- [ ] References extracted and resolved from responses
- [ ] Koin DI wires all components
- [ ] All tests passing with 80%+ coverage
- [ ] Quality gates (detekt, ktlint) passing
