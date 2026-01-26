# Firebase Analytics Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Wire up 20 analytics events across ChatViewModel, ChatScreen, TopBar, and Career screens.

**Architecture:** Centralized logging in ViewModel for action callbacks, parameter threading for screen composables, Firebase BOM for dependency management.

**Tech Stack:** Firebase Analytics via BOM 34.7.0, Kotlin Multiplatform, Compose Multiplatform, Koin DI

---

## Task 1: Update Firebase BOM Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml:49-158`
- Modify: `android-app/build.gradle.kts:81-106`

**Step 1: Update libs.versions.toml**

Add firebase-bom version and update firebase-analytics to use BOM:

```toml
# In [versions] section, add after line 50:
firebase-bom = "34.7.0"

# In [libraries] section, replace line 158:
# OLD: firebase-analytics = { module = "com.google.firebase:firebase-analytics-ktx", version = "22.4.0" }
# NEW:
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebase-bom" }
firebase-analytics = { module = "com.google.firebase:firebase-analytics-ktx" }
```

**Step 2: Update android-app/build.gradle.kts**

Add BOM platform dependency. After line 100 (kotlinx.serialization.json), add:

```kotlin
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
```

**Step 3: Sync and verify build**

Run: `./gradlew :android-app:assembleDebug --dry-run`
Expected: BUILD SUCCESSFUL (configuration phase passes)

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml android-app/build.gradle.kts
git commit -m "build: update Firebase to BOM 34.7.0

Use Firebase BOM for version management instead of direct version.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Add Analytics Tests for ChatViewModel

**Files:**
- Modify: `shared-agent/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Create FakeAnalytics test helper**

Add after the `FakeGroqApiClient` class (around line 200):

```kotlin
class FakeAnalytics : Analytics {
    val loggedEvents = mutableListOf<AnalyticsEvent>()

    override fun logEvent(event: AnalyticsEvent) {
        loggedEvents.add(event)
    }

    override fun setUserId(userId: String?) {}
    override fun setUserProperty(name: String, value: String?) {}
    override fun setCurrentScreen(screenName: String, screenClass: String?) {}

    fun clear() = loggedEvents.clear()

    inline fun <reified T : AnalyticsEvent> findEvent(): T? =
        loggedEvents.filterIsInstance<T>().firstOrNull()

    inline fun <reified T : AnalyticsEvent> hasEvent(): Boolean =
        loggedEvents.any { it is T }
}
```

Add import at top:
```kotlin
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
```

**Step 2: Add FakeAnalytics to test setup**

Update setup() to include analytics:

```kotlin
private lateinit var fakeAnalytics: FakeAnalytics

@BeforeTest
fun setup() {
    Logger.setMinSeverity(Severity.Assert)
    Dispatchers.setMain(testDispatcher)
    fakeApiClient = FakeGroqApiClient()
    fakeAnalytics = FakeAnalytics()
    viewModel = ChatViewModel(
        apiClient = fakeApiClient,
        promptBuilder = SystemPromptBuilder(),
        suggestionExtractor = SuggestionExtractor(),
        dataProvider = null,
        analytics = fakeAnalytics
    )
}
```

**Step 3: Add test for MessageSent event**

```kotlin
@Test
fun sendMessageLogsMessageSentEvent() = runTest {
    viewModel.sendMessage("Hello world")
    advanceUntilIdle()

    val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.MessageSent>()
    assertNotNull(event, "MessageSent event should be logged")
    assertEquals(11, event.messageLength)
    assertEquals(1, event.turnNumber)
}
```

**Step 4: Add test for HistoryCleared event**

```kotlin
@Test
fun clearHistoryLogsHistoryClearedEvent() = runTest {
    // Send a message first to have history
    viewModel.sendMessage("Test message")
    advanceUntilIdle()
    fakeAnalytics.clear()

    viewModel.clearHistory()

    val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.HistoryCleared>()
    assertNotNull(event, "HistoryCleared event should be logged")
    assertTrue(event.messageCount >= 1)
}
```

**Step 5: Run tests to verify they fail**

Run: `./gradlew :shared-agent:testDebugUnitTest --tests "*MessageSent*" --tests "*HistoryCleared*" -i 2>&1 | tail -20`
Expected: Tests FAIL (events not logged yet)

**Step 6: Commit test file**

```bash
git add shared-agent/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt
git commit -m "test: add analytics event tests for ChatViewModel

