# PRD: Snapshot Testing Implementation

**Date:** 2026-01-31
**Branch:** testing
**Status:** Draft
**Author:** Claude + Developer

---

## 1. Overview

### Problem Statement

The CV Agent project lacks visual regression testing. UI changes can introduce unintended visual regressions that unit tests don't catch. The [testing analysis report](../kmp/report/v1.0.0-8-g10e1a6f-2026-01-31-main/testing.md) identified this as a key gap:

- No Roborazzi/Paparazzi snapshot testing
- No @Preview functions for composables
- Limited UI component test coverage

### Goals

1. **Catch visual regressions** before they reach production
2. **Document component states** through golden images
3. **Speed up UI review** - reviewers see visual diffs in PRs
4. **Enable confident refactoring** - change internals without breaking appearance

### Success Criteria

| Metric | Target |
|--------|--------|
| Component coverage | 100% of shared-ui composables |
| State coverage | All meaningful states per component |
| Theme coverage | Light + Dark for all components |
| CI integration | Block PRs on snapshot failures |
| Golden image review | Visual diff artifacts on failure |

### Scope

| In Scope | Out of Scope |
|----------|--------------|
| shared-ui composables | iOS-specific rendering |
| Android screenshots via Roborazzi | Desktop screenshots (future) |
| Light/Dark themes | Dynamic color themes |
| Phone form factor | Tablet/foldable (future) |

---

## 2. Technical Architecture

### Technology Choice: Roborazzi

**Why Roborazzi over alternatives:**

| Aspect | Roborazzi | Paparazzi | PreviewScanner |
|--------|-----------|-----------|----------------|
| KMP Support | Best | Android only | Partial |
| Speed | Fast (JVM) | Fastest (JVM) | Depends |
| Accuracy | Good (Robolectric) | Excellent (layoutlib) | Same as backend |
| Setup | Medium | Easy | Needs @Preview first |
| Future-proof | Desktop/iOS support | Android only | Limited |

**Decision:** Roborazzi - best fit for KMP project with future multiplatform expansion.

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CI Pipeline                          │
├─────────────────────────────────────────────────────────────┤
│  PR opened → verifyRoborazziDebug → Pass/Fail + Diff Report │
│  Main merged → recordRoborazziDebug → Update golden images  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Test Infrastructure                       │
├─────────────────────────────────────────────────────────────┤
│  ScreenshotTest (base)       │  Theme wrappers              │
│  - configures Roborazzi      │  - ArcaneTheme(dark=false)   │
│  - sets device config        │  - ArcaneTheme(dark=true)    │
│  - handles naming            │  - Surface background        │
├─────────────────────────────────────────────────────────────┤
│                    Screenshot Tests                          │
├─────────────────────────────────────────────────────────────┤
│  MessageBubbleScreenshots    │  ChatScreenScreenshots       │
│  SuggestionChipScreenshots   │  TopBarScreenshots           │
│  SuggestionGridScreenshots   │  InputFieldScreenshots       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Golden Images Storage                     │
├─────────────────────────────────────────────────────────────┤
│  shared-ui-screenshots/src/test/snapshots/                  │
│  ├── images/                  # Golden reference images     │
│  │   ├── MessageBubbleScreenshots_user_short_light.png      │
│  │   ├── MessageBubbleScreenshots_user_short_dark.png       │
│  │   └── ...                                                │
│  └── compare/                 # Generated diffs (gitignored)│
└─────────────────────────────────────────────────────────────┘
```

### Module Structure

```
cv-agent/
├── shared-ui/                          # Production composables (unchanged)
│   └── src/commonMain/kotlin/.../ui/
│       ├── components/
│       │   ├── MessageBubble.kt
│       │   ├── SuggestionChip.kt
│       │   ├── SuggestionChipsGrid.kt
│       │   ├── TopBar.kt
│       │   └── ChatInputField.kt
│       └── ChatScreen.kt
│
├── shared-ui-screenshots/              # NEW: Screenshot test module
│   ├── build.gradle.kts
│   └── src/test/
│       ├── kotlin/io/github/devmugi/cv/agent/ui/screenshots/
│       │   ├── ScreenshotTest.kt               # Base class
│       │   ├── MessageBubbleScreenshots.kt
│       │   ├── SuggestionChipScreenshots.kt
│       │   ├── SuggestionGridScreenshots.kt
│       │   ├── TopBarScreenshots.kt
│       │   ├── InputFieldScreenshots.kt
│       │   └── ChatScreenScreenshots.kt
│       └── snapshots/
│           └── images/                          # Golden images (committed)
│
├── gradle/libs.versions.toml           # Add Roborazzi dependencies
└── settings.gradle.kts                 # Add: include(":shared-ui-screenshots")
```

### Dependencies

```toml
# gradle/libs.versions.toml - additions

