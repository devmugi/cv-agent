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
./gradlew :shared:test                    # Business logic tests
./gradlew :shared-ui:test                 # UI component tests
./gradlew :shared-domain:test             # Domain tests
./gradlew allTests                        # All tests

# Run single test class
./gradlew :shared:test --tests "io.github.devmugi.cv.agent.agent.ChatViewModelTest"

# Quality checks
./gradlew qualityCheck                    # Runs ktlint + detekt
./gradlew ktlintCheck                     # Linting only
./gradlew detekt                          # Static analysis only

# Fix lint issues
./gradlew ktlintFormat
```

## Module Architecture

```
shared-domain/     → Pure domain models (no dependencies)
shared-ui/         → UI components (depends on: shared-domain, Arcane Design System)
shared/            → Business logic, API client, ViewModels (depends on: shared-domain, shared-ui)
android-app/       → Android entry point (depends on: shared)
iosApp/            → iOS app via Xcode (depends on: Shared.framework)
```

**Dependency flow:** `shared-domain` ← `shared-ui` ← `shared` ← `android-app`/`iosApp`

## Key Architecture Patterns

**State Management:** MutableStateFlow in ChatViewModel with reactive collection in Compose. Max 10 messages in API history.

**Dependency Injection:** Koin with module in `shared/src/commonMain/kotlin/.../di/AppModule.kt`. ViewModels use factory scope with parameters.

**API Client:** `GroqApiClient` streams Server-Sent Events (SSE) from Groq API (model: llama-3.3-70b-versatile) with `onChunk`, `onComplete`, `onError` callbacks.

**Platform Config:** `GroqConfig` uses expect/actual pattern for API key injection per platform.

**Reference Extraction:** `ReferenceExtractor` parses CV references from AI responses, displayed as tappable chips.

**Error Handling:** All errors displayed via Arcane toast notifications (no in-UI error messages).

## Key Files

| Purpose | Location |
|---------|----------|
| ViewModel | `shared/src/commonMain/.../agent/ChatViewModel.kt` |
| API Client | `shared/src/commonMain/.../api/GroqApiClient.kt` |
| DI Setup | `shared/src/commonMain/.../di/AppModule.kt` |
| Main Screen | `shared-ui/src/commonMain/.../ui/ChatScreen.kt` |
| Domain Models | `shared-domain/src/commonMain/.../domain/models/` |
| Android Entry | `android-app/src/main/kotlin/.../MainActivity.kt` |
| Test Fakes | `shared/src/commonTest/.../agent/ChatViewModelTest.kt` |

## Design System

Uses **Arcane Design System** (io.github.nicholashauschild:arcane-components). Key components:
- `ArcaneTheme`, `ArcaneButton`, `ArcaneTextField`, `ArcaneToast`, `ArcaneToastHost`
- Theme colors: `ArcaneTheme.colors.*`
- Typography: `ArcaneTheme.typography.*`

## Testing Patterns

- Use `StandardTestDispatcher` for coroutine testing
- Use `Turbine` for Flow testing (`flow.test { }`)
- Use `MockK` for mocking
- Create fake implementations (see `FakeGroqApiClient`, `FakeCVRepository` in ViewModel tests)
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
