# Chat UI Arcane Upgrade Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Upgrade cv-agent chat UI to use Arcane Design System 0.1.4 chat components with unified message rendering.

**Architecture:** Update Arcane library with content slot and configurable styling, then migrate cv-agent to use ArcaneChatScreenScaffold, ArcaneChatMessageList, and unified ArcaneAssistantMessageBlock for all assistant states (thinking, streaming, complete).

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Arcane Design System 0.1.4

---

## Phase 1: Arcane Library Changes

### Task 1: Update ArcaneAssistantMessageBlock

**Files:**
- Modify: `/Users/den/IdeaProjects/ArcaneDesignSystem/arcane-components/src/commonMain/kotlin/io/github/devmugi/arcane/design/components/controls/AssistantMessageBlock.kt`

**Step 1: Update function signature**

Remove `text: String` and `onCopyClick`. Add `enableTruncation` and `content` slot:

```kotlin
@Composable
fun ArcaneAssistantMessageBlock(
    modifier: Modifier = Modifier,
    title: String? = null,
    isLoading: Boolean = false,
    maxContentHeight: Dp = 160.dp,
    enableTruncation: Boolean = true,
    showBottomActions: Boolean = false,
    autoShowWhenTruncated: Boolean = true,
    onShowMoreClick: (() -> Unit)? = null,
    titleActions: @Composable (RowScope.() -> Unit)? = null,
    bottomActions: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit
)
```

**Step 2: Update title row - remove copy button**

Remove the entire `if (onCopyClick != null)` block (lines 120-138).

**Step 3: Update content box - use content slot and enableTruncation**

Replace the `Text(text = text, ...)` with `content()` and add truncation condition:

```kotlin
// Message Content
val maxHeightPx = with(density) { maxContentHeight.toPx() }
Box(
    modifier = Modifier
        .fillMaxWidth()
        .then(
            if (enableTruncation && !isExpanded) {
                Modifier.heightIn(max = maxContentHeight)
            } else {
                Modifier
            }
        )
        .onSizeChanged { size ->
            if (enableTruncation && !isExpanded) {
                isTruncated = size.height >= maxHeightPx.toInt()
            }
        }
        .drawWithContent {
            drawContent()
            if (enableTruncation && isTruncated && !isExpanded) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.surfaceRaised),
                        startY = size.height * 0.6f,
                        endY = size.height
                    )
                )
            }
        }
) {
    content()
}
```

**Step 4: Update bottom actions condition**

```kotlin
val shouldShowBottomActions = showBottomActions ||
    (enableTruncation && autoShowWhenTruncated && isTruncated && !isExpanded)
```

**Step 5: Delete CopyIcon composable**

Remove the entire `CopyIcon` private function (lines 216-255) - no longer needed.

**Step 6: Build and verify**

Run: `cd /Users/den/IdeaProjects/ArcaneDesignSystem && ./gradlew :arcane-components:build`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
cd /Users/den/IdeaProjects/ArcaneDesignSystem
git add arcane-components/src/commonMain/kotlin/io/github/devmugi/arcane/design/components/controls/AssistantMessageBlock.kt
git commit -m "feat(components): add content slot and enableTruncation to ArcaneAssistantMessageBlock

BREAKING: Remove text parameter, add required content slot
BREAKING: Remove onCopyClick parameter and built-in copy button
- Add enableTruncation parameter (default true)
- Apps now use content slot for custom rendering (e.g., markdown)"
```

---

### Task 2: Update ArcaneUserMessageBlock

**Files:**
- Modify: `/Users/den/IdeaProjects/ArcaneDesignSystem/arcane-components/src/commonMain/kotlin/io/github/devmugi/arcane/design/components/controls/UserMessageBlock.kt`

**Step 1: Add imports**

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
```

**Step 2: Update function signature**

