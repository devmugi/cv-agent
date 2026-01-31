package io.github.devmugi.cv.agent.analytics

/**
 * Callback-based interface that Swift can implement.
 * Swift provides Firebase Analytics implementation.
 */
interface AnalyticsProvider {
    fun logEvent(name: String, parameters: Map<String, Any>?)
    fun setUserId(userId: String?)
    fun setUserProperty(name: String, value: String?)
    fun setCurrentScreen(screenName: String, screenClass: String?)
}

/**
 * Kotlin wrapper that bridges [AnalyticsProvider] to [Analytics] interface.
 */
class SwiftAnalytics(
    private val provider: AnalyticsProvider
) : Analytics {

    override fun logEvent(event: AnalyticsEvent) {
        // Filter out null values and convert to non-nullable map for Firebase
        val filteredParams = event.params.filterValues { it != null }.mapValues { it.value!! }
        provider.logEvent(event.name, filteredParams.ifEmpty { null })
    }

    override fun setUserId(userId: String?) {
        provider.setUserId(userId)
    }

    override fun setUserProperty(name: String, value: String?) {
        provider.setUserProperty(name, value)
    }

    override fun setCurrentScreen(screenName: String, screenClass: String?) {
        provider.setCurrentScreen(screenName, screenClass)
    }
}
