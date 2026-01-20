# Phase 5: Android E2E Testing Design

**Date:** 2026-01-20
**Goal:** Add 3 Android E2E tests with real Groq API integration

---

## Overview

End-to-end tests for Android using Compose Multiplatform UI Testing framework. Tests hit the real Groq API to verify complete user journeys. Tests skip gracefully if API key is not configured.

---

## Test Infrastructure

### Framework
- Compose Multiplatform UI Testing with `runComposeUiTest`
- JUnit4 test runner for Android instrumented tests

### Location
`composeApp/src/androidInstrumentedTest/kotlin/io/github/devmugi/cv/agent/`

### Dependencies

Add to `composeApp/build.gradle.kts`:

```kotlin
// In kotlin.androidTarget block
@OptIn(ExperimentalKotlinGradlePluginApi::class)
instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)

// In android.defaultConfig block
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

// Dependencies
androidTestImplementation("androidx.compose.ui:ui-test-junit4-android:1.7.0")
debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.0")
```

### Run Command
```bash
./gradlew :composeApp:connectedAndroidTest
```

---

## API Key Handling

API key read from `local.properties` (same as app). Tests skip if not configured:

```kotlin
@Before
fun checkApiKey() {
    Assume.assumeTrue(
        "Skipping E2E test - GROQ_API_KEY not configured",
        BuildConfig.GROQ_API_KEY.isNotEmpty()
    )
}
```

---

## Retry Logic

Custom JUnit rule retries failed tests up to 3 times:

```kotlin
class RetryRule(private val maxAttempts: Int = 3) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                var lastError: Throwable? = null
                repeat(maxAttempts) { attempt ->
                    try {
                        base.evaluate()
                        return
                    } catch (e: Throwable) {
                        lastError = e
                        println("Attempt ${attempt + 1} failed: ${e.message}")
                    }
                }
                throw lastError!!
            }
        }
    }
}
```

### Timeouts
- Use `waitUntil(timeoutMillis = 30_000)` for API response assertions

---

## Test Cases

### Test 1: happyPath_tapSuggestionAndReceiveResponse
1. Launch app
2. Verify suggestion chips visible
3. Tap first suggestion chip
4. Wait for response (30s timeout)
5. Verify assistant message appears with content
6. Verify reference chips appear (if any)

### Test 2: userInput_typeMessageAndReceiveResponse
1. Launch app
2. Tap input field
3. Type "What are Denys's skills?"
4. Tap send button
5. Wait for response
6. Verify user message appears
7. Verify assistant response appears

### Test 3: conversation_multipleMessagesInHistory
1. Send first message, wait for response
2. Send second message, wait for response
3. Verify both user messages visible
4. Verify both assistant responses visible
5. Verify scroll works (messages accessible)

---

## UI Test Tags

Add `testTag` modifiers to composables for reliable element finding:

| Component | Tag Pattern |
|-----------|-------------|
| ChatScreen LazyColumn | `chat_messages_list` |
| Welcome section | `welcome_section` |
| SuggestionChip | `suggestion_chip_$index` |
| MessageInput TextField | `message_input_field` |
| Send button | `send_button` |
| MessageBubble | `message_user_$id` / `message_assistant_$id` |
| ReferenceChip | `reference_chip_$id` |

---

## Files to Create/Modify

| File | Action |
|------|--------|
| `composeApp/build.gradle.kts` | Add test dependencies, instrumented test config |
| `composeApp/src/androidInstrumentedTest/kotlin/.../RetryRule.kt` | Create retry rule |
| `composeApp/src/androidInstrumentedTest/kotlin/.../ChatE2ETest.kt` | Create 3 tests |
| `composeApp/src/commonMain/.../ChatScreen.kt` | Add test tags |
| `composeApp/src/commonMain/.../SuggestionChip.kt` | Add test tags |
| `composeApp/src/commonMain/.../SuggestionChipsGrid.kt` | Pass index to chips |
| `composeApp/src/commonMain/.../MessageInput.kt` | Add test tags |
| `composeApp/src/commonMain/.../MessageBubble.kt` | Add test tags |
| `composeApp/src/commonMain/.../ReferenceChip.kt` | Add test tags |

---

## Success Criteria

- [ ] All 3 E2E tests pass with API key configured
- [ ] Tests skip gracefully without API key
- [ ] Retry logic handles transient failures
- [ ] `./gradlew :composeApp:connectedAndroidTest` succeeds
