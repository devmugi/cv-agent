package io.github.devmugi.cv.agent.identity

/**
 * Creates the platform-specific [InstallationIdentity] implementation.
 *
 * @param context Platform context (Android Context, null for iOS)
 * @return Platform-appropriate InstallationIdentity implementation
 */
expect fun createPlatformInstallationIdentity(context: Any?): InstallationIdentity