```kotlin
@Composable
fun ArcaneUserMessageBlock(
    text: String,
    modifier: Modifier = Modifier,
    timestamp: String? = null,
    maxWidth: Dp = 280.dp,
    backgroundColor: Color = ArcaneTheme.colors.primary.copy(alpha = 0.15f),
    textStyle: TextStyle = ArcaneTheme.typography.bodyMedium
)
```

**Step 3: Update Column modifier to use parameters**

```kotlin
Column(
    modifier = Modifier
        .widthIn(max = maxWidth)
        .clip(UserMessageShape)
        .background(backgroundColor)
        .padding(
            horizontal = ArcaneSpacing.Small,
            vertical = ArcaneSpacing.XSmall
        ),
    horizontalAlignment = Alignment.End
) {
    Text(
        text = text,
        style = textStyle,
        color = colors.text
    )
    // ... timestamp unchanged
}
```

**Step 4: Build and verify**

Run: `cd /Users/den/IdeaProjects/ArcaneDesignSystem && ./gradlew :arcane-components:build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
cd /Users/den/IdeaProjects/ArcaneDesignSystem
git add arcane-components/src/commonMain/kotlin/io/github/devmugi/arcane/design/components/controls/UserMessageBlock.kt
git commit -m "feat(components): add configurable styling to ArcaneUserMessageBlock

- Add maxWidth parameter (default 280.dp)
- Add backgroundColor parameter (default primary @ 15% alpha)
- Add textStyle parameter (default bodyMedium)"
```

---

### Task 3: Bump Arcane version and publish

**Files:**
- Modify: `/Users/den/IdeaProjects/ArcaneDesignSystem/gradle.properties`

**Step 1: Update version**

Change `VERSION_NAME=0.1.3` to `VERSION_NAME=0.1.4`

**Step 2: Publish to maven local**

Run: `cd /Users/den/IdeaProjects/ArcaneDesignSystem && ./gradlew publishToMavenLocal`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
cd /Users/den/IdeaProjects/ArcaneDesignSystem
git add gradle.properties
git commit -m "chore: bump version to 0.1.4"
```

---

## Phase 2: cv-agent Domain Changes

### Task 4: Update ChatState

**Files:**
- Modify: `/Users/den/IdeaProjects/cv-agent/shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/ChatState.kt`

**Step 1: Replace streamingContent with streamingMessageId**

```kotlin
package io.github.devmugi.cv.agent.domain.models

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingMessageId: String? = null,
    val thinkingStatus: String? = null,
    val error: ChatError? = null,
    val suggestions: List<String> = defaultSuggestions
)

sealed class ChatError {
    data class Network(val message: String) : ChatError()
    data class Api(val message: String) : ChatError()
    data object RateLimit : ChatError()
}

val defaultSuggestions = listOf(
    "What's Denys's experience?",
    "Tell me about his skills",
    "What projects has he worked on?",
    "What are his achievements?"
)
```

**Step 2: Build to check for compile errors**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew :shared-domain:build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
cd /Users/den/IdeaProjects/cv-agent
git add shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/ChatState.kt
git commit -m "refactor(domain): replace streamingContent with streamingMessageId

Message content now updated in-place during streaming to prevent
layout jumps when streaming completes."
```

---

### Task 5: Update ChatViewModel

**Files:**
- Modify: `/Users/den/IdeaProjects/cv-agent/shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt`

**Step 1: Update streamResponse to create real Message at streaming start**

Replace the entire `streamResponse()` function:

