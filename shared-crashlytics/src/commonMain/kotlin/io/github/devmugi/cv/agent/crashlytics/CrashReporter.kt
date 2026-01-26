package io.github.devmugi.cv.agent.crashlytics

/**
 * Crash reporting interface for recording exceptions and diagnostic info.
 * Platform-specific implementations provided via [createPlatformCrashReporter].
 *
 * Android: Firebase Crashlytics
 * iOS: NoOp (future implementation)
 */
interface CrashReporter {
    /**
     * Record a non-fatal exception for crash reporting.
     */
    fun recordException(throwable: Throwable)

    /**
     * Log a message that will appear in the crash report timeline.
     */
    fun log(message: String)

    /**
     * Set a custom key-value pair for crash context.
     */
    fun setCustomKey(key: String, value: String)

    /**
     * Set the user ID for crash correlation.
     * Used to link crashes with OpenTelemetry traces via installation_id.
     */
    fun setUserId(userId: String)

    companion object {
        /**
         * No-op implementation for testing and platforms without crash reporting.
         */
        val NOOP: CrashReporter = NoOpCrashReporter()
    }
}

/**
 * No-op implementation that discards all crash reports.
 * Used for testing and as default when crash reporting is unavailable.
 */
@Suppress("EmptyFunctionBlock")
internal class NoOpCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) = Unit
    override fun log(message: String) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
    override fun setUserId(userId: String) = Unit
}
