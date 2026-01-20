# Phase 5: Business Logic Test Coverage Design

**Date:** 2026-01-20
**Goal:** Maximize test coverage for core business logic to reach 80%+ coverage target

---

## Overview

Phase 5 focuses on comprehensive testing of business logic. This design covers 18 new unit tests across the three core components: ChatViewModel, GroqApiClient, and ReferenceExtractor.

---

## 1. ChatViewModel Test Gaps (8 new tests)

The current `ChatViewModelTest` covers basic scenarios but misses critical paths.

### New Tests

| Test Name | Description |
|-----------|-------------|
| `retry resends last user message` | Verifies `retry()` calls API with the same message |
| `retry does nothing when no previous message` | Edge case for retry on fresh state |
| `message history truncated to MAX_HISTORY` | When >10 messages, only last 10 sent to API |
| `buildApiMessages includes system prompt when CV data available` | Verifies system prompt injection |
| `buildApiMessages omits system prompt when CV data null` | Handles missing CV data |
| `rate limit error mapped to ChatError.RateLimit` | Specific error mapping |
| `auth error mapped to ChatError.Api` | Specific error mapping |
| `streaming updates streamingContent progressively` | Verifies incremental streaming state updates |

### Implementation Notes

- Extend `FakeGroqApiClient` to capture messages sent for history verification
- Add `FakeCVDataProvider` to test system prompt inclusion/exclusion
- Use `advanceTimeBy` for streaming progression tests

---

## 2. GroqApiClient Test Gaps (5 new tests)

The current `GroqApiClientTest` covers basic success and error responses but misses edge cases in stream parsing.

### New Tests

| Test Name | Description |
|-----------|-------------|
| `handles malformed SSE chunk gracefully` | When JSON parsing fails mid-stream, continues processing |
| `handles empty lines in SSE stream` | SSE blank line separators should be skipped |
| `handles Forbidden 403 as AuthError` | 403 status maps to AuthError like 401 |
| `handles network exception during request` | HttpClient exceptions map to NetworkError |
| `handles chunks with empty delta content` | Null `delta.content` skipped without calling onChunk |

### Implementation Notes

- Use `MockEngine` to simulate various SSE response formats
- Test exception throwing from mock engine for network errors
- Verify onChunk is NOT called for empty/null content

---

## 3. ReferenceExtractor Test Gaps (5 new tests)

The current `ReferenceExtractorTest` covers core scenarios but misses some reference types and edge cases.

### New Tests

| Test Name | Description |
|-----------|-------------|
| `extracts Achievement type references` | Pattern supports `[Achievement: id]` |
| `extracts Education type references` | Pattern supports `[Education: id]` |
| `handles whitespace around ID in brackets` | Extra spaces should be trimmed |
| `handles empty content string` | Empty input returns empty result |
| `case sensitive type matching` | Lowercase `[experience: id]` should NOT match |

### Implementation Notes

- Extend mock repository to support achievement/education lookups
- Verify regex behavior for case sensitivity

---

## Test File Locations

All tests in `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/`:

- `agent/ChatViewModelTest.kt` - extend existing
- `api/GroqApiClientTest.kt` - extend existing
- `agent/ReferenceExtractorTest.kt` - extend existing

---

## Success Criteria

- All 18 new tests pass
- No regressions in existing tests
- `./gradlew allTests` passes on Android and iOS
