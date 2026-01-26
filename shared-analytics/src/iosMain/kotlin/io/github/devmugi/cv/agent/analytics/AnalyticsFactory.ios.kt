package io.github.devmugi.cv.agent.analytics

/**
 * iOS implementation of [createPlatformAnalytics].
 *
 * Currently returns NOOP implementation.
 * Future: Will return Firebase iOS Analytics when integrated.
 *
 * @param context Not used on iOS, pass null
 * @return NOOP Analytics implementation
 */
actual fun createPlatformAnalytics(context: Any?): Analytics {
    // Future: Return Firebase iOS Analytics
    return Analytics.NOOP
}
