# arize-tracing

Kotlin Multiplatform OpenTelemetry tracing for [Arize Phoenix](https://arize.com/docs/ax/observe/tracing).

Implements [OpenInference semantic conventions](https://arize.com/docs/ax/observe/tracing/spans) for full LLM observability.

## Features

- Kotlin DSL builder for span configuration
- Cost tracking with automatic calculation
- Session and conversation tracking
- Prompt template instrumentation
- Metadata and tags support
- Mid-span events
- Mode-based processor selection (production/testing)
- Arize Cloud support via custom headers

## Installation

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(projects.arizeTracing)
}
```

## Quick Start

```kotlin
// Create tracer (Android/JVM)
val tracer = OpenTelemetryArizeTracer.create(
    endpoint = "http://localhost:6006/v1/traces",
    serviceName = "my-app",
    mode = TracingMode.TESTING
)

// Start a span
val span = tracer.startLlmSpan {
    model("llama-3.3-70b-versatile")
    provider("groq")
    systemPrompt("You are a helpful assistant.")
    messages(listOf(ChatMessage("user", "Hello!")))
}

// On first token
span.recordFirstToken()

// On completion
span.complete(
    fullResponse = "Hello! How can I help you?",
    tokenUsage = TokenUsage(promptTokens = 50, completionTokens = 10, totalTokens = 60)
)

// Cleanup
tracer.flush()
tracer.shutdown()
```

## Phoenix Setup

1. Install Phoenix:
```bash
pipx install arize-phoenix
```

2. Start Phoenix server:
```bash
phoenix serve
```

3. View traces at: http://localhost:6006

## Usage Examples

### Basic LLM Tracing

Minimal setup for tracing LLM calls:

```kotlin
val span = tracer.startLlmSpan {
    model("gpt-4")
    provider("openai")
    messages(chatHistory)
}

try {
    val response = llmClient.complete(messages)
    span.complete(response.text, response.usage?.let {
        TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens)
    })
} catch (e: Exception) {
    span.error(e, errorType = "api", retryable = true)
}
```

### Multi-turn Conversations

Track conversation sessions with turn numbers:

```kotlin
// Generate session ID once per conversation
val sessionId = UUID.randomUUID().toString()
var turnNumber = 0

// Each message in the conversation
fun sendMessage(userMessage: String) {
    turnNumber++

    val span = tracer.startLlmSpan {
        model("llama-3.3-70b-versatile")
        provider("groq")
        sessionId(sessionId)
        turnNumber(turnNumber)
        messages(conversationHistory)
    }

    // ... handle response
    span.complete(response, tokenUsage)
}
```

### Cost Tracking

Configure pricing to track costs in Phoenix:

```kotlin
val span = tracer.startLlmSpan {
    model("llama-3.3-70b-versatile")
    provider("groq")

    // Groq pricing (as of Jan 2025)
    pricing(
        promptPerMillion = 0.59,
        completionPerMillion = 0.79
    )

    messages(chatHistory)
}

// Cost is automatically calculated when you call complete() with token usage
span.complete(response, TokenUsage(
    promptTokens = 1000,
    completionTokens = 500,
    totalTokens = 1500
))
// Phoenix will show: prompt cost, completion cost, total cost
```

### Prompt Templates

Instrument prompt templates for Phoenix Playground experimentation:

```kotlin
val template = "You are a {role} assistant. Help the user with {topic}."
val variables = mapOf("role" to "helpful", "topic" to "coding questions")

val span = tracer.startLlmSpan {
    model("claude-3-opus")
    provider("anthropic")

    promptTemplate(
        template = template,
        version = "v1.2",
        variables = variables
    )

    systemPrompt(template.replace("{role}", "helpful").replace("{topic}", "coding questions"))
    messages(chatHistory)
}
```

### Adding Evaluations

Record evaluation scores on spans:

```kotlin
val span = tracer.startLlmSpan {
    model("gpt-4")
    messages(chatHistory)
}

// ... get response
span.complete(response, tokenUsage)

// Add evaluations (can be called before or after complete)
span.addEvaluation("relevance", score = 0.95)
span.addEvaluation("hallucination", score = 0.1, label = "low")
span.addEvaluation("toxicity", score = 0.0, label = "safe")
```

### Error Handling

Categorize errors for filtering in Phoenix:

```kotlin
val span = tracer.startLlmSpan {
    model("llama-3.3-70b-versatile")
    provider("groq")
    messages(chatHistory)
}

try {
    val response = apiClient.complete(messages)
    span.complete(response.text, response.tokenUsage)
} catch (e: RateLimitException) {
    span.error(e, errorType = "rate_limit", retryable = true)
} catch (e: AuthenticationException) {
    span.error(e, errorType = "auth", retryable = false)
} catch (e: SocketTimeoutException) {
    span.error(e, errorType = "timeout", retryable = true)
} catch (e: Exception) {
    span.error(e, errorType = "api", retryable = false)
}
```

Error types: `auth`, `rate_limit`, `api`, `timeout`, `network`

### Metadata and Tags

Add custom attributes for filtering and analysis:

```kotlin
val span = tracer.startLlmSpan {
    model("llama-3.3-70b-versatile")
    provider("groq")

    // Custom metadata (use vendor prefix to avoid conflicts)
    metadata(
        "myapp.environment" to "production",
        "myapp.feature_flag" to "new_prompt_v2",
        "myapp.user_tier" to "premium"
    )

    // Tags for categorization
    tags("evaluation", "curated", "high-priority")

    messages(chatHistory)
}

