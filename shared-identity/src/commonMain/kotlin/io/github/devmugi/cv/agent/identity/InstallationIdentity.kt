package io.github.devmugi.cv.agent.identity

/**
 * Provides a stable installation identifier for this app instance.
 *
 * The ID is:
 * - Stable across app updates
 * - Reset on app reinstall or data clear
 * - Used for correlating analytics and tracing data
 *
 * Platform implementations:
 * - Android: Firebase Installation ID
 * - iOS: Locally-persisted UUID
 */
interface InstallationIdentity {
    /**
     * Returns the installation ID for this app instance.
     * The ID is cached after first retrieval.
     *
     * @throws InstallationIdentityException if ID cannot be retrieved
     */
    suspend fun getInstallationId(): String
}

/**
 * Exception thrown when installation ID cannot be retrieved.
 */
class InstallationIdentityException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
