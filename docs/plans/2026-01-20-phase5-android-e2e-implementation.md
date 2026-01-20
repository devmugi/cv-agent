# Phase 5: Android E2E Testing Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add 3 Android E2E tests with real Groq API integration using Compose Multiplatform UI Testing.

**Architecture:** Add test tags to UI components for reliable element finding, configure Android instrumented tests with retry logic, implement 3 E2E tests covering core user journeys.

**Tech Stack:** Compose Multiplatform UI Testing, JUnit4, AndroidJUnitRunner

---

## Task 1: Configure Build for Instrumented Tests

**Files:**
- Modify: `composeApp/build.gradle.kts`

**Step 1: Add instrumented test configuration**

Add after line 16 (inside `androidTarget` block, after `compilerOptions`):

```kotlin
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree.test)
```

**Step 2: Add testInstrumentationRunner**

Add after line 89 (inside `android.defaultConfig`, after `versionName`):

```kotlin
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
```

**Step 3: Add test dependencies**

Add after line 114 (after `debugImplementation(compose.uiTooling)`):

```kotlin
    androidTestImplementation("androidx.compose.ui:ui-test-junit4-android:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
```

**Step 4: Verify build compiles**

Run: `./gradlew :composeApp:assembleDebugAndroidTest`

Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/build.gradle.kts
git commit -m "build: configure Android instrumented tests"
```

---

## Task 2: Add Test Tags to UI Components

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageInput.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChip.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChipsGrid.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageBubble.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/WelcomeSection.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`

**Step 1: Add testTag import and tags to MessageInput.kt**

Add import after line 30:
```kotlin
import androidx.compose.ui.platform.testTag
```

Modify TextField (line 55-87) to add testTag:
```kotlin
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f).testTag("message_input_field"),
```

Modify IconButton (line 89-111) to add testTag:
```kotlin
            IconButton(
                onClick = {
                    if (canSend) {
                        onSend(text)
                        text = ""
                    }
                },
                enabled = canSend,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp)
                    .testTag("send_button"),
```

**Step 2: Add testTag to SuggestionChip.kt**

Add import after line 10:
```kotlin
import androidx.compose.ui.platform.testTag
```

Add testTag parameter to the composable function signature (line 18-23):
```kotlin
@Composable
fun SuggestionChip(
    text: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Surface(
        onClick = { onClick(text) },
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
```

**Step 3: Update SuggestionChipsGrid.kt to pass testTag**

Add import after line 10:
```kotlin
import androidx.compose.ui.platform.testTag
```

Update the SuggestionChip call (lines 34-38) to include index:
```kotlin
                rowItems.forEachIndexed { colIndex, suggestion ->
                    val index = suggestions.indexOf(suggestion)
                    SuggestionChip(
                        text = suggestion,
                        onClick = onSuggestionClick,
                        testTag = "suggestion_chip_$index"
                    )
                }
```

**Step 4: Add testTag to MessageBubble.kt**

Add import after line 18:
```kotlin
import androidx.compose.ui.platform.testTag
```

Modify Box (line 62-64) to add testTag based on message:
```kotlin
    val roleTag = if (isUser) "user" else "assistant"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("message_${roleTag}_${message.id}"),
        contentAlignment = alignment
    ) {
```

**Step 5: Add testTag to WelcomeSection.kt**

Add import after line 15:
```kotlin
import androidx.compose.ui.platform.testTag
```

Modify Column (line 23-27) to add testTag:
```kotlin
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("welcome_section"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
```

**Step 6: Add testTag to ChatScreen.kt LazyColumn**

Add import after line 22:
```kotlin
import androidx.compose.ui.platform.testTag
```

Modify LazyColumn (line 91-96) to add testTag:
```kotlin
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("chat_messages_list"),
                    state = listState,
```

**Step 7: Verify build compiles**

Run: `./gradlew :composeApp:assembleDebug`

Expected: BUILD SUCCESSFUL

**Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/
git commit -m "feat(ui): add test tags to components for E2E testing"
```

---

## Task 3: Create RetryRule

**Files:**
- Create: `composeApp/src/androidInstrumentedTest/kotlin/io/github/devmugi/cv/agent/RetryRule.kt`

**Step 1: Create directory structure**

Run: `mkdir -p composeApp/src/androidInstrumentedTest/kotlin/io/github/devmugi/cv/agent`

**Step 2: Create RetryRule.kt**

```kotlin
package io.github.devmugi.cv.agent

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryRule(private val maxAttempts: Int = 3) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                var lastError: Throwable? = null
                repeat(maxAttempts) { attempt ->
                    try {
                        base.evaluate()
                        return // Success
                    } catch (e: Throwable) {
                        lastError = e
                        println("${description.methodName}: Attempt ${attempt + 1}/$maxAttempts failed: ${e.message}")
                        if (attempt < maxAttempts - 1) {
                            Thread.sleep(1000) // Wait 1s before retry
                        }
                    }
                }
                throw lastError!!
            }
        }
    }
}
```

**Step 3: Verify file created**

Run: `ls composeApp/src/androidInstrumentedTest/kotlin/io/github/devmugi/cv/agent/`

Expected: `RetryRule.kt`

**Step 4: Commit**

```bash
git add composeApp/src/androidInstrumentedTest/
git commit -m "test: add RetryRule for E2E test resilience"
```

---

## Task 4: Create ChatE2ETest

**Files:**
- Create: `composeApp/src/androidInstrumentedTest/kotlin/io/github/devmugi/cv/agent/ChatE2ETest.kt`

**Step 1: Create ChatE2ETest.kt**

```kotlin
package io.github.devmugi.cv.agent

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ChatE2ETest {

    @get:Rule
    val retryRule = RetryRule(maxAttempts = 3)

    @Before
    fun checkApiKey() {
        Assume.assumeTrue(
            "Skipping E2E test - GROQ_API_KEY not configured in local.properties",
            BuildConfig.GROQ_API_KEY.isNotEmpty()
        )
    }

    @Test
    fun happyPath_tapSuggestionAndReceiveResponse() = runComposeUiTest {
        setContent {
            MainApp()
        }

        // Wait for welcome section to appear
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag("welcome_section")).fetchSemanticsNodes().isNotEmpty()
        }

        // Tap first suggestion chip
        onNodeWithTag("suggestion_chip_0").performClick()

        // Wait for assistant response (real API call)
        waitUntil(timeoutMillis = 30_000) {
            onAllNodes(hasTestTag("message_assistant_", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify user message exists
        onAllNodes(hasTestTag("message_user_", substring = true))
            .fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun userInput_typeMessageAndReceiveResponse() = runComposeUiTest {
        setContent {
            MainApp()
        }

        // Wait for input to be ready
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag("message_input_field")).fetchSemanticsNodes().isNotEmpty()
        }

        // Type message
        onNodeWithTag("message_input_field").performTextInput("What are Denys's skills?")

        // Tap send button
        onNodeWithTag("send_button").performClick()

        // Wait for assistant response
        waitUntil(timeoutMillis = 30_000) {
            onAllNodes(hasTestTag("message_assistant_", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify user message visible
        onAllNodes(hasTestTag("message_user_", substring = true))
            .fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun conversation_multipleMessagesInHistory() = runComposeUiTest {
        setContent {
            MainApp()
        }

        // Wait for input
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag("message_input_field")).fetchSemanticsNodes().isNotEmpty()
        }

        // Send first message
        onNodeWithTag("message_input_field").performTextInput("Tell me about Denys")
        onNodeWithTag("send_button").performClick()

        // Wait for first response
        waitUntil(timeoutMillis = 30_000) {
            onAllNodes(hasTestTag("message_assistant_", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Send second message
        onNodeWithTag("message_input_field").performTextInput("What projects has he worked on?")
        onNodeWithTag("send_button").performClick()

        // Wait for second response
        waitUntil(timeoutMillis = 30_000) {
            onAllNodes(hasTestTag("message_assistant_", substring = true))
                .fetchSemanticsNodes().size >= 2
        }

        // Verify both user messages exist
        val userMessages = onAllNodes(hasTestTag("message_user_", substring = true))
            .fetchSemanticsNodes()
        assert(userMessages.size >= 2) { "Expected at least 2 user messages, found ${userMessages.size}" }

        // Verify both assistant messages exist
        val assistantMessages = onAllNodes(hasTestTag("message_assistant_", substring = true))
            .fetchSemanticsNodes()
        assert(assistantMessages.size >= 2) { "Expected at least 2 assistant messages, found ${assistantMessages.size}" }
    }
}
```

**Step 2: Create MainApp composable for tests**

We need a `MainApp` composable that sets up the full app. Create file:
`composeApp/src/androidInstrumentedTest/kotlin/io/github/devmugi/cv/agent/MainApp.kt`

```kotlin
package io.github.devmugi.cv.agent

import androidx.compose.runtime.Composable
import io.github.devmugi.cv.agent.di.appModule
import io.github.devmugi.cv.agent.ui.ChatScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
fun MainApp() {
    KoinApplication(application = { modules(appModule()) }) {
        val viewModel: io.github.devmugi.cv.agent.agent.ChatViewModel = koinInject()
        ChatScreen(viewModel = viewModel)
    }
}
```

**Step 3: Verify build compiles**

Run: `./gradlew :composeApp:assembleDebugAndroidTest`

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/androidInstrumentedTest/
git commit -m "test: add Android E2E tests for chat functionality"
```

---

## Task 5: Run and Verify Tests

**Step 1: Start Android emulator or connect device**

Ensure an Android device/emulator is running.

**Step 2: Verify GROQ_API_KEY in local.properties**

Check that `local.properties` contains:
```
GROQ_API_KEY=your_actual_key_here
```

**Step 3: Run E2E tests**

Run: `./gradlew :composeApp:connectedDebugAndroidTest`

Expected: 3 tests pass (or skip if no API key)

**Step 4: Final commit**

```bash
git add -A
git commit -m "docs: Phase 5 Android E2E testing complete

Added 3 E2E tests with real Groq API:
- Happy path (tap suggestion, get response)
- User input (type message, get response)
- Conversation history (multiple messages)

Features:
- Retry logic (3 attempts per test)
- 30s timeout for API responses
- Graceful skip when API key not configured"
```

---

## Success Criteria

- [ ] Build compiles with instrumented test configuration
- [ ] Test tags added to all relevant UI components
- [ ] RetryRule provides resilience against flaky tests
- [ ] All 3 E2E tests pass with API key configured
- [ ] Tests skip gracefully without API key
- [ ] `./gradlew :composeApp:connectedDebugAndroidTest` succeeds