Tests for MessageSent and HistoryCleared events (currently failing).

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Implement ChatViewModel Direct Events

**Files:**
- Modify: `shared-agent/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt`

**Step 1: Add MessageSent logging in sendMessage()**

In `sendMessage()` method (around line 95), add after `lastUserMessage = content`:

```kotlin
fun sendMessage(content: String) {
    Logger.d(TAG) { "Sending message - length: ${content.length}" }
    lastUserMessage = content

    // Log analytics event
    analytics.logEvent(
        AnalyticsEvent.Chat.MessageSent(
            messageLength = content.length,
            sessionId = sessionId,
            turnNumber = turnNumber + 1
        )
    )

    val userMessage = Message(role = MessageRole.USER, content = content)
    // ... rest unchanged
```

**Step 2: Add HistoryCleared logging in clearHistory()**

In `clearHistory()` method (around line 129), add before clearing:

```kotlin
@OptIn(ExperimentalUuidApi::class)
fun clearHistory() {
    val messageCount = _state.value.messages.size
    Logger.d(TAG) { "Clearing chat history" }

    // Log analytics event
    analytics.logEvent(
        AnalyticsEvent.Chat.HistoryCleared(
            messageCount = messageCount,
            sessionId = sessionId
        )
    )

    _state.update { current -> ChatState(projectNames = current.projectNames) }
    // ... rest unchanged
```

**Step 3: Run tests to verify they pass**

Run: `./gradlew :shared-agent:testDebugUnitTest --tests "*MessageSent*" --tests "*HistoryCleared*"`
Expected: Tests PASS

**Step 4: Commit**

```bash
git add shared-agent/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt
git commit -m "feat(analytics): log MessageSent and HistoryCleared events

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Add ResponseCompleted and ErrorDisplayed Events

**Files:**
- Modify: `shared-agent/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt`
- Modify: `shared-agent/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Add test for ResponseCompleted**

```kotlin
@Test
fun streamResponseLogsResponseCompletedEvent() = runTest {
    fakeApiClient.responseChunks = listOf("Hello", " world")

    viewModel.sendMessage("Test")
    advanceUntilIdle()

    val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.ResponseCompleted>()
    assertNotNull(event, "ResponseCompleted event should be logged")
    assertTrue(event.responseTimeMs >= 0)
}
```

**Step 2: Add test for ErrorDisplayed**

```kotlin
@Test
fun streamResponseErrorLogsErrorDisplayedEvent() = runTest {
    fakeApiClient.shouldFail = true
    fakeApiClient.failureException = GroqApiException.NetworkError("Test error")

    viewModel.sendMessage("Test")
    advanceUntilIdle()

    val event = fakeAnalytics.findEvent<AnalyticsEvent.Error.ErrorDisplayed>()
    assertNotNull(event, "ErrorDisplayed event should be logged")
    assertEquals(AnalyticsEvent.Error.ErrorType.NETWORK, event.errorType)
}
```

**Step 3: Run tests to verify they fail**

Run: `./gradlew :shared-agent:testDebugUnitTest --tests "*ResponseCompleted*" --tests "*ErrorDisplayed*"`
Expected: Tests FAIL

**Step 4: Implement ResponseCompleted in streamResponse()**

Add `streamStartTime` variable and log in onComplete:

