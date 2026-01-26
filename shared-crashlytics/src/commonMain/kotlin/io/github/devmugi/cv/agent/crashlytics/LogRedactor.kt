package io.github.devmugi.cv.agent.crashlytics

/**
 * Redacts sensitive patterns from log messages.
 * Used to sanitize breadcrumb logs before sending to Crashlytics.
 */
object LogRedactor {
    private val EMAIL_PATTERN = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val TOKEN_PATTERN = Regex("\\b(Bearer\\s+)?[A-Za-z0-9_-]{20,}\\b")

    /**
     * Redacts sensitive patterns from the message.
     * - Email addresses -> [EMAIL]
     * - Long alphanumeric tokens (20+ chars) -> [TOKEN]
     * - Bearer tokens -> [TOKEN]
     */
    fun redact(message: String): String {
        return message
            .replace(EMAIL_PATTERN, "[EMAIL]")
            .replace(TOKEN_PATTERN, "[TOKEN]")
    }
}
