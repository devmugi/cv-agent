package io.github.devmugi.cv.agent.crashlytics

import io.github.devmugi.cv.agent.identity.InstallationIdentity

/**
 * Initializes CrashReporter with installation ID for trace correlation.
 * Call [initialize] once at app startup after DI is ready.
 */
class CrashReporterInitializer(
    private val crashReporter: CrashReporter,
    private val installationIdentity: InstallationIdentity
) {
    /**
     * Sets the installation ID as user ID for crash-trace correlation.
     * Suspend function - call from a coroutine scope.
     */
    suspend fun initialize() {
        val installationId = installationIdentity.getInstallationId()
        crashReporter.setUserId(installationId)
    }
}
