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
import io.github.devmugi.arcane.chat.components.input.ArcaneAgentChatInput
import io.github.devmugi.arcane.chat.components.messages.ArcaneAssistantMessageBlock
import io.github.devmugi.arcane.chat.components.messages.ArcaneChatMessageList
import io.github.devmugi.arcane.chat.components.messages.ArcaneUserMessageBlock
import io.github.devmugi.arcane.chat.components.scaffold.ArcaneChatScreenScaffold
import io.github.devmugi.arcane.chat.models.MessageBlock
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
        containerColor = ArcaneTheme.colors.surfaceContainerLow,
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
                    } else {
                        null
                    },
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

@Suppress("UnusedParameter")
@Composable
private fun MessageItem(
    message: Message,
    state: ChatState,
    @Suppress("UNUSED_PARAMETER") cvData: CVData?,
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
                        )
                    ),
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
                    } else {
                        null
                    }
                )
            }
        }
        MessageRole.SYSTEM -> { /* Skip system messages */ }
    }
}
