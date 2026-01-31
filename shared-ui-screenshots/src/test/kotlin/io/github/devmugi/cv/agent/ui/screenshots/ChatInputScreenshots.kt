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
    fun input_transcribing() = snapshotBothThemes("transcribing") {
        AnimatedChatInput(
            value = "Transcribing...",
            onValueChange = {},
            onSend = {},
            enabled = true,
            isTranscribing = true
        )
    }
}
