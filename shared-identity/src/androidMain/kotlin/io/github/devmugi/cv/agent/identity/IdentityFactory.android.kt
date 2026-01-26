package io.github.devmugi.cv.agent.identity

import android.content.Context

/**
 * Android implementation - uses Firebase Installations.
 */
actual fun createPlatformInstallationIdentity(context: Any?): InstallationIdentity {
    requireNotNull(context) { "Android Context required for InstallationIdentity" }
    return FirebaseInstallationIdentity(context as Context)
}
