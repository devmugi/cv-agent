package io.github.devmugi.cv.agent.analytics

/**
 * Analytics interface for tracking user events across the application.
 * Platform-specific implementations are provided via [createPlatformAnalytics].
 *
 * Android: Firebase Analytics
 * iOS: NOOP (future Firebase iOS implementation)
 */
interface Analytics {
    /**
     * Log an analytics event with associated parameters.
     */
    fun logEvent(event: AnalyticsEvent)

    /**
     * Set a user ID for analytics tracking.
     * Pass null to clear the user ID.
     */
    fun setUserId(userId: String?)

    /**
     * Set a user property for segmentation.
     * Pass null value to clear the property.
     */
    fun setUserProperty(name: String, value: String?)

    /**
     * Log a screen view event.
     */
    fun setCurrentScreen(screenName: String, screenClass: String? = null)

    companion object {
        /**
         * No-op implementation for testing and platforms without analytics.
         */
        val NOOP: Analytics = NoOpAnalytics()
    }
}

/**
 * No-op implementation that discards all events.
 * Used for testing and as default when analytics is disabled.
 */
internal class NoOpAnalytics : Analytics {
    override fun logEvent(event: AnalyticsEvent) {}
    override fun setUserId(userId: String?) {}
    override fun setUserProperty(name: String, value: String?) {}
    override fun setCurrentScreen(screenName: String, screenClass: String?) {}
}
