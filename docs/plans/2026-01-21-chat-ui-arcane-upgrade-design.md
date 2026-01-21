# Chat UI Arcane Component Upgrade Design

**Date:** 2026-01-21
**Goal:** Upgrade cv-agent chat UI to use Arcane Design System 0.1.3+ chat components
**Scope:** ArcaneChatScreenScaffold, ArcaneUserMessageBlock, ArcaneAssistantMessageBlock, ArcaneChatMessageList, ArcaneAgentChatInput

---

## Arcane Library Changes (0.1.3 → 0.1.4)

### ArcaneAssistantMessageBlock

**File:** `ArcaneDesignSystem/arcane-components/.../AssistantMessageBlock.kt`

Remove `text: String` and `onCopyClick`. Add required `content` slot and `enableTruncation` parameter.

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

**Behavior:**
- `content` is required, replaces removed `text` parameter
- `enableTruncation = false` skips `maxContentHeight` constraint and gradient fade
- Copy functionality removed - apps use `titleActions` or `bottomActions` slots if needed

### ArcaneUserMessageBlock

**File:** `ArcaneDesignSystem/arcane-components/.../UserMessageBlock.kt`

Add configurable styling parameters with defaults.

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

---

## cv-agent App Changes

### ChatState.kt

Replace `streamingContent: String` with `streamingMessageId: String?`.

```kotlin
data class ChatState(
    val messages: List<Message>,
    val isLoading: Boolean,
    val isStreaming: Boolean,
    val streamingMessageId: String?,  // ID of message being streamed
    val thinkingStatus: String?,
    val error: ChatError?,
    val suggestions: List<String>
)
```

### ChatViewModel.kt

Update streaming flow to manage real Message from start:

1. User sends message → add user Message → set `isLoading = true`
2. First chunk arrives → add assistant Message with ID → set `isStreaming = true`, `streamingMessageId = id`
3. More chunks → update assistant Message content in list
4. Complete → set `isStreaming = false`, `streamingMessageId = null`

### ChatScreen.kt

#### Scaffold Integration

```kotlin
Scaffold(...) {
    ArcaneChatScreenScaffold(
        isEmpty = state.messages.isEmpty() && !state.isLoading && !state.isStreaming,
        emptyState = {
            WelcomeSection(
                suggestions = state.suggestions,
                onSuggestionClick = onSuggestionClick
            )
        },
        content = {
            Column {
                ArcaneChatMessageList(...)
                Disclaimer()
                ArcaneAgentChatInput(...)
            }
        }
    )
}
```

#### Message List

```kotlin
ArcaneChatMessageList(
    messages = state.messages,
    modifier = Modifier.weight(1f),
    reverseLayout = true,
    showScrollToBottom = true,
    messageKey = { it.id },
    messageContent = { message ->
        when (message.role) {
            MessageRole.USER -> ArcaneUserMessageBlock(
                text = message.content,
                maxWidth = screenWidth * 0.8f,
                backgroundColor = ArcaneTheme.colors.surface,
                textStyle = ArcaneTheme.typography.bodySmall
            )
            MessageRole.ASSISTANT -> {
                val isStreaming = message.id == state.streamingMessageId
                val isThinking = isStreaming && message.content.isEmpty()

                ArcaneAssistantMessageBlock(
                    title = if (isThinking) "Thinking..." else "Assistant",
                    isLoading = isStreaming,
                    enableTruncation = true,
                    onShowMoreClick = { showMessagePopup(message) },
                    bottomActions = if (!isStreaming) { { MessageActions(...) } } else null
                ) {
                    if (isThinking) {
                        Text(state.thinkingStatus ?: "...")
                    } else {
                        MarkdownText(text = message.content, ...)
                    }
                }
            }
            MessageRole.SYSTEM -> { }
        }
    }
)
```

#### Chat Input

```kotlin
ArcaneAgentChatInput(
    value = inputText,
    onValueChange = { inputText = it },
    onSend = { ... },
    enabled = !state.isLoading && !state.isStreaming,
    placeholder = "Ask about my experience...",
    onVoiceToTextClick = { /* TODO: future feature */ },
    onAudioRecordClick = { /* TODO: future feature */ },
    activeItemsContent = { if (state.messages.isNotEmpty()) ContextChip(...) }
)
```

### New: Message Popup

Add dialog composable for "Show more" full text view. Opens when truncated message's onShowMoreClick triggers.

### Files to Delete

- `ThinkingIndicator.kt` - Replaced by ArcaneAssistantMessageBlock with isLoading=true

### Files to Simplify

- `MessageBubble.kt` - Remove StreamingMessageBubble, simplify or delete if fully replaced

---

## Summary

| Component | Action |
|-----------|--------|
| Arcane `ArcaneAssistantMessageBlock` | Add `content` slot, `enableTruncation`, remove `text`/`onCopyClick` |
| Arcane `ArcaneUserMessageBlock` | Add `maxWidth`, `backgroundColor`, `textStyle` params |
| `ChatState.kt` | Replace `streamingContent` with `streamingMessageId` |
| `ChatViewModel.kt` | Real Message from streaming start, update content as chunks arrive |
| `ChatScreen.kt` | Use ArcaneChatScreenScaffold, ArcaneChatMessageList, updated components |
| `ThinkingIndicator.kt` | Delete |
| `MessageBubble.kt` | Simplify or delete |
| New popup | Dialog for full message text |