```kotlin
@OptIn(ExperimentalUuidApi::class)
private suspend fun streamResponse() {
    turnNumber++
    val currentTurn = turnNumber
    val currentSessionId = sessionId
    val streamStartTime = System.currentTimeMillis()  // Add this line
    Logger.d(TAG) { "Starting turn $currentTurn in session $currentSessionId" }

    // ... existing code until onComplete ...

    onComplete = {
        val responseTimeMs = System.currentTimeMillis() - streamStartTime
        Logger.d(TAG) { "Stream completed - content length: ${streamedContent.length}" }

        // Log analytics event
        analytics.logEvent(
            AnalyticsEvent.Chat.ResponseCompleted(
                responseTimeMs = responseTimeMs,
                tokenCount = null,
                sessionId = currentSessionId
            )
        )

        val extractionResult = suggestionExtractor.extract(streamedContent)
        // ... rest unchanged
    },
```

**Step 5: Implement ErrorDisplayed in streamResponse()**

Update onError handler:

```kotlin
    onError = { exception ->
        Logger.w(TAG) { "Stream error: ${exception.javaClass.simpleName}" }

        val errorType = when (exception) {
            is GroqApiException.NetworkError -> AnalyticsEvent.Error.ErrorType.NETWORK
            is GroqApiException.RateLimitError -> AnalyticsEvent.Error.ErrorType.RATE_LIMIT
            is GroqApiException.AuthError -> AnalyticsEvent.Error.ErrorType.AUTH
            is GroqApiException.ApiError -> AnalyticsEvent.Error.ErrorType.API
        }

        // Log analytics event
        analytics.logEvent(
            AnalyticsEvent.Error.ErrorDisplayed(
                errorType = errorType,
                errorMessage = exception.message,
                sessionId = currentSessionId
            )
        )

        val error = when (exception) {
            is GroqApiException.NetworkError -> ChatError.Network(exception.reason)
            // ... rest unchanged
```

**Step 6: Run tests to verify they pass**

Run: `./gradlew :shared-agent:testDebugUnitTest --tests "*ResponseCompleted*" --tests "*ErrorDisplayed*"`
Expected: Tests PASS

**Step 7: Commit**

```bash
git add shared-agent/
git commit -m "feat(analytics): log ResponseCompleted and ErrorDisplayed events

Track response time and error types for observability.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Add ChatViewModel Callback Wrappers

**Files:**
- Modify: `shared-agent/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt`
- Modify: `shared-agent/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Add tests for callback wrapper methods**

