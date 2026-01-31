package io.github.devmugi.cv.agent.identity

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Callback-based interface that Swift can implement.
 * Swift provides Firebase Installations implementation.
 */
interface InstallationIdentityProvider {
    /**
     * Get the installation ID via callback.
     * @param completion Called with (id, errorMessage) - either id is non-null or errorMessage is non-null
     */
    fun getInstallationId(completion: (String?, String?) -> Unit)
}

/**
 * Kotlin wrapper that bridges [InstallationIdentityProvider] callback to suspend [InstallationIdentity].
 */
class SwiftInstallationIdentity(
    private val provider: InstallationIdentityProvider
) : InstallationIdentity {

    override suspend fun getInstallationId(): String = suspendCancellableCoroutine { cont ->
        provider.getInstallationId { id, errorMessage ->
            if (errorMessage != null || id == null) {
                cont.resumeWithException(
                    InstallationIdentityException(
                        errorMessage ?: "Failed to get installation ID"
                    )
                )
            } else {
                cont.resume(id)
            }
        }
    }
}
