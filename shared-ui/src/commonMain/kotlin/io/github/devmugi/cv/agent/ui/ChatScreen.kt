package io.github.devmugi.cv.agent.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import io.github.devmugi.arcane.chat.components.messages.ArcaneAssistantMessageBlock
import io.github.devmugi.arcane.chat.components.messages.ArcaneChatMessageList
import io.github.devmugi.arcane.chat.components.messages.ArcaneUserMessageBlock
import io.github.devmugi.arcane.chat.components.scaffold.ArcaneChatScreenScaffold
import io.github.devmugi.arcane.chat.models.MessageBlock
import io.github.devmugi.arcane.design.components.feedback.ArcaneSkeleton
import io.github.devmugi.arcane.design.components.feedback.ArcaneSkeletonShape
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastState
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastStyle
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.domain.models.ChatError
import io.github.devmugi.cv.agent.domain.models.ChatState
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.models.MessageRole
import io.github.devmugi.cv.agent.ui.components.AnimatedChatInput
import io.github.devmugi.cv.agent.ui.components.CVAgentTopBar
import io.github.devmugi.cv.agent.ui.components.ContextChip
import io.github.devmugi.cv.agent.ui.components.Disclaimer
import io.github.devmugi.cv.agent.ui.components.FloatingInputContainer
import io.github.devmugi.cv.agent.ui.components.FeedbackState
import io.github.devmugi.cv.agent.ui.components.MessageActions
import io.github.devmugi.cv.agent.ui.components.SuggestionChip
import io.github.devmugi.cv.agent.ui.components.WelcomeSection

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    state: ChatState,
    toastState: ArcaneToastState,
    onSendMessage: (String) -> Unit,
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
    var inputText by remember { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible

    val showWelcome = state.messages.isEmpty() && !state.isLoading && !state.isStreaming

    // Clear focus when keyboard hides
    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible) {
            focusManager.clearFocus()
        }
    }

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

    Scaffold(
        modifier = modifier,
        containerColor = ArcaneTheme.colors.surfaceContainerLow,
        topBar = {
            CVAgentTopBar(onCareerClick = onNavigateToCareerTimeline)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            ArcaneChatScreenScaffold(
                isEmpty = showWelcome,
                modifier = Modifier.fillMaxSize().imePadding(),
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
                        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp),
                        showScrollToBottom = true,
                        messageKey = { it.id },
                        messageContent = { message ->
                            MessageItem(
                                message = message,
                                state = state,
                                onNavigateToProject = onNavigateToProject,
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

            FloatingInputContainer(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Disclaimer()
                AnimatedChatInput(
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
                    } else {
                        null
                    },
                    onInputFocusChanged = { focused -> isInputFocused = focused },
                    modifier = Modifier.testTag("chat_input")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageItem(
    message: Message,
    state: ChatState,
    onNavigateToProject: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onShareMessage: (String) -> Unit,
    onLikeMessage: (String) -> Unit,
    onDislikeMessage: (String) -> Unit,
    onRegenerateMessage: (String) -> Unit
) {
    when (message.role) {
        MessageRole.USER -> {
            ArcaneUserMessageBlock(
                blocks = listOf(
                    MessageBlock.Text(
                        id = "${message.id}-text",
                        content = message.content
                    )
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                backgroundColor = ArcaneTheme.colors.surfaceContainerLow
            )
        }
        MessageRole.ASSISTANT -> {
            val isStreaming = message.id == state.streamingMessageId
            val isThinking = isStreaming && message.content.isEmpty()

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ArcaneAssistantMessageBlock(
                    blocks = listOf(
                        MessageBlock.Custom(
                            id = "${message.id}-content",
                            content = {
                                if (isThinking) {
                                    ArcaneSkeleton(
                                        shape = ArcaneSkeletonShape.Text(lines = 3)
                                    )
                                } else {
                                    Markdown(
                                        content = message.content,
                                        colors = markdownColor(text = ArcaneTheme.colors.text)
                                    )
                                }
                            }
                        )
                    ),
                    title = if (isThinking) "Thinking..." else "Assistant",
                    isLoading = isStreaming,
                    enableTruncation = false,
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
                    } else {
                        null
                    }
                )
                if (!isStreaming && message.suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        message.suggestions.forEach { projectId ->
                            val projectName = state.projectNames[projectId] ?: projectId
                            SuggestionChip(
                                text = projectName,
                                onClick = { onNavigateToProject(projectId) }
                            )
                        }
                    }
                }
            }
        }
        MessageRole.SYSTEM -> { /* Skip system messages */ }
    }
}