```kotlin
@Test
fun onMessageCopiedLogsEvent() = runTest {
    fakeApiClient.responseChunks = listOf("Response content here")
    viewModel.sendMessage("Test")
    advanceUntilIdle()

    val assistantMessage = viewModel.state.value.messages.find { it.role == MessageRole.ASSISTANT }
    assertNotNull(assistantMessage)
    fakeAnalytics.clear()

    viewModel.onMessageCopied(assistantMessage.id)

    val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.MessageCopied>()
    assertNotNull(event, "MessageCopied event should be logged")
    assertEquals(assistantMessage.id, event.messageId)
}

@Test
fun onMessageLikedLogsEvent() = runTest {
    viewModel.onMessageLiked("test-message-id")

    val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.MessageLiked>()
    assertNotNull(event)
    assertEquals("test-message-id", event.messageId)
}

@Test
fun onMessageDislikedLogsEvent() = runTest {
    viewModel.onMessageDisliked("test-message-id")

    val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.MessageDisliked>()
    assertNotNull(event)
    assertEquals("test-message-id", event.messageId)
}

@Test
fun onRegenerateClickedLogsEventAndRetries() = runTest {
    fakeApiClient.responseChunks = listOf("First response")
    viewModel.sendMessage("Test")
    advanceUntilIdle()
    fakeAnalytics.clear()

    viewModel.onRegenerateClicked("test-id")
    advanceUntilIdle()

    val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.RegenerateClicked>()
    assertNotNull(event)
}

@Test
fun onProjectSuggestionClickedLogsEvent() = runTest {
    viewModel.onProjectSuggestionClicked("mcdonalds", 0)

    val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.SuggestionClicked>()
    assertNotNull(event)
    assertEquals("mcdonalds", event.projectId)
    assertEquals(0, event.position)
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :shared-agent:testDebugUnitTest --tests "*Copied*" --tests "*Liked*" --tests "*Disliked*" --tests "*Regenerate*" --tests "*SuggestionClicked*"`
Expected: Tests FAIL (methods don't exist)

**Step 3: Implement callback wrapper methods**

Add after `clearHistory()` method:

```kotlin
// ============ Analytics Callback Wrappers ============

fun onMessageCopied(messageId: String) {
    val message = _state.value.messages.find { it.id == messageId }
    analytics.logEvent(
        AnalyticsEvent.Chat.MessageCopied(
            messageId = messageId,
            messageLength = message?.content?.length ?: 0
        )
    )
}

fun onMessageLiked(messageId: String) {
    analytics.logEvent(AnalyticsEvent.Chat.MessageLiked(messageId = messageId))
}

fun onMessageDisliked(messageId: String) {
    analytics.logEvent(AnalyticsEvent.Chat.MessageDisliked(messageId = messageId))
}

fun onRegenerateClicked(messageId: String) {
    analytics.logEvent(
        AnalyticsEvent.Chat.RegenerateClicked(
            messageId = messageId,
            turnNumber = turnNumber
        )
    )
    retry()
}

fun onProjectSuggestionClicked(projectId: String, position: Int) {
    analytics.logEvent(
        AnalyticsEvent.Chat.SuggestionClicked(
            projectId = projectId,
            position = position
        )
    )
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :shared-agent:testDebugUnitTest --tests "*Copied*" --tests "*Liked*" --tests "*Disliked*" --tests "*Regenerate*" --tests "*SuggestionClicked*"`
Expected: Tests PASS

**Step 5: Commit**

```bash
git add shared-agent/
git commit -m "feat(analytics): add callback wrapper methods for UI events

Centralized logging for copy, like, dislike, regenerate, and suggestion clicks.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Add Analytics Parameter to ChatScreen

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`

**Step 1: Add analytics parameter and import**

Add import:
```kotlin
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
```

Update function signature (around line 64):
```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    state: ChatState,
    toastState: ArcaneToastState,
    onSendMessage: (String) -> Unit,
    analytics: Analytics = Analytics.NOOP,
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit = onSendMessage,
    onCopyMessage: (String) -> Unit = {},
    onShareMessage: (String) -> Unit = {},
    onLikeMessage: (String) -> Unit = {},
    onDislikeMessage: (String) -> Unit = {},
    onRegenerateMessage: (String) -> Unit = {},
    onClearHistory: (() -> Unit)? = null,
    onNavigateToCareerTimeline: () -> Unit = {},
    onNavigateToProject: (String) -> Unit = {}
) {
```

**Step 2: Add ScreenView logging**

After the variable declarations (around line 84), add:
```kotlin
    // Log screen view once on first composition
    LaunchedEffect(Unit) {
        analytics.logEvent(
            AnalyticsEvent.Navigation.ScreenView(
                screenName = AnalyticsEvent.Navigation.Screen.CHAT
            )
        )
    }
```

**Step 3: Verify build**

Run: `./gradlew :shared-ui:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt
git commit -m "feat(analytics): add Analytics parameter to ChatScreen

Log ScreenView event on first composition.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 7: Add Analytics to CVAgentTopBar

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/TopBar.kt`

**Step 1: Add imports and analytics parameter**

Add imports:
```kotlin
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
```

Update CVAgentTopBar signature:
```kotlin
@Composable
fun CVAgentTopBar(
    onCareerClick: () -> Unit = {},
    showContactBanner: Boolean = true,
    analytics: Analytics = Analytics.NOOP
) {
```

**Step 2: Wrap Career button click with logging**

Update the Career button (around line 78):
```kotlin
                ArcaneTextButton(
                    text = "Career",
                    onClick = {
                        analytics.logEvent(
                            AnalyticsEvent.Navigation.ScreenView(
                                screenName = AnalyticsEvent.Navigation.Screen.CAREER_TIMELINE,
                                previousScreen = AnalyticsEvent.Navigation.Screen.CHAT
                            )
                        )
                        onCareerClick()
                    },
                    style = ArcaneButtonStyle.Outlined(),
                    size = ArcaneButtonSize.Medium
                )
```

**Step 3: Add analytics parameter to ContactBanner**

Update ContactBanner signature and calls:
```kotlin
@Composable
private fun ContactBanner(
    onLinkedInClick: () -> Unit,
    onGitHubClick: () -> Unit,
    onEmailClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onCVClick: () -> Unit,
    onPdfClick: () -> Unit,
    analytics: Analytics,
    modifier: Modifier = Modifier
) {
```

**Step 4: Wrap each contact link click with logging**

Create wrapper function inside ContactBanner:
```kotlin
    fun logAndOpen(linkType: AnalyticsEvent.Link.LinkType, url: String, action: () -> Unit) {
        analytics.logEvent(
            AnalyticsEvent.Link.ExternalLinkClicked(
                linkType = linkType,
                url = url
            )
        )
        action()
    }
```

Update each ContactIconButton onClick:
```kotlin
            ContactIconButton(
                icon = SimpleIcons.Linkedin,
                contentDescription = "LinkedIn",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.LINKEDIN, "linkedin.com/in/denyshoncharenko") {
                        onLinkedInClick()
                    }
                },
                tint = LinkedInBlue
            )
            ContactIconButton(
                icon = SimpleIcons.Github,
                contentDescription = "GitHub",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.GITHUB, "github.com/devmugi") {
                        onGitHubClick()
                    }
                }
            )
            ContactIconButton(
                icon = Icons.Filled.Email,
                contentDescription = "Email",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.EMAIL, "aidevmugi@gmail.com") {
                        onEmailClick()
                    }
                }
            )
            ContactIconButton(
                icon = Icons.Filled.Phone,
                contentDescription = "Phone",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.PHONE, "+32470383388") {
                        onPhoneClick()
                    }
                }
            )
            ContactIconButton(
                icon = Icons.Filled.Language,
                contentDescription = "Website",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.CV_WEBSITE, "devmugi.github.io") {
                        onCVClick()
                    }
                }
            )
            ContactIconButton(
                icon = Icons.Filled.PictureAsPdf,
                contentDescription = "PDF CV",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.CV_PDF, "cv.pdf") {
                        onPdfClick()
                    }
                }
            )
```

**Step 5: Update CVAgentTopBar to pass analytics to ContactBanner**

```kotlin
        AnimatedVisibility(
            visible = showContactBanner,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ContactBanner(
                onLinkedInClick = {
                    uriHandler.openUri("https://www.linkedin.com/in/denyshoncharenko/")
                },
                onGitHubClick = {
                    uriHandler.openUri("https://github.com/devmugi")
                },
                onEmailClick = {
                    uriHandler.openUri("mailto:aidevmugi@gmail.com")
                },
                onPhoneClick = {
                    uriHandler.openUri("tel:+32470383388")
                },
                onCVClick = {
                    uriHandler.openUri("https://devmugi.github.io/devmugi/")
                },
                onPdfClick = {
                    uriHandler.openUri("https://raw.githubusercontent.com/devmugi/devmugi/main/cv/Denys%20Honcharenko%20CV.pdf")
                },
                analytics = analytics
            )
        }
```

**Step 6: Verify build**

Run: `./gradlew :shared-ui:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/TopBar.kt
git commit -m "feat(analytics): add link tracking to CVAgentTopBar

Log ExternalLinkClicked for all contact links and ScreenView for Career navigation.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 8: Thread Analytics from ChatScreen to TopBar

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`

**Step 1: Pass analytics to CVAgentTopBar**

Update the CVAgentTopBar call (around line 129):
```kotlin
        topBar = {
            CVAgentTopBar(
                onCareerClick = onNavigateToCareerTimeline,
                showContactBanner = showContactBanner,
                analytics = analytics
            )
        }
```

**Step 2: Verify build**

Run: `./gradlew :shared-ui:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt
git commit -m "feat(analytics): thread analytics from ChatScreen to TopBar

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 9: Add Analytics to Career Screens

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/CareerProjectsTimelineScreen.kt`
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/CareerProjectDetailsScreen.kt`

**Step 1: Update CareerProjectsTimelineScreen**

```kotlin
package io.github.devmugi.cv.agent.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
import io.github.devmugi.cv.agent.career.models.ProjectDataTimeline
import io.github.devmugi.cv.agent.career.ui.CareerProjectsTimelineScreenScaffold

@Composable
fun CareerProjectsTimelineScreen(
    projects: List<ProjectDataTimeline>,
    onProjectClick: (ProjectDataTimeline) -> Unit,
    onBackClick: () -> Unit,
    analytics: Analytics = Analytics.NOOP,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        analytics.logEvent(
            AnalyticsEvent.Navigation.ScreenView(
                screenName = AnalyticsEvent.Navigation.Screen.CAREER_TIMELINE
            )
        )
    }

    CareerProjectsTimelineScreenScaffold(
        projects = projects,
        onProjectDetailsClick = { project ->
            analytics.logEvent(
                AnalyticsEvent.Navigation.ProjectSelected(
                    projectId = project.id,
                    source = AnalyticsEvent.Navigation.SelectionSource.TIMELINE
                )
            )
            onProjectClick(project)
        },
        onBackClick = onBackClick,
        modifier = modifier
    )
}
```

**Step 2: Update CareerProjectDetailsScreen**

```kotlin
package io.github.devmugi.cv.agent.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent
import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.ui.CareerProjectDetailsScreenScaffold

