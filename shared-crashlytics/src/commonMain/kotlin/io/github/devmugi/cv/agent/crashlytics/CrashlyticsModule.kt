package io.github.devmugi.cv.agent.crashlytics

import org.koin.dsl.module

/**
 * Koin module for crash reporting dependencies.
 *
 * Provides:
 * - [CrashReporter] - platform-specific crash reporter
 * - [CrashReporterInitializer] - initializer for trace correlation
 *
 * Note: [InstallationIdentity] must be provided by another module (analyticsModule).
 */
val crashlyticsModule = module {
    single<CrashReporter> { createPlatformCrashReporter() }
    single { CrashReporterInitializer(get(), get()) }
}
