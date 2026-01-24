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
eval/                    Evaluation framework for prompt testing
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

## Evaluation Framework

The `eval/` module provides a framework for testing prompt variants and comparing agent performance.

### Running Evaluations

```bash
# Start Phoenix for trace collection
phoenix serve

# Run baseline evaluation (SIMPLE questions)
./gradlew :eval:eval -PevalVariant=BASELINE -PevalQuestions=SIMPLE

# Run with a different prompt variant
./gradlew :eval:eval -PevalVariant=PERSONA_CONCISE

# Run all questions including conversations
./gradlew :eval:eval -PevalQuestions=ALL

# Custom delay between API calls (default: 5000ms)
./gradlew :eval:eval -PevalDelayMs=10000
```

### Configuration Options

| Property | Values | Default |
|----------|--------|---------|
| `evalVariant` | BASELINE, PERSONA_CONCISE, PERSONA_RECRUITER, PERSONA_DETAILED, ROLE_FIRST_PERSON | BASELINE |
| `evalModel` | Any Groq model ID | llama-3.3-70b-versatile |
| `evalProjectMode` | CURATED, ALL_PROJECTS | CURATED |
| `evalFormat` | TEXT, JSON, MARKDOWN | TEXT |
| `evalQuestions` | SIMPLE, CONVERSATIONS, ALL | SIMPLE |
| `evalDelayMs` | Milliseconds between API calls | 5000 |

### Comparing Runs

After running evaluations with different variants, compare them:

```bash
# Compare baseline vs a variant
./gradlew :eval:compare -PbaselineRun=<run-id-1> -PvariantRun=<run-id-2>
```

Run IDs are printed at the end of each evaluation and appear in report filenames.

### Reports

Reports are saved to `eval/reports/`:

```
eval/reports/
├── 2026-01-24_222440_95f36218_BASELINE_TEXT.json   # Full structured data
├── 2026-01-24_222440_95f36218_BASELINE_TEXT.md     # Human-readable summary
└── comparisons/
    └── baseline_vs_concise.json                     # Comparison results
```

### Report Contents

Each report includes:
- **Summary**: Success rate, avg latency, TTFT, P50/P95 latency
- **Per-question results**: Latency, TTFT, suggested projects, response text
- **Configuration**: Variant, model, format, project mode

Example markdown report:

| ID | Category | Latency | TTFT | Suggestions | Status |
|----|----------|---------|------|-------------|--------|
| Q1 | personal info | 846ms | 819ms | - | OK |
| Q2 | skills | 613ms | 605ms | mcdonalds, geosatis | OK |
| Q3 | featured project | 923ms | 916ms | mcdonalds, android-school | OK |

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