@Composable
fun CareerProjectDetailsScreen(
    project: CareerProject,
    onBackClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    analytics: Analytics = Analytics.NOOP,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        analytics.logEvent(
            AnalyticsEvent.Navigation.ScreenView(
                screenName = AnalyticsEvent.Navigation.Screen.PROJECT_DETAILS
            )
        )
    }

    CareerProjectDetailsScreenScaffold(
        project = project,
        onBackClick = onBackClick,
        onLinkClick = { url ->
            analytics.logEvent(
                AnalyticsEvent.Link.ProjectLinkClicked(
                    projectId = project.id,
                    linkType = "external",
                    url = url
                )
            )
            onLinkClick(url)
        },
        modifier = modifier
    )
}
```

**Step 3: Verify build**

Run: `./gradlew :shared-ui:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/CareerProjectsTimelineScreen.kt
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/CareerProjectDetailsScreen.kt
git commit -m "feat(analytics): add tracking to Career screens

Log ScreenView, ProjectSelected, and ProjectLinkClicked events.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 10: Wire Analytics in MainActivity

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

**Step 1: Add imports**

```kotlin
import io.github.devmugi.cv.agent.analytics.Analytics
import org.koin.compose.koinInject
```

**Step 2: Get analytics instance in CVAgentApp**

