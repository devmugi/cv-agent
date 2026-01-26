package io.github.devmugi.cv.agent.analytics

/**
 * Factory function to create platform-specific Analytics implementation.
 *
 * @param context Platform-specific context (Android Context, null for iOS)
 * @return Platform-specific Analytics implementation
 */
expect fun createPlatformAnalytics(context: Any? = null): Analytics
