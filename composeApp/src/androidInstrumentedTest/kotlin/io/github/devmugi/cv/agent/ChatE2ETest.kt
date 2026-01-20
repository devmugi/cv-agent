package io.github.devmugi.cv.agent

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Custom matcher that checks if a test tag contains the given substring.
 */
fun hasTestTagContaining(substring: String): SemanticsMatcher {
    return SemanticsMatcher("TestTag contains '$substring'") { semanticsNode ->
        val testTag = semanticsNode.config.getOrElseNullable(SemanticsProperties.TestTag) { null }
        testTag?.contains(substring) == true
    }
}

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ChatE2ETest {

    @get:Rule
    val retryRule = RetryRule(maxAttempts = 3)

    @Before
    fun checkApiKey() {
        Assume.assumeTrue(
            "Skipping E2E test - GROQ_API_KEY not configured in local.properties",
            BuildConfig.GROQ_API_KEY.isNotEmpty()
        )
    }

    @Test
    fun happyPath_tapSuggestionAndReceiveResponse() = runComposeUiTest {
        setContent {
            MainApp()
        }

        // Wait for welcome section to appear
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag("welcome_section")).fetchSemanticsNodes().isNotEmpty()
        }

        // Tap first suggestion chip
        onNodeWithTag("suggestion_chip_0").performClick()

        // Wait for assistant response (real API call)
        waitUntil(timeoutMillis = 30_000) {
            onAllNodes(hasTestTagContaining("message_assistant_"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify user message exists
        val userMessages = onAllNodes(hasTestTagContaining("message_user_"))
            .fetchSemanticsNodes()
        assert(userMessages.isNotEmpty()) { "Expected at least 1 user message" }
    }

    @Test
    fun userInput_typeMessageAndReceiveResponse() = runComposeUiTest {
        setContent {
            MainApp()
        }

        // Wait for input to be ready
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag("message_input_field")).fetchSemanticsNodes().isNotEmpty()
        }

        // Type message
        onNodeWithTag("message_input_field").performTextInput("What are Denys's skills?")

        // Tap send button
        onNodeWithTag("send_button").performClick()

        // Wait for assistant response
        waitUntil(timeoutMillis = 30_000) {
            onAllNodes(hasTestTagContaining("message_assistant_"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify user message visible
        val userMessages = onAllNodes(hasTestTagContaining("message_user_"))
            .fetchSemanticsNodes()
        assert(userMessages.isNotEmpty()) { "Expected at least 1 user message" }
    }

    @Test
    fun conversation_multipleMessagesInHistory() = runComposeUiTest {
        setContent {
            MainApp()
        }

        // Wait for input
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag("message_input_field")).fetchSemanticsNodes().isNotEmpty()
        }

        // Send first message
        onNodeWithTag("message_input_field").performTextInput("Tell me about Denys")
        onNodeWithTag("send_button").performClick()

        // Wait for first response
        waitUntil(timeoutMillis = 30_000) {
            onAllNodes(hasTestTagContaining("message_assistant_"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Send second message
        onNodeWithTag("message_input_field").performTextInput("What projects has he worked on?")
        onNodeWithTag("send_button").performClick()

        // Wait for second response
        waitUntil(timeoutMillis = 30_000) {
            onAllNodes(hasTestTagContaining("message_assistant_"))
                .fetchSemanticsNodes().size >= 2
        }

        // Verify both user messages exist
        val userMessages = onAllNodes(hasTestTagContaining("message_user_"))
            .fetchSemanticsNodes()
        assert(userMessages.size >= 2) { "Expected at least 2 user messages, found ${userMessages.size}" }

        // Verify both assistant messages exist
        val assistantMessages = onAllNodes(hasTestTagContaining("message_assistant_"))
            .fetchSemanticsNodes()
        assert(assistantMessages.size >= 2) { "Expected at least 2 assistant messages, found ${assistantMessages.size}" }
    }
}