// Can also add metadata/tags after span creation
span.addMetadata("myapp.latency_bucket", "fast")
span.addTags("completed", "successful")
```

## Configuration

### TracingMode

| Mode | Processor | Use Case |
|------|-----------|----------|
| `PRODUCTION` | BatchSpanProcessor | Production apps - async, better performance |
| `TESTING` | SimpleSpanProcessor | Tests - sync, deterministic, immediate flush |

```kotlin
// Production: spans batched and exported async
val prodTracer = OpenTelemetryArizeTracer.create(
    mode = TracingMode.PRODUCTION
)

// Testing: spans exported immediately, sync
val testTracer = OpenTelemetryArizeTracer.create(
    mode = TracingMode.TESTING
)
```

### Endpoints

**Local Phoenix (default):**
```kotlin
val tracer = OpenTelemetryArizeTracer.create(
    endpoint = "http://localhost:6006/v1/traces",
    serviceName = "my-app"
)
```

**Arize Cloud:**
```kotlin
val tracer = OpenTelemetryArizeTracer.create(
    endpoint = "https://otlp.arize.com/v1/traces",
    serviceName = "my-app",
    headers = mapOf("authorization" to "Bearer $ARIZE_API_KEY"),
    mode = TracingMode.PRODUCTION
)
```

## API Reference

### ArizeTracer

| Method | Description |
|--------|-------------|
| `startLlmSpan { }` | Start a new LLM span with DSL configuration |
| `flush()` | Flush pending spans (call at end of tests) |
| `shutdown()` | Shutdown tracer and flush remaining spans |

### LlmSpanBuilder

| Method | Attribute | Description |
|--------|-----------|-------------|
| `model(name)` | `llm.model_name` | Model name (required) |
| `provider(name)` | `llm.provider` | Provider name |
| `systemPrompt(content)` | `llm.input_messages.0.*` | System prompt |
| `messages(list)` | `llm.input_messages.N.*` | Conversation history |
| `sessionId(id)` | `session.id` | Conversation session ID |
| `turnNumber(n)` | `llm.turn_number` | Turn in conversation |
| `userId(id)` | `user.id` | User identifier |
| `installationId(id)` | `device.installation_id` | Device ID |
| `promptTemplate(...)` | `llm.prompt_template.*` | Template for playground |
| `pricing(...)` | (used for cost calc) | Token pricing |
| `metadata(...)` | `metadata.*` | Custom key-value pairs |
| `tags(...)` | `tag.tags` | Categorizing labels |
| `temperature(v)` | `llm.invocation_parameters` | Temperature param |
| `maxTokens(v)` | `llm.invocation_parameters` | Max tokens param |

### TracingSpan

| Method | Attribute | Description |
|--------|-----------|-------------|
| `addResponseChunk(chunk)` | - | Accumulate streaming response |
| `recordFirstToken()` | `llm.latency.time_to_first_token_ms` | Record TTFT |
| `complete(response, usage)` | `llm.output_messages.*`, `llm.token_count.*`, `llm.cost.*` | Complete span |
| `error(ex, type, retryable)` | `error.type`, `error.retryable` | Record error |
| `addEvaluation(name, score, label)` | `evals.{name}.*` | Add eval score |
| `addEvent(name, attrs)` | OTEL event | Mid-span event |
| `addMetadata(key, value)` | `metadata.*` | Late-bound metadata |
| `addTags(...)` | `tag.tags` | Late-bound tags |

## Migration from shared-agent-api

If you're migrating from the old `AgentTracer`:

**Before:**
```kotlin
import io.github.devmugi.cv.agent.api.tracing.AgentTracer

val tracer: AgentTracer = OpenTelemetryAgentTracer.create(...)

val span = tracer.startLlmSpan(
    model = "llama-3.3-70b-versatile",
    systemPrompt = prompt,
    messages = messages,
    temperature = 0.7,
    maxTokens = 1024,
    sessionId = sessionId,
    turnNumber = turnNumber,
    promptMetadata = PromptMetadata(version, variant),
    installationId = installationId
)
```

**After:**
```kotlin
import io.github.devmugi.arize.tracing.ArizeTracer
import io.github.devmugi.arize.tracing.OpenTelemetryArizeTracer

val tracer: ArizeTracer = OpenTelemetryArizeTracer.create(...)

val span = tracer.startLlmSpan {
    model("llama-3.3-70b-versatile")
    provider("groq")  // NEW: provider tracking
    systemPrompt(prompt)
    messages(messages.map { ChatMessage(it.role, it.content) })
    temperature(0.7)
    maxTokens(1024)
    sessionId?.let { sessionId(it) }
    turnNumber?.let { turnNumber(it) }
    promptTemplate(template = "", version = version)  // NEW: separate from variant
    installationId?.let { installationId(it) }
    pricing(0.59, 0.79)  // NEW: cost tracking
}
```
