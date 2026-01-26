package io.github.devmugi.cv.agent.analytics

import android.content.Context

/**
 * Android implementation of [createPlatformAnalytics].
 *
 * @param context Android Context (required for Firebase Analytics)
 * @return FirebaseAnalyticsWrapper if context provided, NOOP otherwise
 */
actual fun createPlatformAnalytics(context: Any?): Analytics {
    return if (context != null) {
        FirebaseAnalyticsWrapper(context as Context)
    } else {
        Analytics.NOOP
    }
}
