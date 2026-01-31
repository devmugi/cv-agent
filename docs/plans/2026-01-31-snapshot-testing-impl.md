# Snapshot Testing Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement Roborazzi-based screenshot testing for all shared-ui composables with light/dark theme coverage.

**Architecture:** New `shared-ui-screenshots` Android library module that depends on `shared-ui` and `shared-domain`. Uses Roborazzi with Robolectric for JVM-based screenshot capture. Golden images stored in `src/test/snapshots/images/`.

**Tech Stack:** Roborazzi 1.40.0, Robolectric 4.14.1, Compose UI Test, JUnit 4, ArcaneTheme

---

## Phase 1: Foundation

### Task 1.1: Add Roborazzi Dependencies to Version Catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

**Step 1: Add roborazzi version**

Add after line 59 (robolectric version):

```toml
roborazzi = "1.40.0"
```

**Step 2: Add roborazzi libraries**

Add after line 140 (robolectric library):

```toml
roborazzi = { module = "io.github.takahirom.roborazzi:roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { module = "io.github.takahirom.roborazzi:roborazzi-compose", version.ref = "roborazzi" }
roborazzi-junit-rule = { module = "io.github.takahirom.roborazzi:roborazzi-junit-rule", version.ref = "roborazzi" }
```

**Step 3: Add roborazzi plugin**

Add after line 193 (detekt plugin):

```toml
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
```

**Step 4: Verify syntax**

Run: `./gradlew --version`
Expected: No errors about libs.versions.toml

**Step 5: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: add Roborazzi dependencies to version catalog"
```

---

### Task 1.2: Create Screenshot Module Directory Structure

**Files:**
- Create: `shared-ui-screenshots/build.gradle.kts`
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/.gitkeep`
- Create: `shared-ui-screenshots/src/test/snapshots/images/.gitkeep`

**Step 1: Create directory structure**

```bash
mkdir -p shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots
mkdir -p shared-ui-screenshots/src/test/snapshots/images
touch shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/.gitkeep
touch shared-ui-screenshots/src/test/snapshots/images/.gitkeep
```

**Step 2: Create build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.devmugi.cv.agent.ui.screenshots"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
            }
        }
    }
}

dependencies {
    // Modules under test
    implementation(projects.sharedUi)
    implementation(projects.sharedDomain)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    // Design system
    implementation(libs.arcane.foundation)
    implementation(libs.arcane.components)
    implementation(libs.arcane.chat)

    // Screenshot testing
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.compose.ui.test.junit4)
}
```

**Step 3: Commit**

```bash
git add shared-ui-screenshots/
git commit -m "build: create shared-ui-screenshots module structure"
```

---

### Task 1.3: Register Module in Settings

**Files:**
- Modify: `settings.gradle.kts`

**Step 1: Add module to settings.gradle.kts**

Add after line 44 (`include(":eval")`):

```kotlin
include(":shared-ui-screenshots")
```

**Step 2: Verify module is recognized**

Run: `./gradlew projects`
Expected: Output includes `:shared-ui-screenshots`

**Step 3: Commit**

```bash
git add settings.gradle.kts
git commit -m "build: register shared-ui-screenshots module"
```

---

### Task 1.4: Create Base ScreenshotTest Class

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/ScreenshotTest.kt`

**Step 1: Write the base test class**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.devmugi.arcane.design.foundation.theme.ArcaneColors
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Base class for screenshot tests.
 *
 * Provides helpers for capturing screenshots with consistent theming and naming.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w400dp-h800dp-xhdpi")
