package io.github.devmugi.cv.agent.crashlytics

/**
 * Creates the platform-specific [CrashReporter] implementation.
 *
 * @return Platform-appropriate CrashReporter implementation
 */
expect fun createPlatformCrashReporter(): CrashReporter
