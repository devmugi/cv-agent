package io.github.devmugi.cv.agent.crashlytics

/**
 * Android implementation - uses Firebase Crashlytics.
 */
actual fun createPlatformCrashReporter(): CrashReporter = FirebaseCrashReporter()
