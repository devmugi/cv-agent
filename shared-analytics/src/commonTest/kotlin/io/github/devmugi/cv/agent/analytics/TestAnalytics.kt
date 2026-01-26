package io.github.devmugi.cv.agent.analytics

/**
 * Test implementation of Analytics that captures all events for assertions.
 * Use in unit tests to verify correct analytics instrumentation.
 */
class TestAnalytics : Analytics {

    private val _events = mutableListOf<AnalyticsEvent>()
    val events: List<AnalyticsEvent> get() = _events.toList()

    private var _userId: String? = null
    val userId: String? get() = _userId

    private val _userProperties = mutableMapOf<String, String?>()
    val userProperties: Map<String, String?> get() = _userProperties.toMap()

    private var _currentScreen: String? = null
    val currentScreen: String? get() = _currentScreen

    override fun logEvent(event: AnalyticsEvent) {
        _events.add(event)
    }

    override fun setUserId(userId: String?) {
        _userId = userId
    }

    override fun setUserProperty(name: String, value: String?) {
        _userProperties[name] = value
    }

    override fun setCurrentScreen(screenName: String, screenClass: String?) {
        _currentScreen = screenName
    }

    /**
     * Clear all captured events and state.
     */
    fun clear() {
        _events.clear()
        _userId = null
        _userProperties.clear()
        _currentScreen = null
    }

    /**
     * Find the first event of the specified type.
     */
    inline fun <reified T : AnalyticsEvent> findEvent(): T? =
        events.filterIsInstance<T>().firstOrNull()

    /**
     * Find all events of the specified type.
     */
    inline fun <reified T : AnalyticsEvent> findEvents(): List<T> =
        events.filterIsInstance<T>()

    /**
     * Assert that an event of the specified type was logged.
     * @throws AssertionError if the event was not found
     */
    inline fun <reified T : AnalyticsEvent> assertEventLogged(): T {
        val event = findEvent<T>()
        check(event != null) {
            "Expected event ${T::class.simpleName} not found. Logged events: ${events.map { it.name }}"
        }
        return event
    }

    /**
     * Assert that no event of the specified type was logged.
     * @throws AssertionError if the event was found
     */
    inline fun <reified T : AnalyticsEvent> assertEventNotLogged() {
        val event = findEvent<T>()
        check(event == null) {
            "Event ${T::class.simpleName} should not have been logged, but found: $event"
        }
    }

    /**
     * Assert the total number of events logged.
     */
    fun assertEventCount(expected: Int) {
        check(events.size == expected) {
            "Expected $expected events, but found ${events.size}: ${events.map { it.name }}"
        }
    }
}
