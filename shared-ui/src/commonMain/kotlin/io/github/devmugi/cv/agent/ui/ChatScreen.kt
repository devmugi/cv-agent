package io.github.devmugi.cv.agent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.devmugi.cv.agent.agent.ChatState
import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.ui.components.CVAgentTopBar
import io.github.devmugi.cv.agent.ui.components.ErrorMessage
import io.github.devmugi.cv.agent.ui.components.MessageBubble
import io.github.devmugi.cv.agent.ui.components.MessageInput
import io.github.devmugi.cv.agent.ui.components.StreamingMessageBubble
import io.github.devmugi.cv.agent.ui.components.WelcomeSection

@Composable
fun ChatScreen(
    state: ChatState,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    cvData: CVData? = null,
    onSuggestionClick: (String) -> Unit = onSendMessage,
    onRetry: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val showWelcome = state.messages.isEmpty() && !state.isLoading && !state.isStreaming

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size, state.streamingContent) {
        if (state.messages.isNotEmpty() || state.isStreaming) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { CVAgentTopBar() },
        bottomBar = {
            MessageInput(
                onSend = onSendMessage,
                isLoading = state.isLoading || state.isStreaming,
                modifier = Modifier.navigationBarsPadding().imePadding()
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showWelcome) {
                WelcomeSection(
                    suggestions = state.suggestions,
                    onSuggestionClick = onSuggestionClick,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("chat_messages_list"),
                    state = listState,
                    reverseLayout = true,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Error message at top (bottom visually due to reverse)
                    state.error?.let { error ->
                        item(key = "error") {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                ErrorMessage(
                                    error = error,
                                    onRetry = onRetry,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Streaming message (appears at bottom, first in reversed list)
                    if (state.isStreaming && state.streamingContent.isNotEmpty()) {
                        item(key = "streaming") {
                            StreamingMessageBubble(content = state.streamingContent)
                        }
                    }

                    // Messages in reverse order (newest first in list = bottom visually)
                    items(
                        items = state.messages.reversed(),
                        key = { it.id }
                    ) { message ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            MessageBubble(
                                message = message,
                                cvData = cvData
                            )
                        }
                    }
                }
            }
        }
    }
}
