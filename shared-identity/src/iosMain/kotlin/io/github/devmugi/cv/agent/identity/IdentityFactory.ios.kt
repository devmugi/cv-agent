package io.github.devmugi.cv.agent.identity

import platform.Foundation.NSUUID

/**
 * iOS implementation - uses locally persisted UUID.
 *
 * Note: This is a simple in-memory stub. For production, implement
 * persistence using NSUserDefaults or Keychain.
 */
actual fun createPlatformInstallationIdentity(context: Any?): InstallationIdentity {
    return object : InstallationIdentity {
        private var cachedId: String? = null

        override suspend fun getInstallationId(): String {
            return cachedId ?: NSUUID().UUIDString.also { cachedId = it }
        }
    }
}
