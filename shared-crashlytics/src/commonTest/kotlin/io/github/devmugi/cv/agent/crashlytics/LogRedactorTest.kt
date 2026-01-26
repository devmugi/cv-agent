package io.github.devmugi.cv.agent.crashlytics

import kotlin.test.Test
import kotlin.test.assertEquals

class LogRedactorTest {

    @Test
    fun redactsEmailAddresses() {
        val input = "User john@example.com logged in"
        val result = LogRedactor.redact(input)
        assertEquals("User [EMAIL] logged in", result)
    }

    @Test
    fun redactsMultipleEmails() {
        val input = "From alice@test.org to bob@example.com"
        val result = LogRedactor.redact(input)
        assertEquals("From [EMAIL] to [EMAIL]", result)
    }

    @Test
    fun redactsBearerTokens() {
        val input = "Auth: Bearer abc123def456ghi789jkl012mno"
        val result = LogRedactor.redact(input)
        assertEquals("Auth: [TOKEN]", result)
    }

    @Test
    fun redactsLongAlphanumericTokens() {
        val input = "API key: sk_live_abc123def456ghi789"
        val result = LogRedactor.redact(input)
        assertEquals("API key: [TOKEN]", result)
    }

    @Test
    fun redactsTokensWithUnderscoresAndDashes() {
        val input = "Token: gsk_abc123-def456_ghi789-jkl"
        val result = LogRedactor.redact(input)
        assertEquals("Token: [TOKEN]", result)
    }

    @Test
    fun preservesNormalMessages() {
        val input = "Starting chat session"
        val result = LogRedactor.redact(input)
        assertEquals("Starting chat session", result)
    }

    @Test
    fun preservesShortStrings() {
        val input = "User ID: abc123"
        val result = LogRedactor.redact(input)
        assertEquals("User ID: abc123", result)
    }

    @Test
    fun handlesEmptyString() {
        val result = LogRedactor.redact("")
        assertEquals("", result)
    }

    @Test
    fun redactsBothEmailAndToken() {
        val input = "User test@email.com with token abc123def456ghi789jkl012"
        val result = LogRedactor.redact(input)
        assertEquals("User [EMAIL] with token [TOKEN]", result)
    }
}