In `CVAgentApp` composable, add after toastState:
```kotlin
@OptIn(ExperimentalResourceApi::class)
@Suppress("FunctionNaming")
@Composable
private fun CVAgentApp(
    onOpenUrl: (String) -> Unit,
    currentTheme: ThemeVariant,
    onThemeChange: (ThemeVariant) -> Unit
) {
    val toastState = rememberArcaneToastState()
    val analytics: Analytics = koinInject()
    // ... rest unchanged
```

**Step 3: Pass analytics to AppContent**

Update AppContent call:
```kotlin
    AppContent(
        currentScreen = currentScreen,
        state = state,
        toastState = toastState,
        viewModel = viewModel,
        careerProjects = dataResult.timelineProjects,
        careerProjectsMap = dataResult.projectsMap,
        selectedProject = selectedProject,
        onScreenChange = { currentScreen = it },
        onProjectSelect = { selectedProject = it },
        onOpenUrl = onOpenUrl,
        currentTheme = currentTheme,
        onThemeChange = onThemeChange,
        analytics = analytics
    )
```

**Step 4: Update AppContent signature and screen calls**

```kotlin
@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun AppContent(
    currentScreen: Screen,
    state: io.github.devmugi.cv.agent.domain.models.ChatState,
    toastState: ArcaneToastState,
    viewModel: ChatViewModel,
    careerProjects: List<ProjectDataTimeline>,
    careerProjectsMap: Map<String, CareerProject>,
    selectedProject: CareerProject?,
    onScreenChange: (Screen) -> Unit,
    onProjectSelect: (CareerProject?) -> Unit,
    onOpenUrl: (String) -> Unit,
    currentTheme: ThemeVariant,
    onThemeChange: (ThemeVariant) -> Unit,
    analytics: Analytics
) {
```

