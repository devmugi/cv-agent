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
    fun screen_withProjectSuggestions() = snapshotBothThemes("with_project_suggestions") {
        ChatScreen(
            state = ChatScreenTestData.withProjectSuggestions(),
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