```kotlin
private suspend fun streamResponse() {
    val cvData = cvDataProvider()
    val systemPrompt = cvData?.let { promptBuilder.build(it) } ?: ""

    val apiMessages = buildApiMessages(systemPrompt)
    val assistantMessageId = java.util.UUID.randomUUID().toString()

    // Create assistant message at start with empty content
    val assistantMessage = Message(
        id = assistantMessageId,
        role = MessageRole.ASSISTANT,
        content = "",
        references = emptyList()
    )

    _state.update { current ->
        current.copy(
            messages = current.messages + assistantMessage,
            isLoading = false,
            isStreaming = true,
            streamingMessageId = assistantMessageId,
            thinkingStatus = null
        )
    }

    var streamedContent = ""

    apiClient.streamChatCompletion(
        messages = apiMessages,
        onChunk = { chunk ->
            streamedContent += chunk
            // Update the assistant message content in-place
            _state.update { current ->
                current.copy(
                    messages = current.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(content = streamedContent)
                        } else {
                            msg
                        }
                    }
                )
            }
        },
        onComplete = {
            val extractionResult = referenceExtractor.extract(streamedContent)
            // Finalize the message with cleaned content and references
            _state.update { current ->
                current.copy(
                    messages = current.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(
                                content = extractionResult.cleanedContent,
                                references = extractionResult.references
                            )
                        } else {
                            msg
                        }
                    },
                    isStreaming = false,
                    streamingMessageId = null
                )
            }
        },
        onError = { exception ->
            val error = when (exception) {
                is GroqApiException.NetworkError -> ChatError.Network(exception.reason)
                is GroqApiException.RateLimitError -> ChatError.RateLimit
                is GroqApiException.AuthError -> ChatError.Api("Authentication failed")
                is GroqApiException.ApiError -> ChatError.Api(exception.message)
            }
            // Remove the incomplete assistant message on error
            _state.update { current ->
                current.copy(
                    messages = current.messages.filter { it.id != assistantMessageId },
                    isLoading = false,
                    isStreaming = false,
                    streamingMessageId = null,
                    error = error
                )
            }
        }
    )
}
```

**Step 2: Add UUID import if needed**

For KMP, use `kotlin.uuid.Uuid` (Kotlin 2.0+) or create a simple ID generator:

```kotlin
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// In streamResponse:
@OptIn(ExperimentalUuidApi::class)
val assistantMessageId = Uuid.random().toString()
```

**Step 3: Build and verify**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew :shared:build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
cd /Users/den/IdeaProjects/cv-agent
git add shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt
git commit -m "refactor(viewmodel): update streaming to use real Message from start

- Create assistant Message when streaming begins
- Update message content in-place as chunks arrive
- Prevents layout jumps when streaming completes
- Remove incomplete message on error"
```

---

## Phase 3: cv-agent UI Changes

### Task 6: Update cv-agent Arcane dependency version

**Files:**
- Modify: `/Users/den/IdeaProjects/cv-agent/shared-ui/build.gradle.kts`
- Modify: `/Users/den/IdeaProjects/cv-agent/android-app/build.gradle.kts`

**Step 1: Update shared-ui/build.gradle.kts**

Change version from `0.1.3` to `0.1.4`:

```kotlin
// Arcane Design System
implementation("io.github.devmugi.design.arcane:arcane-foundation:0.1.4")
implementation("io.github.devmugi.design.arcane:arcane-components:0.1.4")
```

**Step 2: Update android-app/build.gradle.kts**

Change version from `0.1.3` to `0.1.4`:

```kotlin
// Arcane Design System (needed for ArcaneTheme and ArcaneToastHost in MainActivity)
implementation("io.github.devmugi.design.arcane:arcane-foundation:0.1.4")
implementation("io.github.devmugi.design.arcane:arcane-components:0.1.4")
```

**Step 3: Sync and verify**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew :shared-ui:dependencies`
Expected: Shows arcane 0.1.4

**Step 4: Commit**

```bash
cd /Users/den/IdeaProjects/cv-agent
git add shared-ui/build.gradle.kts android-app/build.gradle.kts
git commit -m "build: update Arcane Design System to 0.1.4"
```

---

### Task 7: Add MessagePopupDialog component

**Files:**
- Create: `/Users/den/IdeaProjects/cv-agent/shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessagePopupDialog.kt`

**Step 1: Create the popup dialog**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