**Step 5: Wire analytics to all screens**

Update ChatScreen call:
```kotlin
                Screen.Chat -> ChatScreen(
                    state = state,
                    toastState = toastState,
                    onSendMessage = viewModel::sendMessage,
                    onSuggestionClick = viewModel::onSuggestionClicked,
                    onCopyMessage = viewModel::onMessageCopied,
                    onLikeMessage = viewModel::onMessageLiked,
                    onDislikeMessage = viewModel::onMessageDisliked,
                    onRegenerateMessage = viewModel::onRegenerateClicked,
                    onClearHistory = viewModel::clearHistory,
                    onNavigateToCareerTimeline = { onScreenChange(Screen.CareerTimeline) },
                    onNavigateToProject = { projectId ->
                        careerProjectsMap[projectId]?.let { project ->
                            viewModel.onProjectSuggestionClicked(projectId, 0)
                            onProjectSelect(project)
                            onScreenChange(Screen.ProjectDetails)
                        }
                    },
                    analytics = analytics
                )
```

Update CareerTimeline call:
```kotlin
                Screen.CareerTimeline -> CareerProjectsTimelineScreen(
                    projects = careerProjects,
                    onProjectClick = { timelineProject ->
                        onProjectSelect(careerProjectsMap[timelineProject.id])
                        onScreenChange(Screen.ProjectDetails)
                    },
                    onBackClick = { onScreenChange(Screen.Chat) },
                    analytics = analytics
                )
```

Update ProjectDetails call:
```kotlin
                Screen.ProjectDetails -> selectedProject?.let { project ->
                    CareerProjectDetailsScreen(
                        project = project,
                        onBackClick = { onScreenChange(Screen.CareerTimeline) },
                        onLinkClick = onOpenUrl,
                        analytics = analytics
                    )
                }
```

**Step 6: Verify build**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt
git commit -m "feat(analytics): wire analytics to all screens in MainActivity

Connect ChatViewModel callbacks and pass Analytics to all screens.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 11: Final Verification

**Step 1: Run all tests**

Run: `./gradlew :shared-agent:testDebugUnitTest`
Expected: All tests PASS

**Step 2: Run quality checks**

Run: `./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL

**Step 3: Build debug APK**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL with APK at `android-app/build/outputs/apk/debug/android-app-debug.apk`

**Step 4: Commit any remaining fixes if needed**

---

## Summary

| Task | Events Added |
|------|--------------|
| Task 3 | MessageSent, HistoryCleared |
| Task 4 | ResponseCompleted, ErrorDisplayed |
| Task 5 | MessageCopied, MessageLiked, MessageDisliked, RegenerateClicked, SuggestionClicked |
| Task 6 | ScreenView(CHAT) |
| Task 7 | ExternalLinkClicked (6 links), ScreenView(CAREER) navigation |
| Task 9 | ScreenView(CAREER_TIMELINE), ScreenView(PROJECT_DETAILS), ProjectSelected, ProjectLinkClicked |
| **Total** | **20 events** |
