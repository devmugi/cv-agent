package io.github.devmugi.cv.agent.analytics

import android.content.Context
import io.github.devmugi.cv.agent.identity.InstallationIdentity

/**
 * Android implementation of [createPlatformAnalytics].
 *
 * @param context Android Context (required for Firebase Analytics)
 * @param installationIdentity Optional identity for correlation
 * @return FirebaseAnalyticsWrapper if context provided, NOOP otherwise
 */
actual fun createPlatformAnalytics(context: Any?): Analytics {
    return if (context != null) {
        FirebaseAnalyticsWrapper(context as Context)
    } else {
        Analytics.NOOP
    }
}

/**
 * Creates Analytics with InstallationIdentity for trace correlation.
 */
fun createPlatformAnalyticsWithIdentity(
    context: Context,
    installationIdentity: InstallationIdentity
): Analytics {
    return FirebaseAnalyticsWrapper(context, installationIdentity)
}