@Composable
fun MessagePopupDialog(
    content: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = ArcaneTheme.colors.primary
                )
            }
        },
        title = {
            Text(
                text = "Full Message",
                style = ArcaneTheme.typography.headlineLarge,
                color = ArcaneTheme.colors.text
            )
        },
        text = {
            Markdown(
                content = content,
                colors = markdownColor(text = ArcaneTheme.colors.text),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            )
        },
        containerColor = ArcaneTheme.colors.surface,
        modifier = modifier
    )
}
```

**Step 2: Build and verify**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew :shared-ui:build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
cd /Users/den/IdeaProjects/cv-agent
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessagePopupDialog.kt
git commit -m "feat(ui): add MessagePopupDialog for full message view"
```

---

### Task 8: Rewrite ChatScreen with Arcane components

**Files:**
- Modify: `/Users/den/IdeaProjects/cv-agent/shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`

**Step 1: Update imports**

```kotlin
package io.github.devmugi.cv.agent.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import io.github.devmugi.arcane.design.components.controls.ArcaneAgentChatInput
import io.github.devmugi.arcane.design.components.controls.ArcaneAssistantMessageBlock
import io.github.devmugi.arcane.design.components.controls.ArcaneChatMessageList
import io.github.devmugi.arcane.design.components.controls.ArcaneChatScreenScaffold
import io.github.devmugi.arcane.design.components.controls.ArcaneUserMessageBlock
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastState
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastStyle
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.domain.models.ChatError
import io.github.devmugi.cv.agent.domain.models.ChatState
import io.github.devmugi.cv.agent.domain.models.CVData
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.models.MessageRole
import io.github.devmugi.cv.agent.ui.components.CVAgentTopBar
import io.github.devmugi.cv.agent.ui.components.ContextChip
import io.github.devmugi.cv.agent.ui.components.Disclaimer
import io.github.devmugi.cv.agent.ui.components.FeedbackState
import io.github.devmugi.cv.agent.ui.components.MessageActions
import io.github.devmugi.cv.agent.ui.components.MessagePopupDialog
import io.github.devmugi.cv.agent.ui.components.WelcomeSection
```

**Step 2: Rewrite ChatScreen composable**

