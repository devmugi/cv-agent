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
    tracer = tracer
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

### Running Integration Tests

Integration tests make real API calls and send traces to Phoenix:

```bash
# Start Phoenix first
phoenix serve

# Run integration tests
./gradlew :shared-agent:testAndroidUnitTest

# View traces in Phoenix UI
open http://localhost:6006
```

## Configuration

Set your Groq API key in `local.properties`:

```properties
GROQ_API_KEY=your_api_key_here
```

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Arize Phoenix](https://docs.arize.com/phoenix)
- [OpenTelemetry](https://opentelemetry.io/)
