package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.domain.models.ChatState
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.models.MessageRole

/**
 * Test data factory for ChatScreen screenshot tests.
 */
object ChatScreenTestData {

    fun emptyState() = ChatState()

    fun singleUserMessage() = ChatState(
        messages = listOf(
            Message(
                role = MessageRole.USER,
                content = "What's your Android experience?"
            )
        )
    )

    fun conversation() = ChatState(
        messages = listOf(
            Message(
                role = MessageRole.USER,
                content = "What's your Android experience?"
            ),
            Message(
                role = MessageRole.ASSISTANT,
                content = "I have 8+ years of Android development experience, " +
                    "including Kotlin, Jetpack Compose, and modern architecture patterns " +
                    "like MVI and Clean Architecture."
            ),
            Message(
                role = MessageRole.USER,
                content = "Tell me about a challenging project"
            ),
            Message(
                role = MessageRole.ASSISTANT,
                content = "One challenging project was building a real-time collaboration " +
                    "feature that required WebSocket integration and complex state synchronization."
            )
        )
    )

    fun streaming() = ChatState(
        messages = listOf(
            Message(
                id = "streaming-msg",
                role = MessageRole.USER,
                content = "Explain your testing approach"
            ),
            Message(
                id = "assistant-streaming",
                role = MessageRole.ASSISTANT,
                content = "I believe in comprehensive testing..."
            )
        ),
        isStreaming = true,
        streamingMessageId = "assistant-streaming"
    )

    fun withProjectSuggestions() = ChatState(
        messages = listOf(
            Message(
                role = MessageRole.USER,
                content = "Tell me about your McDonald's experience"
            ),
            Message(
                role = MessageRole.ASSISTANT,
                content = "I worked on the McDonald's mobile app, implementing new features " +
                    "and improving performance. It was a large-scale project with millions of users.",
                suggestions = listOf("mcdonalds-app", "android-performance")
            )
        ),
        projectNames = mapOf(
            "mcdonalds-app" to "McDonald's App",
            "android-performance" to "Performance Optimization"
        )
    )

    fun longConversation() = ChatState(
        messages = listOf(
            Message(
                role = MessageRole.USER,
                content = "Hi"
            ),
            Message(
                role = MessageRole.ASSISTANT,
                content = "Hello! How can I help you today?"
            ),
            Message(
                role = MessageRole.USER,
                content = "Tell me about your Android experience"
            ),
            Message(
                role = MessageRole.ASSISTANT,
                content = "I specialize in Android development with over 8 years of experience..."
            ),
            Message(
                role = MessageRole.USER,
                content = "What about Compose?"
            ),
            Message(
                role = MessageRole.ASSISTANT,
                content = "Jetpack Compose is my preferred UI toolkit. I've been using it since alpha..."
            ),
            Message(
                role = MessageRole.USER,
                content = "How do you approach testing?"
            ),
            Message(
                role = MessageRole.ASSISTANT,
                content = "I follow TDD practices with comprehensive test coverage including " +
                    "unit, integration, and UI tests."
            )
        )
    )
}