```kotlin
@Composable
fun ChatScreen(
    state: ChatState,
    toastState: ArcaneToastState,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    cvData: CVData? = null,
    onSuggestionClick: (String) -> Unit = onSendMessage,
    onCopyMessage: (String) -> Unit = {},
    onShareMessage: (String) -> Unit = {},
    onLikeMessage: (String) -> Unit = {},
    onDislikeMessage: (String) -> Unit = {},
    onRegenerateMessage: (String) -> Unit = {},
    onClearHistory: (() -> Unit)? = null
) {
    var inputText by remember { mutableStateOf("") }
    var popupMessage by remember { mutableStateOf<Message?>(null) }

    val showWelcome = state.messages.isEmpty() && !state.isLoading && !state.isStreaming

    // Show errors as toasts
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            toastState.show(
                message = when (error) {
                    is ChatError.Network -> error.message
                    is ChatError.Api -> error.message
                    ChatError.RateLimit -> "Too many requests. Please wait a moment."
                },
                style = ArcaneToastStyle.Error
            )
        }
    }

    // Message popup dialog
    popupMessage?.let { message ->
        MessagePopupDialog(
            content = message.content,
            onDismiss = { popupMessage = null }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = ArcaneTheme.colors.surface,
        topBar = { CVAgentTopBar() },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding().imePadding()) {
                Disclaimer()
                ArcaneAgentChatInput(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    placeholder = "Ask about my experience...",
                    enabled = !(state.isLoading || state.isStreaming),
                    onVoiceToTextClick = {
                        toastState.show("Voice to text not implemented yet")
                    },
                    onAudioRecordClick = {
                        toastState.show("Voice conversation not implemented yet")
                    },
                    activeItemsContent = if (state.messages.isNotEmpty() && onClearHistory != null) {
                        { ContextChip(onDismiss = onClearHistory) }
                    } else null,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("chat_input")
                )
            }
        }
    ) { padding ->
        ArcaneChatScreenScaffold(
            isEmpty = showWelcome,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            emptyState = {
                WelcomeSection(
                    suggestions = state.suggestions,
                    onSuggestionClick = onSuggestionClick
                )
            },
            content = {
                ArcaneChatMessageList(
                    messages = state.messages.reversed(),
                    modifier = Modifier.fillMaxSize().testTag("chat_messages_list"),
                    reverseLayout = true,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    showScrollToBottom = true,
                    messageKey = { it.id },
                    messageContent = { message ->
                        MessageItem(
                            message = message,
                            state = state,
                            cvData = cvData,
                            onShowMore = { popupMessage = message },
                            onCopyMessage = onCopyMessage,
                            onShareMessage = onShareMessage,
                            onLikeMessage = onLikeMessage,
                            onDislikeMessage = onDislikeMessage,
                            onRegenerateMessage = onRegenerateMessage
                        )
                    }
                )
            }
        )
    }
}

@Composable
private fun MessageItem(
    message: Message,
    state: ChatState,
    cvData: CVData?,
    onShowMore: () -> Unit,
    onCopyMessage: (String) -> Unit,
    onShareMessage: (String) -> Unit,
    onLikeMessage: (String) -> Unit,
    onDislikeMessage: (String) -> Unit,
    onRegenerateMessage: (String) -> Unit
) {
    when (message.role) {
        MessageRole.USER -> {
            ArcaneUserMessageBlock(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                backgroundColor = ArcaneTheme.colors.surface,
                textStyle = ArcaneTheme.typography.bodySmall
            )
        }
        MessageRole.ASSISTANT -> {
            val isStreaming = message.id == state.streamingMessageId
            val isThinking = isStreaming && message.content.isEmpty()

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ArcaneAssistantMessageBlock(
                    title = if (isThinking) "Thinking..." else "Assistant",
                    isLoading = isStreaming,
                    enableTruncation = true,
                    onShowMoreClick = onShowMore,
                    bottomActions = if (!isStreaming) {
                        {
                            MessageActions(
                                onCopy = { onCopyMessage(message.id) },
                                onShare = { onShareMessage(message.id) },
                                onLike = { onLikeMessage(message.id) },
                                onDislike = { onDislikeMessage(message.id) },
                                onRegenerate = { onRegenerateMessage(message.id) },
                                feedbackState = FeedbackState.NONE
                            )
                        }
                    } else null
                ) {
                    if (isThinking) {
                        Text(
                            text = state.thinkingStatus ?: "...",
                            style = ArcaneTheme.typography.bodyMedium,
                            color = ArcaneTheme.colors.textSecondary
                        )
                    } else {
                        Markdown(
                            content = message.content,
                            colors = markdownColor(text = ArcaneTheme.colors.text)
                        )
                    }
                }
            }
        }
        MessageRole.SYSTEM -> { /* Skip system messages */ }
    }
}
```

**Step 3: Build and verify**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew :shared-ui:build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
cd /Users/den/IdeaProjects/cv-agent
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt
git commit -m "refactor(ui): migrate ChatScreen to Arcane chat components

- Use ArcaneChatScreenScaffold for empty/content states
- Use ArcaneChatMessageList with auto-scroll
- Use ArcaneUserMessageBlock for user messages
- Use ArcaneAssistantMessageBlock with content slot for assistant
- Unified rendering for thinking, streaming, and complete states
- Add popup dialog for 'Show more' full message view"
```

---

### Task 9: Delete ThinkingIndicator.kt

**Files:**
- Delete: `/Users/den/IdeaProjects/cv-agent/shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ThinkingIndicator.kt`

**Step 1: Delete file**

Run: `rm /Users/den/IdeaProjects/cv-agent/shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ThinkingIndicator.kt`

**Step 2: Build and verify**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew :shared-ui:build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
cd /Users/den/IdeaProjects/cv-agent
git add -A shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ThinkingIndicator.kt
git commit -m "refactor(ui): delete ThinkingIndicator - replaced by ArcaneAssistantMessageBlock"
```

