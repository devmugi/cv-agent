package io.github.devmugi.cv.agent.analytics

/**
 * Android implementation of [createPlatformAnalytics].
 *
 * Phase 1: Returns NOOP implementation
 * Phase 2: Will return FirebaseAnalyticsWrapper when Firebase is integrated
 *
 * @param context Android Context (required for Firebase Analytics)
 * @return Analytics implementation (NOOP in Phase 1, Firebase in Phase 2)
 */
actual fun createPlatformAnalytics(context: Any?): Analytics {
    // Phase 1: Return NOOP
    // Phase 2: Uncomment and return FirebaseAnalyticsWrapper
    // return if (context != null) {
    //     FirebaseAnalyticsWrapper(context as android.content.Context)
    // } else {
    //     Analytics.NOOP
    // }
    return Analytics.NOOP
}
