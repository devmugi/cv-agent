# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CV Agent is a Kotlin Multiplatform (KMP) Compose Multiplatform mobile app that provides an AI-powered chat interface for exploring CV/resume data. It uses Groq's LLM API with streaming responses and extracts CV references from AI responses.

## Build Commands

```bash
# Build
./gradlew build                           # Full build
./gradlew :android-app:assembleDebug      # Android debug APK

# Tests
./gradlew :shared-agent:testAndroidUnitTest  # Agent tests
./gradlew :shared-agent-api:testAndroidUnitTest  # API client tests
./gradlew :shared-ui:testAndroidUnitTest  # UI component tests
./gradlew :shared-domain:testAndroidUnitTest  # Domain tests
./gradlew allTests                        # All tests

# Run single test class
./gradlew :shared-agent:testAndroidUnitTest --tests "io.github.devmugi.cv.agent.agent.ChatViewModelTest"

# Quality checks
./gradlew qualityCheck                    # Runs ktlint + detekt
./gradlew ktlintCheck                     # Linting only
./gradlew detekt                          # Static analysis only

# Fix lint issues
./gradlew ktlintFormat
```

## Build Rules

**Do NOT build iOS targets.** Reason: iOS tooling sucks, Xcode takes infinity time to build. Use Android-specific tasks instead:
- `./gradlew :shared-agent:compileAndroidMain` instead of `./gradlew :shared-agent:build`
- `./gradlew :shared:compileAndroidMain` instead of `./gradlew :shared:build`

## Module Architecture

```
shared-domain/         → Pure domain models (no dependencies)
shared-career-projects/→ Career/CV data models & UI components
shared-agent-api/      → LLM API client + OpenTelemetry tracing
shared-agent/          → Agent business logic (ViewModel, prompts)
shared-ui/             → UI components (depends on: shared-domain, Arcane Design System)
shared/                → DI wiring only (depends on: all shared modules)
android-app/           → Android entry point (depends on: shared)
iosApp/                → iOS app via Xcode (depends on: Shared.framework)
```

**Dependency flow:**
```
shared-domain ← shared-career-projects ← shared-agent-api ← shared-agent
                                              ↑                  ↑
                                              └─── shared ───────┘
                                                      ↑
                                                 android-app
```

## Key Architecture Patterns

**State Management:** MutableStateFlow in ChatViewModel with reactive collection in Compose. Max 10 messages in API history.

**Dependency Injection:** Koin with module in `shared/src/commonMain/kotlin/.../di/AppModule.kt`. ViewModels use factory scope with parameters.

**API Client:** `GroqApiClient` in shared-agent-api streams Server-Sent Events (SSE) from Groq API (model: llama-3.3-70b-versatile) with `onChunk`, `onComplete`, `onError` callbacks. Supports OpenTelemetry tracing via `AgentTracer`.

**Platform Config:** `GroqConfig` uses expect/actual pattern for API key injection per platform.

**Suggestion Extraction:** `SuggestionExtractor` parses project suggestions from AI responses, displayed as tappable chips.

**Error Handling:** All errors displayed via Arcane toast notifications (no in-UI error messages).

## Key Files

| Purpose | Location |
|---------|----------|
| ViewModel | `shared-agent/src/commonMain/.../agent/ChatViewModel.kt` |
| API Client | `shared-agent-api/src/commonMain/.../api/GroqApiClient.kt` |
| Tracing | `shared-agent-api/src/commonMain/.../api/tracing/AgentTracer.kt` |
| OTEL Tracer | `shared-agent-api/src/androidMain/.../api/tracing/OpenTelemetryAgentTracer.kt` |
| Prompt Builder | `shared-agent/src/commonMain/.../agent/SystemPromptBuilder.kt` |
| DI Setup | `shared/src/commonMain/.../di/AppModule.kt` |
| Main Screen | `shared-ui/src/commonMain/.../ui/ChatScreen.kt` |
| Domain Models | `shared-domain/src/commonMain/.../domain/models/` |
| Android Entry | `android-app/src/main/kotlin/.../MainActivity.kt` |
| Agent Tests | `shared-agent/src/commonTest/.../agent/ChatViewModelTest.kt` |

## LLM Observability

The agent includes OpenTelemetry instrumentation for evaluating prompts:

