package io.github.devmugi.cv.agent.crashlytics

/**
 * Callback-based interface that Swift can implement.
 * Swift provides Firebase Crashlytics implementation.
 */
interface CrashReporterProvider {
    fun recordException(domain: String, code: Int, message: String)
    fun log(message: String)
    fun setCustomKey(key: String, value: String)
    fun setUserId(userId: String)
}

/**
 * Kotlin wrapper that bridges [CrashReporterProvider] to [CrashReporter] interface.
 */
class SwiftCrashReporter(
    private val provider: CrashReporterProvider
) : CrashReporter {

    override fun recordException(throwable: Throwable) {
        val domain = throwable::class.simpleName ?: "KotlinException"
        val message = throwable.message ?: "Unknown error"
        provider.recordException(domain, 0, message)
    }

    override fun log(message: String) {
        provider.log(message)
    }

    override fun setCustomKey(key: String, value: String) {
        provider.setCustomKey(key, value)
    }

    override fun setUserId(userId: String) {
        provider.setUserId(userId)
    }
}
