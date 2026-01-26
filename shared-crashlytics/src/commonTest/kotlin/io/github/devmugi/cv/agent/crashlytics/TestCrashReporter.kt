package io.github.devmugi.cv.agent.crashlytics

/**
 * Test implementation of [CrashReporter] with observable state.
 */
class TestCrashReporter : CrashReporter {
    val recordedExceptions = mutableListOf<Throwable>()
    val loggedMessages = mutableListOf<String>()
    val customKeys = mutableMapOf<String, String>()
    var userId: String? = null
        private set

    override fun recordException(throwable: Throwable) {
        recordedExceptions.add(throwable)
    }

    override fun log(message: String) {
        loggedMessages.add(message)
    }

    override fun setCustomKey(key: String, value: String) {
        customKeys[key] = value
    }

    override fun setUserId(userId: String) {
        this.userId = userId
    }

    fun reset() {
        recordedExceptions.clear()
        loggedMessages.clear()
        customKeys.clear()
        userId = null
    }
}