```bash
# Install and start Phoenix
pipx install arize-phoenix
phoenix serve

# Run tests with tracing
./gradlew :shared-agent:testAndroidUnitTest

# View traces at http://localhost:6006
```

## Evaluation Tests Analysis

**After running `evaluationTests`, ALWAYS query Phoenix API to analyze results.**

### Running Evaluation Tests

```bash
# Run all evaluation tests (with rate limit delay)
GROQ_TEST_DELAY_MS=10000 ./gradlew :shared-agent-api:evaluationTests

# Run only CURATED simple questions (Q1-Q10)
GROQ_TEST_DELAY_MS=10000 ./gradlew :shared-agent-api:evaluationTests --tests "*Q* CURATED*"

# Run only conversations
GROQ_TEST_DELAY_MS=10000 ./gradlew :shared-agent-api:evaluationTests --tests "*Conv* CURATED*"
```

### Phoenix API Analysis (Required After Tests)

After tests complete, query Phoenix GraphQL API at `http://localhost:6006/graphql`:

```bash
# Get project ID
curl -s http://localhost:6006/graphql -H "Content-Type: application/json" \
  -d '{"query":"{ projects { edges { node { id name } } } }"}'

# Get recent spans with full attributes (use project ID from above)
curl -s http://localhost:6006/graphql -H "Content-Type: application/json" \
  -d '{"query":"query { node(id: \"PROJECT_ID\") { ... on Project { spans(first: 20, sort: { col: startTime, dir: desc }) { edges { node { name latencyMs statusCode startTime attributes } } } } } }"}'
```

### Required Analysis Output

After running evaluation tests, provide a summary table:

| Test | Question (short) | Latency | TTFT | Prompt Tokens | Completion Tokens | Response Analysis |
|------|------------------|---------|------|---------------|-------------------|-------------------|
| Q1   | Job title        | 299ms   | 297ms| 1,997         | 36                | ✅ Accurate, concise |

**Include for each test:**
1. **Question** - Short summary of what was asked
2. **Latency** - Total response time (ms)
3. **TTFT** - Time to first token (ms) from `llm.latency.time_to_first_token_ms`
4. **Token counts** - From `llm.token_count.prompt/completion/total`
5. **Response analysis** - Brief quality assessment:
   - ✅ Accurate and complete
   - ⚠️ Partially correct or verbose
   - ❌ Incorrect or missing info

**Insights to include:**
- Average latency and TTFT
- Token efficiency (completion/prompt ratio)
- Prompt variant used (`llm.prompt.variant`: CURATED vs ALL_PROJECTS)
- Session tracking (`session.id`, `llm.turn_number`) for conversations
- Any errors (`error.type`, `error.retryable`)

### Trace Attributes Reference

| Attribute | Description |
|-----------|-------------|
| `llm.token_count.prompt` | Input tokens |
| `llm.token_count.completion` | Output tokens |
| `llm.token_count.total` | Total tokens |
| `llm.latency.time_to_first_token_ms` | TTFT in milliseconds |
| `llm.prompt.version` | Prompt version (e.g., "1.0.0") |
| `llm.prompt.variant` | CURATED or ALL_PROJECTS |
| `session.id` | Conversation session UUID |
| `llm.turn_number` | Turn in conversation (1, 2, 3...) |
| `error.type` | auth, rate_limit, api, timeout, network |
| `error.retryable` | true/false |

## Design System

Uses **Arcane Design System** (io.github.devmugi.design.arcane). Key components:
- `ArcaneTheme`, `ArcaneButton`, `ArcaneTextField`, `ArcaneToast`, `ArcaneToastHost`
- Theme colors: `ArcaneTheme.colors.*`
- Typography: `ArcaneTheme.typography.*`

## Testing Patterns

- Use `StandardTestDispatcher` for coroutine testing
- Use `Turbine` for Flow testing (`flow.test { }`)
- Use `MockK` for mocking
- Create fake implementations (see `FakeGroqApiClient` in ChatViewModelTest)
- UI tests use `@OptIn(ExperimentalTestApi::class)` with `runComposeUiTest`

## Code Quality

- **Max line length:** 120 characters
- **Detekt:** Warnings as errors, cyclomatic complexity threshold 15
- Run `./gradlew qualityCheck` before committing

## API Configuration

Set `GROQ_API_KEY` in `local.properties`:
```properties
GROQ_API_KEY=your_key_here
```
