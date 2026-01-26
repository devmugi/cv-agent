package io.github.devmugi.cv.agent.crashlytics

/**
 * iOS implementation - returns NoOp (future: Firebase Crashlytics iOS).
 */
actual fun createPlatformCrashReporter(): CrashReporter = CrashReporter.NOOP
