# CV Agent

A Kotlin Multiplatform mobile app that provides an AI-powered chat interface for exploring CV/resume data. Uses Groq's LLM API with streaming responses.

## Module Structure

```
shared-domain/           Pure domain models (ChatState, Message, etc.)
shared-career-projects/  Career/CV data models and UI components
shared-agent-api/        LLM API client with OpenTelemetry tracing
shared-agent/            Agent business logic (ViewModel, prompts)
shared-ui/               Shared UI components
shared/                  DI wiring and platform configuration
android-app/             Android application entry point
iosApp/                  iOS application (Xcode project)
```

## Build Commands

```bash
# Build Android app
./gradlew :android-app:assembleDebug

# Run all tests
./gradlew allTests

# Run agent tests only
./gradlew :shared-agent:testAndroidUnitTest

# Quality checks
./gradlew qualityCheck
```

## Agent Module

The agent functionality is split into two modules for better testability:

### shared-agent-api

Contains the LLM API client with OpenTelemetry tracing support:
- `GroqApiClient` - Streaming chat completions via Groq API
- `RateLimiter` / `TokenBucketRateLimiter` - Rate limiting for API calls
- `AgentTracer` - Tracing interface for LLM observability
- `OpenTelemetryAgentTracer` - OTEL implementation (Android)

### shared-agent

Contains the agent business logic:
- `ChatViewModel` - State management and message flow
- `SystemPromptBuilder` - Builds system prompts from CV data
- `AgentDataProvider` - Provides CV data to the agent
- `SuggestionExtractor` - Extracts project suggestions from responses

## LLM Observability with Arize Phoenix

The agent module includes OpenTelemetry instrumentation for evaluating prompts and responses.

### Setup Phoenix

```bash
# Install Phoenix
pipx install arize-phoenix

# Start Phoenix server
phoenix serve
```

Phoenix UI will be available at `http://localhost:6006`

### Enable Tracing

To enable tracing in the app, inject `OpenTelemetryAgentTracer` when creating the `GroqApiClient`:

```kotlin
val tracer = OpenTelemetryAgentTracer.create(
    endpoint = "http://localhost:4317",  // OTLP gRPC endpoint
    serviceName = "cv-agent"
)

val apiClient = GroqApiClient(
    httpClient = httpClient,
    apiKey = apiKey,
    tracer = tracer,
    rateLimiter = TokenBucketRateLimiter()  // Optional, enabled by default via DI
)
```

### What Gets Traced

Each LLM request creates a span with:
- Model name, temperature, max tokens
- System prompt content
- User messages
- Full response content
- Latency and token counts
- Errors with exception details

### Running Evaluation Tests

Evaluation and integration tests make real Groq API calls and are **excluded from regular test runs** to avoid rate limits. They have a dedicated Gradle task:

```bash
# Start Phoenix first
phoenix serve

# Run evaluation tests (2s delay between tests by default)
./gradlew :shared-agent-api:evaluationTests

# Run with custom delay (in milliseconds)
GROQ_TEST_DELAY_MS=3000 ./gradlew :shared-agent-api:evaluationTests

# Run a single test (no delay needed)
GROQ_TEST_DELAY_MS=0 ./gradlew :shared-agent-api:evaluationTests --tests "*Q1 CURATED*"

# View traces in Phoenix UI
open http://localhost:6006
```

## Rate Limiting

The app includes built-in rate limiting to respect Groq API limits (30 RPM on free tier).

### How It Works

- `TokenBucketRateLimiter` enforces a minimum 2-second delay between API requests
- On 429 (rate limit exceeded) responses, the `Retry-After` header is respected
- Falls back to 60-second backoff if no `Retry-After` header is provided

### Configuration

Rate limiting is enabled by default in production. To customize:

```kotlin
// Custom rate limiter with different delay
val rateLimiter = TokenBucketRateLimiter(minDelayMs = 1000) // 1 second

val apiClient = GroqApiClient(
    httpClient = httpClient,
    apiKey = apiKey,
    rateLimiter = rateLimiter
)

// Or disable rate limiting entirely (not recommended for production)
val apiClient = GroqApiClient(
    httpClient = httpClient,
    apiKey = apiKey,
    rateLimiter = RateLimiter.NOOP
)
```

## API Key Configuration

Set your Groq API key in `local.properties`:

```properties
GROQ_API_KEY=your_api_key_here
```

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Arize Phoenix](https://docs.arize.com/phoenix)
- [OpenTelemetry](https://opentelemetry.io/)