[versions]
roborazzi = "1.40.0"

[libraries]
roborazzi = { module = "io.github.takahirom.roborazzi:roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { module = "io.github.takahirom.roborazzi:roborazzi-compose", version.ref = "roborazzi" }
roborazzi-junit-rule = { module = "io.github.takahirom.roborazzi:roborazzi-junit-rule", version.ref = "roborazzi" }

[plugins]
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
```

### Module Build Configuration

```kotlin
// shared-ui-screenshots/build.gradle.kts
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
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
```

---

## 3. Implementation Phases

### Phase 1: Foundation

**Goal:** Create module, configure Roborazzi, verify setup with one test.

**Estimated Duration:** ~2 hours

**Tasks:**

| # | Task | File |
|---|------|------|
| 1.1 | Add Roborazzi to version catalog | `gradle/libs.versions.toml` |
| 1.2 | Create screenshot module | `shared-ui-screenshots/build.gradle.kts` |
| 1.3 | Register module in settings | `settings.gradle.kts` |
| 1.4 | Create base test class | `ScreenshotTest.kt` |
| 1.5 | Create first screenshot test | `SuggestionChipScreenshots.kt` |
| 1.6 | Record initial golden image | Run `recordRoborazziDebug` |
| 1.7 | Commit golden image | `snapshots/images/` |

**Base Test Class Implementation:**

```kotlin
// ScreenshotTest.kt
package io.github.devmugi.cv.agent.ui.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.devmugi.design.arcane.foundation.ArcaneTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w400dp-h800dp-xhdpi")
abstract class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "src/test/snapshots/images",
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(
                    changeThreshold = 0.01f  // 1% pixel difference tolerance
                )
            )
        )
    )

    /**
     * Capture a screenshot with the given name and theme.
     */
    protected fun snapshot(
        name: String,
        darkTheme: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val themeSuffix = if (darkTheme) "dark" else "light"
        val fileName = "${this::class.simpleName}_${name}_$themeSuffix.png"

        composeRule.setContent {
            ArcaneTheme(darkTheme = darkTheme) {
                Surface(color = ArcaneTheme.colors.background) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        content()
                    }
                }
            }
        }

        composeRule.onRoot().captureRoboImage(fileName)
    }

    /**
     * Capture both light and dark theme variants.
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

**Acceptance Criteria:**

- [ ] `./gradlew :shared-ui-screenshots:testDebugUnitTest` compiles and runs
- [ ] `./gradlew :shared-ui-screenshots:recordRoborazziDebug` generates golden image
- [ ] `./gradlew :shared-ui-screenshots:verifyRoborazziDebug` passes
- [ ] Golden image committed to repository

---

### Phase 2: Component Screenshots

**Goal:** Cover all shared-ui components with screenshot tests.

**Estimated Duration:** ~3 hours

**Components & States:**

| Component | States | Tests |
|-----------|--------|-------|
| MessageBubble | user, assistant, streaming, markdown, long-text | 5 |
| SuggestionChip | short, medium, long, pressed | 4 |
| SuggestionChipsGrid | 1-chip, 3-chips, 6-chips, overflow | 4 |
| TopBar | default, with-subtitle, scrolled | 3 |
| ChatInputField | empty, with-text, disabled, sending | 4 |
| **Total** | | **20** |

**Example: MessageBubbleScreenshots.kt**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.ui.components.MessageBubble
import org.junit.Test

class MessageBubbleScreenshots : ScreenshotTest() {

    @Test
    fun userMessage_short() = snapshot("user_short") {
        MessageBubble(
            message = Message.user("Hello!"),
            isStreaming = false
        )
    }

    @Test
    fun userMessage_long() = snapshot("user_long") {
        MessageBubble(
            message = Message.user(
                "This is a much longer message that should wrap " +
                "across multiple lines to test the text layout " +
                "behavior in the message bubble component."
            ),
            isStreaming = false
        )
    }

    @Test
    fun assistantMessage_plain() = snapshot("assistant_plain") {
        MessageBubble(
            message = Message.assistant(
                "I'd be happy to help you with that question."
            ),
            isStreaming = false
        )
    }

    @Test
    fun assistantMessage_streaming() = snapshot("assistant_streaming") {
        MessageBubble(
            message = Message.assistant("I'm thinking about your question..."),
            isStreaming = true
        )
    }

    @Test
    fun assistantMessage_markdown() = snapshot("assistant_markdown") {
        MessageBubble(
            message = Message.assistant(
                """
                Here's what I can help with:

                - **Android development** with Kotlin
                - Jetpack Compose UI
                - Architecture patterns (MVI, MVVM)

                Would you like to know more?
                """.trimIndent()
            ),
            isStreaming = false
        )
    }
}
```

**Example: SuggestionChipScreenshots.kt**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.SuggestionChip
import org.junit.Test

class SuggestionChipScreenshots : ScreenshotTest() {

    @Test
    fun chip_short() = snapshot("short") {
        SuggestionChip(text = "Skills", onClick = {})
    }

    @Test
    fun chip_medium() = snapshot("medium") {
        SuggestionChip(text = "Tell me about your experience", onClick = {})
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

**Example: SuggestionGridScreenshots.kt**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.SuggestionChipsGrid
import org.junit.Test

class SuggestionGridScreenshots : ScreenshotTest() {

    @Test
    fun grid_singleChip() = snapshot("single") {
        SuggestionChipsGrid(
            suggestions = listOf("Tell me about yourself"),
            onSuggestionClick = {}
        )
    }

    @Test
    fun grid_threeChips() = snapshot("three") {
        SuggestionChipsGrid(
            suggestions = listOf(
                "Experience",
                "Skills",
                "Projects"
            ),
            onSuggestionClick = {}
        )
    }

    @Test
    fun grid_sixChips() = snapshot("six_two_rows") {
        SuggestionChipsGrid(
            suggestions = listOf(
                "Android",
                "Kotlin",
                "Compose",
                "Architecture",
                "Testing",
                "CI/CD"
            ),
            onSuggestionClick = {}
        )
    }

    @Test
    fun grid_overflow() = snapshot("overflow") {
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

**Acceptance Criteria:**

- [ ] MessageBubbleScreenshots: 5 tests passing
- [ ] SuggestionChipScreenshots: 4 tests passing
- [ ] SuggestionGridScreenshots: 4 tests passing
- [ ] TopBarScreenshots: 3 tests passing
- [ ] InputFieldScreenshots: 4 tests passing
- [ ] All 20 golden images committed

---

### Phase 3: Screen-Level Screenshots

**Goal:** Capture full ChatScreen in all meaningful states.

**Estimated Duration:** ~2 hours

**Screen States:**

| State | Description |
|-------|-------------|
| empty | No messages, initial state |
| single_message | User sent one message |
| conversation | Multiple back-and-forth messages |
| streaming | Assistant is currently typing |
| with_suggestions | Suggestion chips visible after response |
| long_conversation | Scrollable message list (5+ messages) |

**Implementation: ChatScreenScreenshots.kt**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.ui.ChatScreenContent
import io.github.devmugi.cv.agent.ui.ChatState
import org.junit.Test

class ChatScreenScreenshots : ScreenshotTest() {

    @Test
    fun screen_empty() = snapshot("empty") {
        ChatScreenContent(
            state = ChatState(),
            onSendMessage = {},
            onSuggestionClick = {}
        )
    }

    @Test
    fun screen_singleMessage() = snapshot("single_message") {
        ChatScreenContent(
            state = ChatState(
                messages = listOf(
                    Message.user("What's your Android experience?")
                )
            ),
            onSendMessage = {},
            onSuggestionClick = {}
        )
    }

    @Test
    fun screen_conversation() = snapshot("conversation") {
        ChatScreenContent(
            state = ChatState(
                messages = listOf(
                    Message.user("What's your Android experience?"),
                    Message.assistant(
                        "I have 8+ years of Android development experience, " +
                        "including Kotlin, Jetpack Compose, and modern architecture patterns."
                    ),
                    Message.user("Tell me about a challenging project"),
                    Message.assistant(
                        "One challenging project was building a real-time " +
                        "collaboration feature that required WebSocket integration " +
                        "and complex state synchronization."
                    )
                )
            ),
            onSendMessage = {},
            onSuggestionClick = {}
        )
    }

    @Test
    fun screen_streaming() = snapshot("streaming") {
        ChatScreenContent(
            state = ChatState(
                messages = listOf(
                    Message.user("Explain your testing approach"),
                    Message.assistant("I believe in comprehensive testing...")
                ),
                isStreaming = true,
                streamingMessageId = "msg_assistant_1"
            ),
            onSendMessage = {},
            onSuggestionClick = {}
        )
    }

    @Test
    fun screen_withSuggestions() = snapshot("with_suggestions") {
        ChatScreenContent(
            state = ChatState(
                messages = listOf(
                    Message.user("Hi"),
                    Message.assistant("Hello! I'm happy to discuss my experience. What would you like to know?")
                ),
                suggestions = listOf(
                    "Tell me about your Android experience",
                    "What projects have you worked on?",
                    "Describe your technical skills"
                )
            ),
            onSendMessage = {},
            onSuggestionClick = {}
        )
    }

    @Test
    fun screen_longConversation() = snapshot("long_conversation") {
        ChatScreenContent(
            state = ChatState(
                messages = listOf(
                    Message.user("Hi"),
                    Message.assistant("Hello! How can I help?"),
                    Message.user("Tell me about Android"),
                    Message.assistant("I specialize in Android development..."),
                    Message.user("What about Compose?"),
                    Message.assistant("Jetpack Compose is my preferred UI toolkit..."),
                    Message.user("Testing?"),
                    Message.assistant("I follow TDD practices with comprehensive test coverage.")
                )
            ),
            onSendMessage = {},
            onSuggestionClick = {}
        )
    }
}
```

**Acceptance Criteria:**

- [ ] 6 ChatScreen state snapshots created
- [ ] States cover empty, conversation, streaming, suggestions
- [ ] Long conversation shows scroll behavior
- [ ] All golden images committed

---

### Phase 4: Theme Variants

**Goal:** Capture dark theme for all components and screens.

**Estimated Duration:** ~1.5 hours

**Approach:**

Update all existing tests to use `snapshotBothThemes()` instead of `snapshot()`:

```kotlin
// Before
@Test
fun userMessage_short() = snapshot("user_short") {
    MessageBubble(message = Message.user("Hello!"), isStreaming = false)
}

// After
@Test
fun userMessage_short() = snapshotBothThemes("user_short") {
    MessageBubble(message = Message.user("Hello!"), isStreaming = false)
}
```

**Coverage Summary:**

| Category | Light | Dark | Total |
|----------|-------|------|-------|
| MessageBubble | 5 | 5 | 10 |
| SuggestionChip | 4 | 4 | 8 |
| SuggestionChipsGrid | 4 | 4 | 8 |
| TopBar | 3 | 3 | 6 |
| ChatInputField | 4 | 4 | 8 |
| ChatScreen | 6 | 6 | 12 |
| **Total** | **26** | **26** | **52** |

**Acceptance Criteria:**

- [ ] All 26 light theme snapshots exist
- [ ] All 26 dark theme snapshots exist
- [ ] Total: 52 golden images committed
- [ ] All verification passes

---

### Phase 5: CI Integration & Polish

**Goal:** Automate verification in CI, add failure reporting.

**Estimated Duration:** ~2 hours

**Tasks:**

| # | Task | File |
|---|------|------|
| 5.1 | Create GitHub Actions workflow | `.github/workflows/screenshot-tests.yml` |
| 5.2 | Configure artifact upload on failure | Workflow |
| 5.3 | Add PR comment on failure | Workflow |
| 5.4 | Update .gitignore for compare output | `.gitignore` |
| 5.5 | Add developer documentation | `shared-ui-screenshots/README.md` |

**GitHub Actions Workflow:**

```yaml
# .github/workflows/screenshot-tests.yml
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
          retention-days: 7

      - name: Comment PR on failure
        if: failure()
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.createComment({
              owner: context.repo.owner,
              repo: context.repo.repo,
              issue_number: context.issue.number,
              body: `## Screenshot Tests Failed

              Visual differences detected. Please review the changes:

              1. Download the \`screenshot-diff-report\` artifact
              2. Review the diff images
              3. If intentional, run \`./gradlew :shared-ui-screenshots:recordRoborazziDebug\`
              4. Commit the updated golden images

              [View workflow run](https://github.com/${{ github.repository }}/actions/runs/${{ github.run_id }})`
            })
```

**Developer Documentation (README.md):**

```markdown
# shared-ui-screenshots

Screenshot tests for CV Agent UI components using Roborazzi.

## Quick Reference

| Command | Purpose |
|---------|---------|
| `./gradlew :shared-ui-screenshots:testDebugUnitTest` | Run all tests |
| `./gradlew :shared-ui-screenshots:recordRoborazziDebug` | Record golden images |
| `./gradlew :shared-ui-screenshots:verifyRoborazziDebug` | Verify against golden |
| `./gradlew :shared-ui-screenshots:compareRoborazziDebug` | Generate diff images |

## Workflow

### Adding New Component Screenshots

1. Create test class extending `ScreenshotTest`
2. Use `snapshot()` or `snapshotBothThemes()` helper
3. Run `recordRoborazziDebug` to generate golden images
4. Commit the new images in `src/test/snapshots/images/`

### Updating Existing Screenshots

1. Make your UI changes
2. Run `verifyRoborazziDebug` - it will fail
3. Review the diff in `build/outputs/roborazzi/`
4. If changes are intentional, run `recordRoborazziDebug`
5. Commit the updated golden images

### CI Behavior

- PRs are blocked if screenshots don't match
- Diff artifacts are uploaded on failure
- Review artifacts to understand what changed
```

**Gitignore Additions:**

```gitignore
# Roborazzi comparison output (generated, not committed)
**/snapshots/images/*_compare.png
**/snapshots/images/*_actual.png
```

**Acceptance Criteria:**

- [ ] GitHub Actions workflow created and tested
- [ ] PRs to shared-ui trigger screenshot verification
- [ ] Failed runs upload diff artifacts
- [ ] README documents developer workflow
- [ ] .gitignore excludes generated diff files

---

## 4. Summary

### Deliverables by Phase

| Phase | Focus | Golden Images | Duration |
|-------|-------|---------------|----------|
| 1 | Foundation | 1 | ~2h |
| 2 | Components | 20 | ~3h |
| 3 | Screens | 6 | ~2h |
| 4 | Themes | +26 (total 52) | ~1.5h |
| 5 | CI | 0 (infrastructure) | ~2h |
| **Total** | | **52** | **~10.5h** |

### File Changes Summary

**New Files:**
- `shared-ui-screenshots/build.gradle.kts`
- `shared-ui-screenshots/src/test/kotlin/.../screenshots/*.kt` (7 files)
- `shared-ui-screenshots/src/test/snapshots/images/*.png` (52 images)
- `shared-ui-screenshots/README.md`
- `.github/workflows/screenshot-tests.yml`

**Modified Files:**
- `gradle/libs.versions.toml` (add Roborazzi)
- `settings.gradle.kts` (add module)
- `.gitignore` (exclude compare output)

### Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Robolectric rendering differs from device | Medium | Low | Accept small differences; 1% threshold |
| Golden images bloat repo | Low | Medium | PNG compression; consider git LFS if >100MB |
| Flaky tests | Low | High | Fixed device config; no animations |
| Slow CI | Medium | Medium | Run only on shared-ui changes |

### Future Enhancements (Out of Scope)

- Desktop screenshot testing with Roborazzi Desktop
- Tablet/foldable form factors
- @Preview function generation with ComposablePreviewScanner
- Accessibility testing (large text, high contrast)
- Animation snapshot testing

---

## 5. Appendix

### Gradle Tasks Reference

```bash
# Record new golden images (after intentional UI changes)
./gradlew :shared-ui-screenshots:recordRoborazziDebug

# Verify screenshots match golden images (CI)
./gradlew :shared-ui-screenshots:verifyRoborazziDebug

# Compare and generate visual diff images (debugging)
./gradlew :shared-ui-screenshots:compareRoborazziDebug

# Clean all recorded images
./gradlew :shared-ui-screenshots:cleanRoborazziDebug

# Run specific test
./gradlew :shared-ui-screenshots:testDebugUnitTest --tests "*.MessageBubbleScreenshots"
```

### Naming Convention

```
{TestClassName}_{testName}_{theme}.png

Examples:
- MessageBubbleScreenshots_user_short_light.png
- MessageBubbleScreenshots_user_short_dark.png
- ChatScreenScreenshots_conversation_light.png
- SuggestionChipScreenshots_long_dark.png
```

### Device Configuration

```kotlin
@Config(
    sdk = [33],                    // Android 13
    qualifiers = "w400dp-h800dp-xhdpi"  // Pixel 5-like
)
```

---

*Document version: 1.0 | Created: 2026-01-31*
