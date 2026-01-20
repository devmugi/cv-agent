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
import org.junit.After
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

    companion object {
        // Timeout for API responses - increased for potentially long responses
        private const val API_TIMEOUT_MS = 60_000L
        private const val UI_TIMEOUT_MS = 10_000L
    }

    @get:Rule
    val retryRule = RetryRule(maxAttempts = 3)

    @Before
    fun setUp() {
        // Stop any existing Koin instance before each test
        ensureKoinStopped()

        Assume.assumeTrue(
            "Skipping E2E test - GROQ_API_KEY not configured in local.properties",
            BuildConfig.GROQ_API_KEY.isNotEmpty()
        )
    }

    @After
    fun tearDown() {
        // Clean up Koin after each test
        ensureKoinStopped()
    }

    @Test
    fun happyPath_tapSuggestionAndReceiveResponse() = runComposeUiTest {
        setContent {
            MainApp()
        }

        // Wait for welcome section to appear
        waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            onAllNodes(hasTestTag("welcome_section")).fetchSemanticsNodes().isNotEmpty()
        }

        // Tap first suggestion chip
        onNodeWithTag("suggestion_chip_0").performClick()

        // Wait for user message to appear first
        waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            onAllNodes(hasTestTagContaining("message_user_"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Wait for assistant response (real API call)
        waitUntil(timeoutMillis = API_TIMEOUT_MS) {
            onAllNodes(hasTestTagContaining("message_assistant_"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun userInput_typeMessageAndReceiveResponse() = runComposeUiTest {
        setContent {
            MainApp()
        }

        // Wait for input to be ready
        waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            onAllNodes(hasTestTag("message_input_field")).fetchSemanticsNodes().isNotEmpty()
        }

        // Type message
        onNodeWithTag("message_input_field").performTextInput("What are Denys's skills?")

        // Tap send button
        onNodeWithTag("send_button").performClick()

        // Wait for user message to appear
        waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            onAllNodes(hasTestTagContaining("message_user_"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Wait for assistant response
        waitUntil(timeoutMillis = API_TIMEOUT_MS) {
            onAllNodes(hasTestTagContaining("message_assistant_"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun conversation_multipleMessagesInHistory() = runComposeUiTest {
        setContent {
            MainApp()
        }

        // Wait for input
        waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            onAllNodes(hasTestTag("message_input_field")).fetchSemanticsNodes().isNotEmpty()
        }

        // Send first message
        onNodeWithTag("message_input_field").performTextInput("Hi")
        onNodeWithTag("send_button").performClick()

        // Wait for first user message
        waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            onAllNodes(hasTestTagContaining("message_user_"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Wait for first assistant response
        waitUntil(timeoutMillis = API_TIMEOUT_MS) {
            onAllNodes(hasTestTagContaining("message_assistant_"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Send second message
        onNodeWithTag("message_input_field").performTextInput("Thanks")
        onNodeWithTag("send_button").performClick()

        // Wait for second user message
        waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            onAllNodes(hasTestTagContaining("message_user_"))
                .fetchSemanticsNodes().size >= 2
        }

        // Wait for second assistant response
        waitUntil(timeoutMillis = API_TIMEOUT_MS) {
            onAllNodes(hasTestTagContaining("message_assistant_"))
                .fetchSemanticsNodes().size >= 2
        }
    }
}
