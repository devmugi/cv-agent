# LLM Observability with Arize Phoenix

The agent module includes OpenTelemetry instrumentation for evaluating prompts and responses.

## Setup Phoenix

```bash
# Install Phoenix
pipx install arize-phoenix

# Start Phoenix server
phoenix serve
```

Phoenix UI will be available at `http://localhost:6006`

## Enable Tracing

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

## What Gets Traced

Each LLM request creates a span with:
- Model name, temperature, max tokens
- System prompt content
- User messages
- Full response content
- Latency and token counts
- Errors with exception details

## Running Evaluation Tests

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

## Production Tracing with Arize Cloud

For production, configure tracing to send to Arize Cloud:

```properties
# local.properties
ARIZE_API_KEY=your_api_key
ARIZE_SPACE_ID=your_space_id
ARIZE_OTLP_ENDPOINT=https://otlp.arize.com/v1
```

The `prodRelease` build variant automatically uses Arize Cloud instead of local Phoenix.