abstract class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(
            changeThreshold = 0.01f // 1% pixel difference tolerance
        )
    )

    /**
     * Capture a screenshot with the given name and theme.
     *
     * @param name Test name suffix (e.g., "user_short")
     * @param darkTheme Whether to use dark theme
     * @param content Composable content to capture
     */
    protected fun snapshot(
        name: String,
        darkTheme: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val themeSuffix = if (darkTheme) "dark" else "light"
        val fileName = "src/test/snapshots/images/${this::class.simpleName}_${name}_$themeSuffix.png"

        val colors = if (darkTheme) ArcaneColors.agent2Dark() else ArcaneColors.agent2Light()

        composeRule.setContent {
            ArcaneTheme(colors = colors) {
                Box(
                    modifier = Modifier
                        .background(ArcaneTheme.colors.background)
                        .padding(8.dp)
                ) {
                    content()
                }
            }
        }

        composeRule.onRoot().captureRoboImage(
            filePath = fileName,
            roborazziOptions = roborazziOptions
        )
    }

    /**
     * Capture both light and dark theme variants.
     *
     * @param name Test name suffix
     * @param content Composable content to capture
     */
    protected fun snapshotBothThemes(
        name: String,
        content: @Composable () -> Unit
    ) {
        snapshot(name, darkTheme = false, content = content)
        snapshot(name, darkTheme = true, content = content)
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-ui-screenshots:compileTestKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/ScreenshotTest.kt
git commit -m "test: add ScreenshotTest base class for Roborazzi"
```

---

### Task 1.5: Create First Screenshot Test (SuggestionChip)

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/SuggestionChipScreenshots.kt`

**Step 1: Write the test class**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.SuggestionChip
import org.junit.Test

class SuggestionChipScreenshots : ScreenshotTest() {

    @Test
    fun chip_short() = snapshot("short") {
        SuggestionChip(
            text = "Skills",
            onClick = {}
        )
    }

    @Test
    fun chip_medium() = snapshot("medium") {
        SuggestionChip(
            text = "Tell me about your experience",
            onClick = {}
        )
    }

    @Test
    fun chip_long() = snapshot("long") {
        SuggestionChip(
            text = "What challenging projects have you worked on recently?",
            onClick = {}
        )
    }
}
```

**Step 2: Run tests to verify setup**

Run: `./gradlew :shared-ui-screenshots:testDebugUnitTest --tests "*.SuggestionChipScreenshots"`
Expected: 3 tests pass (or fail gracefully if images don't exist yet)

**Step 3: Record golden images**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.SuggestionChipScreenshots"`
Expected: 3 PNG files created in `src/test/snapshots/images/`

**Step 4: Verify golden images exist**

```bash
ls -la shared-ui-screenshots/src/test/snapshots/images/
```

Expected output includes:
- `SuggestionChipScreenshots_short_light.png`
- `SuggestionChipScreenshots_medium_light.png`
- `SuggestionChipScreenshots_long_light.png`

**Step 5: Verify screenshots pass**

Run: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug --tests "*.SuggestionChipScreenshots"`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/SuggestionChipScreenshots.kt
git add shared-ui-screenshots/src/test/snapshots/images/
git commit -m "test: add SuggestionChip screenshot tests with golden images"
```

---

### Task 1.6: Add Dark Theme Variants to SuggestionChip

**Files:**
- Modify: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/SuggestionChipScreenshots.kt`

**Step 1: Update tests to use snapshotBothThemes**

Replace entire file:

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.SuggestionChip
import org.junit.Test

class SuggestionChipScreenshots : ScreenshotTest() {

    @Test
    fun chip_short() = snapshotBothThemes("short") {
        SuggestionChip(
            text = "Skills",
            onClick = {}
        )
    }

    @Test
    fun chip_medium() = snapshotBothThemes("medium") {
        SuggestionChip(
            text = "Tell me about your experience",
            onClick = {}
        )
    }

    @Test
    fun chip_long() = snapshotBothThemes("long") {
        SuggestionChip(
            text = "What challenging projects have you worked on recently?",
            onClick = {}
        )
    }
}
```

**Step 2: Record new golden images**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.SuggestionChipScreenshots"`
Expected: 6 PNG files (3 light + 3 dark)

**Step 3: Verify all pass**

Run: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug --tests "*.SuggestionChipScreenshots"`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-ui-screenshots/
git commit -m "test: add dark theme variants for SuggestionChip screenshots"
```

---

## Phase 2: Component Screenshots

### Task 2.1: Add SuggestionChipsGrid Screenshots

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/SuggestionGridScreenshots.kt`

**Step 1: Write the test class**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.SuggestionChipsGrid
import org.junit.Test

class SuggestionGridScreenshots : ScreenshotTest() {

    @Test
    fun grid_single() = snapshotBothThemes("single") {
        SuggestionChipsGrid(
            suggestions = listOf("Tell me about yourself"),
            onSuggestionClick = {}
        )
    }

    @Test
    fun grid_three() = snapshotBothThemes("three") {
        SuggestionChipsGrid(
            suggestions = listOf("Experience", "Skills", "Projects"),
            onSuggestionClick = {}
        )
    }

    @Test
    fun grid_six() = snapshotBothThemes("six") {
        SuggestionChipsGrid(
            suggestions = listOf(
                "Android", "Kotlin", "Compose",
                "Architecture", "Testing", "CI/CD"
            ),
            onSuggestionClick = {}
        )
    }

    @Test
    fun grid_overflow() = snapshotBothThemes("overflow") {
        SuggestionChipsGrid(
            suggestions = listOf(
                "Tell me about your Android experience",
                "What projects have you worked on?",
                "Describe your testing approach",
                "How do you handle architecture?",
                "What's your experience with Compose?"
            ),
            onSuggestionClick = {}
        )
    }
}
```

**Step 2: Record golden images**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.SuggestionGridScreenshots"`
Expected: 8 PNG files created

**Step 3: Verify**

Run: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug --tests "*.SuggestionGridScreenshots"`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-ui-screenshots/
git commit -m "test: add SuggestionChipsGrid screenshot tests"
```

---

### Task 2.2: Add TopBar Screenshots

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/TopBarScreenshots.kt`

**Step 1: Read TopBar component to understand API**

Check: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/TopBar.kt`

**Step 2: Write the test class**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.CVAgentTopBar
import org.junit.Test

class TopBarScreenshots : ScreenshotTest() {

    @Test
    fun topBar_default() = snapshotBothThemes("default") {
        CVAgentTopBar(
            onCareerTimelineClick = {},
            onClearHistory = null
        )
    }

    @Test
    fun topBar_withClearHistory() = snapshotBothThemes("with_clear") {
        CVAgentTopBar(
            onCareerTimelineClick = {},
            onClearHistory = {}
        )
    }
}
```

**Step 3: Record and verify**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.TopBarScreenshots"`
Run: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug --tests "*.TopBarScreenshots"`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-ui-screenshots/
git commit -m "test: add TopBar screenshot tests"
```

---

### Task 2.3: Add WelcomeSection Screenshots

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/WelcomeSectionScreenshots.kt`

**Step 1: Write the test class**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.WelcomeSection
import org.junit.Test

class WelcomeSectionScreenshots : ScreenshotTest() {

    @Test
    fun welcome_default() = snapshotBothThemes("default") {
        WelcomeSection()
    }
}
```

**Step 2: Record and verify**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.WelcomeSectionScreenshots"`
Run: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug --tests "*.WelcomeSectionScreenshots"`

**Step 3: Commit**

```bash
git add shared-ui-screenshots/
git commit -m "test: add WelcomeSection screenshot tests"
```

---

### Task 2.4: Add Disclaimer Screenshots

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/DisclaimerScreenshots.kt`

**Step 1: Write the test class**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.Disclaimer
import org.junit.Test

class DisclaimerScreenshots : ScreenshotTest() {

    @Test
    fun disclaimer_default() = snapshotBothThemes("default") {
        Disclaimer()
    }
}
```

**Step 2: Record and verify**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.DisclaimerScreenshots"`
Run: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug --tests "*.DisclaimerScreenshots"`

**Step 3: Commit**

```bash
git add shared-ui-screenshots/
git commit -m "test: add Disclaimer screenshot tests"
```

---

### Task 2.5: Add ContextChip Screenshots

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/ContextChipScreenshots.kt`

**Step 1: Write the test class**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.ContextChip
import org.junit.Test

class ContextChipScreenshots : ScreenshotTest() {

    @Test
    fun contextChip_short() = snapshotBothThemes("short") {
        ContextChip(
            label = "Android",
            onClick = {}
        )
    }

    @Test
    fun contextChip_long() = snapshotBothThemes("long") {
        ContextChip(
            label = "Kotlin Multiplatform Mobile",
            onClick = {}
        )
    }
}
```

**Step 2: Record and verify**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.ContextChipScreenshots"`
Run: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug --tests "*.ContextChipScreenshots"`

**Step 3: Commit**

```bash
git add shared-ui-screenshots/
git commit -m "test: add ContextChip screenshot tests"
```

---

### Task 2.6: Add AnimatedChatInput Screenshots

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/ChatInputScreenshots.kt`

**Step 1: Write the test class**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.AnimatedChatInput
import org.junit.Test

class ChatInputScreenshots : ScreenshotTest() {

    @Test
    fun input_empty() = snapshotBothThemes("empty") {
        AnimatedChatInput(
            value = "",
            onValueChange = {},
            onSend = {},
            enabled = true
        )
    }

    @Test
    fun input_withText() = snapshotBothThemes("with_text") {
        AnimatedChatInput(
            value = "Tell me about your experience with Android",
            onValueChange = {},
            onSend = {},
            enabled = true
        )
    }

    @Test
    fun input_disabled() = snapshotBothThemes("disabled") {
        AnimatedChatInput(
            value = "",
            onValueChange = {},
            onSend = {},
            enabled = false
        )
    }

    @Test
    fun input_sending() = snapshotBothThemes("sending") {
        AnimatedChatInput(
            value = "Sending message...",
            onValueChange = {},
            onSend = {},
            enabled = false,
            isSending = true
        )
    }
}
```

**Step 2: Record and verify**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.ChatInputScreenshots"`
Run: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug --tests "*.ChatInputScreenshots"`

**Step 3: Commit**

```bash
git add shared-ui-screenshots/
git commit -m "test: add AnimatedChatInput screenshot tests"
```

---

## Phase 3: Screen-Level Screenshots

### Task 3.1: Create ChatScreen Test State Helpers

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/ChatScreenTestData.kt`

**Step 1: Write test data factory**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.domain.models.ChatState
import io.github.devmugi.cv.agent.domain.models.Message

/**
 * Test data factory for ChatScreen screenshot tests.
 */
object ChatScreenTestData {

    fun emptyState() = ChatState()

    fun singleUserMessage() = ChatState(
        messages = listOf(
            Message.user("What's your Android experience?")
        )
    )

    fun conversation() = ChatState(
        messages = listOf(
            Message.user("What's your Android experience?"),
            Message.assistant(
                "I have 8+ years of Android development experience, " +
                "including Kotlin, Jetpack Compose, and modern architecture patterns " +
                "like MVI and Clean Architecture."
            ),
            Message.user("Tell me about a challenging project"),
            Message.assistant(
                "One challenging project was building a real-time collaboration " +
                "feature that required WebSocket integration and complex state synchronization."
            )
        )
    )

    fun streaming() = ChatState(
        messages = listOf(
            Message.user("Explain your testing approach"),
            Message.assistant("I believe in comprehensive testing...")
        ),
        isStreaming = true
    )

    fun withSuggestions() = ChatState(
        messages = listOf(
            Message.user("Hi"),
            Message.assistant("Hello! I'm happy to discuss my experience. What would you like to know?")
        ),
        suggestions = listOf(
            "Tell me about your Android experience",
            "What projects have you worked on?",
            "Describe your technical skills"
        )
    )

    fun longConversation() = ChatState(
        messages = listOf(
            Message.user("Hi"),
            Message.assistant("Hello! How can I help you today?"),
            Message.user("Tell me about your Android experience"),
            Message.assistant("I specialize in Android development with over 8 years of experience..."),
            Message.user("What about Compose?"),
            Message.assistant("Jetpack Compose is my preferred UI toolkit. I've been using it since alpha..."),
            Message.user("How do you approach testing?"),
            Message.assistant("I follow TDD practices with comprehensive test coverage including unit, integration, and UI tests.")
        )
    )
}
```

**Step 2: Commit**

```bash
git add shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/ChatScreenTestData.kt
git commit -m "test: add ChatScreen test data factory"
```

---

### Task 3.2: Add ChatScreen Screenshots

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/ChatScreenScreenshots.kt`

**Step 1: Write the test class**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastState
import io.github.devmugi.cv.agent.ui.ChatScreen
import org.junit.Test

class ChatScreenScreenshots : ScreenshotTest() {

    private val toastState = ArcaneToastState()

    @Test
    fun screen_empty() = snapshotBothThemes("empty") {
        ChatScreen(
            state = ChatScreenTestData.emptyState(),
            toastState = toastState,
            onSendMessage = {},
            modifier = Modifier.fillMaxSize()
        )
    }

    @Test
    fun screen_singleMessage() = snapshotBothThemes("single_message") {
        ChatScreen(
            state = ChatScreenTestData.singleUserMessage(),
            toastState = toastState,
            onSendMessage = {},
            modifier = Modifier.fillMaxSize()
        )
    }

    @Test
    fun screen_conversation() = snapshotBothThemes("conversation") {
        ChatScreen(
            state = ChatScreenTestData.conversation(),
            toastState = toastState,
            onSendMessage = {},
            modifier = Modifier.fillMaxSize()
        )
    }

    @Test
    fun screen_streaming() = snapshotBothThemes("streaming") {
        ChatScreen(
            state = ChatScreenTestData.streaming(),
            toastState = toastState,
            onSendMessage = {},
            modifier = Modifier.fillMaxSize()
        )
    }

    @Test
    fun screen_withSuggestions() = snapshotBothThemes("with_suggestions") {
        ChatScreen(
            state = ChatScreenTestData.withSuggestions(),
            toastState = toastState,
            onSendMessage = {},
            modifier = Modifier.fillMaxSize()
        )
    }

    @Test
    fun screen_longConversation() = snapshotBothThemes("long_conversation") {
        ChatScreen(
            state = ChatScreenTestData.longConversation(),
            toastState = toastState,
            onSendMessage = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

**Step 2: Record golden images**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.ChatScreenScreenshots"`
Expected: 12 PNG files created (6 states × 2 themes)

**Step 3: Verify**

Run: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug --tests "*.ChatScreenScreenshots"`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-ui-screenshots/
git commit -m "test: add ChatScreen screenshot tests for all states"
```

---

## Phase 4: CI Integration

### Task 4.1: Update .gitignore

**Files:**
- Modify: `.gitignore`

**Step 1: Add Roborazzi compare output exclusions**

Add at end of file:

```gitignore

# Roborazzi comparison output (generated, not committed)
**/snapshots/images/*_compare.png
**/snapshots/images/*_actual.png
**/build/outputs/roborazzi/
```

**Step 2: Commit**

```bash
git add .gitignore
git commit -m "build: exclude Roborazzi diff output from git"
```

---

### Task 4.2: Create GitHub Actions Workflow

**Files:**
- Create: `.github/workflows/screenshot-tests.yml`

**Step 1: Create workflow directory**

```bash
mkdir -p .github/workflows
```

**Step 2: Write workflow file**

```yaml
name: Screenshot Tests

on:
  pull_request:
    paths:
      - 'shared-ui/**'
      - 'shared-ui-screenshots/**'
      - 'shared-domain/**'
      - 'gradle/libs.versions.toml'

jobs:
  verify-screenshots:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Verify screenshots
        run: ./gradlew :shared-ui-screenshots:verifyRoborazziDebug

      - name: Upload diff report on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: screenshot-diff-report
          path: |
            shared-ui-screenshots/build/outputs/roborazzi/
            shared-ui-screenshots/src/test/snapshots/images/*_compare.png
            shared-ui-screenshots/src/test/snapshots/images/*_actual.png
          retention-days: 7

      - name: Comment PR on failure
        if: failure() && github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.createComment({
              owner: context.repo.owner,
              repo: context.repo.repo,
              issue_number: context.issue.number,
              body: `## Screenshot Tests Failed

            Visual differences detected. Please review the changes:

            1. Download the \`screenshot-diff-report\` artifact from this workflow run
            2. Review the diff images
            3. If changes are intentional, run locally:
               \`\`\`
               ./gradlew :shared-ui-screenshots:recordRoborazziDebug
               \`\`\`
            4. Commit the updated golden images

            [View workflow run](https://github.com/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId})`
            })
```

**Step 3: Commit**

```bash
git add .github/workflows/screenshot-tests.yml
git commit -m "ci: add GitHub Actions workflow for screenshot tests"
```

---

### Task 4.3: Create Module README

**Files:**
- Create: `shared-ui-screenshots/README.md`

**Step 1: Write documentation**

```markdown
# shared-ui-screenshots

Screenshot tests for CV Agent UI components using Roborazzi.

## Quick Reference

| Command | Purpose |
|---------|---------|
| `./gradlew :shared-ui-screenshots:testDebugUnitTest` | Run all screenshot tests |
| `./gradlew :shared-ui-screenshots:recordRoborazziDebug` | Record new golden images |
| `./gradlew :shared-ui-screenshots:verifyRoborazziDebug` | Verify against golden images |
| `./gradlew :shared-ui-screenshots:compareRoborazziDebug` | Generate visual diff images |

## Workflow

### Adding New Component Screenshots

1. Create test class extending `ScreenshotTest`
2. Use `snapshot()` for single theme or `snapshotBothThemes()` for light+dark
3. Run `recordRoborazziDebug` to generate golden images
4. Commit the new PNG files in `src/test/snapshots/images/`

### Updating Existing Screenshots

1. Make your UI changes in `shared-ui`
2. Run `verifyRoborazziDebug` - it will fail if visuals changed
3. Run `compareRoborazziDebug` to generate diff images
4. Review diffs in `build/outputs/roborazzi/`
5. If changes are intentional, run `recordRoborazziDebug`
6. Commit the updated golden images

### CI Behavior

- PRs touching `shared-ui`, `shared-domain`, or this module trigger verification
- Failed runs upload diff artifacts for review
- Download artifacts to see what changed

## Naming Convention

```
{TestClassName}_{testName}_{theme}.png
```

Examples:
- `SuggestionChipScreenshots_short_light.png`
- `SuggestionChipScreenshots_short_dark.png`
- `ChatScreenScreenshots_conversation_light.png`

## Device Configuration

Tests run on simulated Pixel 5-like device:
- Android SDK 33
- 400dp × 800dp @ xhdpi
- 1% pixel difference threshold
```

**Step 2: Commit**

```bash
git add shared-ui-screenshots/README.md
git commit -m "docs: add README for shared-ui-screenshots module"
```

---

## Phase 5: Final Verification

### Task 5.1: Run Full Test Suite

**Step 1: Run all screenshot tests**

Run: `./gradlew :shared-ui-screenshots:testDebugUnitTest`
Expected: All tests pass

**Step 2: Verify all golden images**

Run: `./gradlew :shared-ui-screenshots:verifyRoborazziDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Count golden images**

```bash
find shared-ui-screenshots/src/test/snapshots/images -name "*.png" | wc -l
```

Expected: ~40-50 images (depends on final component count)

---

### Task 5.2: Create Summary Commit

**Step 1: Review all changes**

```bash
git log --oneline testing...HEAD
```

**Step 2: Verify branch is clean**

```bash
git status
```

Expected: `nothing to commit, working tree clean`

---

## Summary

### Files Created

| File | Purpose |
|------|---------|
| `shared-ui-screenshots/build.gradle.kts` | Module build config |
| `shared-ui-screenshots/README.md` | Developer documentation |
| `shared-ui-screenshots/src/test/.../ScreenshotTest.kt` | Base test class |
| `shared-ui-screenshots/src/test/.../SuggestionChipScreenshots.kt` | Chip tests |
| `shared-ui-screenshots/src/test/.../SuggestionGridScreenshots.kt` | Grid tests |
| `shared-ui-screenshots/src/test/.../TopBarScreenshots.kt` | TopBar tests |
| `shared-ui-screenshots/src/test/.../WelcomeSectionScreenshots.kt` | Welcome tests |
| `shared-ui-screenshots/src/test/.../DisclaimerScreenshots.kt` | Disclaimer tests |
| `shared-ui-screenshots/src/test/.../ContextChipScreenshots.kt` | ContextChip tests |
| `shared-ui-screenshots/src/test/.../ChatInputScreenshots.kt` | Input tests |
| `shared-ui-screenshots/src/test/.../ChatScreenTestData.kt` | Test data factory |
| `shared-ui-screenshots/src/test/.../ChatScreenScreenshots.kt` | Screen tests |
| `.github/workflows/screenshot-tests.yml` | CI workflow |

### Files Modified

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Added Roborazzi dependencies |
| `settings.gradle.kts` | Added module registration |
| `.gitignore` | Added Roborazzi exclusions |

### Gradle Commands

```bash
# Record all golden images
./gradlew :shared-ui-screenshots:recordRoborazziDebug

# Verify all screenshots
./gradlew :shared-ui-screenshots:verifyRoborazziDebug

# Run specific test class
./gradlew :shared-ui-screenshots:testDebugUnitTest --tests "*.ChatScreenScreenshots"
```
