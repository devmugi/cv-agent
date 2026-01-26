package io.github.devmugi.cv.agent.identity

/**
 * iOS implementation - uses locally persisted UUID.
 * TODO: Implement LocalInstallationIdentity when iOS targets enabled
 */
actual fun createPlatformInstallationIdentity(context: Any?): InstallationIdentity {
    return object : InstallationIdentity {
        override suspend fun getInstallationId(): String {
            throw NotImplementedError("iOS InstallationIdentity not yet implemented")
        }
    }
}
