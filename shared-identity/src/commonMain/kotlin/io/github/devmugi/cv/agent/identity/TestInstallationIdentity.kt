package io.github.devmugi.cv.agent.identity

/**
 * Test implementation of [InstallationIdentity] with configurable behavior.
 * Available in commonMain for cross-module test usage.
 */
class TestInstallationIdentity(
    private val id: String = "test-installation-id"
) : InstallationIdentity {

    var getIdCallCount = 0
        private set

    var shouldThrow: InstallationIdentityException? = null

    override suspend fun getInstallationId(): String {
        getIdCallCount++
        shouldThrow?.let { throw it }
        return id
    }

    fun reset() {
        getIdCallCount = 0
        shouldThrow = null
    }
}