---

### Task 10: Simplify MessageBubble.kt

**Files:**
- Modify: `/Users/den/IdeaProjects/cv-agent/shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageBubble.kt`

**Step 1: Remove unused code, keep only helper functions and ReferenceChip rendering**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.domain.models.CVData
import io.github.devmugi.cv.agent.domain.models.CVReference
import io.github.devmugi.cv.agent.domain.models.ReferenceType

/**
 * Helper function to get tooltip content for a CV reference.
 */
fun getTooltipForReference(reference: CVReference, cvData: CVData?): String {
    if (cvData == null) return reference.displayLabel

    return when (reference.type) {
        ReferenceType.WORK_EXPERIENCE -> {
            cvData.workExperience.find { it.company == reference.id }?.let { exp ->
                "${exp.title} at ${exp.company}\n${exp.period}"
            } ?: reference.displayLabel
        }
        ReferenceType.PROJECT -> {
            cvData.projects.find { it.name == reference.id }?.let { proj ->
                "${proj.name}\n${proj.description}"
            } ?: reference.displayLabel
        }
        ReferenceType.SKILL -> reference.displayLabel
        ReferenceType.ACHIEVEMENT -> {
            cvData.achievements.find { it.title == reference.id }?.let { ach ->
                "${ach.title}\n${ach.description}"
            } ?: reference.displayLabel
        }
        ReferenceType.EDUCATION -> {
            cvData.education.find { it.institution == reference.id }?.let { edu ->
                "${edu.degree} - ${edu.institution}\n${edu.period}"
            } ?: reference.displayLabel
        }
    }
}
```

**Step 2: Build and verify**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew :shared-ui:build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
cd /Users/den/IdeaProjects/cv-agent
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageBubble.kt
git commit -m "refactor(ui): simplify MessageBubble to helper functions only

Remove MessageBubble and StreamingMessageBubble composables -
now handled by ArcaneUserMessageBlock and ArcaneAssistantMessageBlock"
```

---

## Phase 4: Testing and Verification

### Task 11: Update ChatViewModel tests

**Files:**
- Modify: `/Users/den/IdeaProjects/cv-agent/shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Update tests to use streamingMessageId instead of streamingContent**

Find and replace assertions:
- `state.streamingContent` → check message content in `state.messages`
- `streamingMessageId != null` for streaming state checks

**Step 2: Run tests**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew :shared:test`
Expected: All tests pass

**Step 3: Commit**

```bash
cd /Users/den/IdeaProjects/cv-agent
git add shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt
git commit -m "test: update ChatViewModel tests for new streaming approach"
```

---

### Task 12: Run full build and tests

**Step 1: Run quality checks**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL

**Step 2: Run all tests**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew allTests`
Expected: All tests pass

**Step 3: Build Android app**

Run: `cd /Users/den/IdeaProjects/cv-agent && ./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit any fixes**

If any fixes were needed:
```bash
cd /Users/den/IdeaProjects/cv-agent
git add -A
git commit -m "fix: address build and test issues from Arcane upgrade"
```

---

## Summary

| Task | Description | Files |
|------|-------------|-------|
| 1 | Update ArcaneAssistantMessageBlock | Arcane library |
| 2 | Update ArcaneUserMessageBlock | Arcane library |
| 3 | Bump Arcane version to 0.1.4 | Arcane gradle.properties |
| 4 | Update ChatState | shared-domain |
| 5 | Update ChatViewModel | shared |
| 6 | Update cv-agent Arcane dependency | build.gradle.kts files |
| 7 | Add MessagePopupDialog | shared-ui |
| 8 | Rewrite ChatScreen | shared-ui |
| 9 | Delete ThinkingIndicator | shared-ui |
| 10 | Simplify MessageBubble | shared-ui |
| 11 | Update ViewModel tests | shared tests |
| 12 | Run full build and tests | all |
