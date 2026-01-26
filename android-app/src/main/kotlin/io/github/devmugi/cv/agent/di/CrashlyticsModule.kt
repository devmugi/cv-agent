package io.github.devmugi.cv.agent.di

import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.BuildConfig
import io.github.devmugi.cv.agent.crashlytics.CrashReporter
import io.github.devmugi.cv.agent.crashlytics.CrashReporterInitializer
import io.github.devmugi.cv.agent.crashlytics.createPlatformCrashReporter
import org.koin.dsl.module

private const val TAG = "CrashlyticsModule"

/**
 * Android-specific DI module for Firebase Crashlytics.
 *
 * When ENABLE_CRASHLYTICS is true, crashes are sent to Firebase.
 * When false (prodDebug), NOOP implementation is used.
 *
 * This module overrides the shared crashlyticsModule to add BuildConfig awareness.
 */
val crashlyticsModule = module {
    single<CrashReporter> {
        if (BuildConfig.ENABLE_CRASHLYTICS) {
            Logger.d(TAG) { "Firebase Crashlytics ENABLED" }
            createPlatformCrashReporter()
        } else {
            Logger.d(TAG) { "Firebase Crashlytics DISABLED (using NOOP)" }
            CrashReporter.NOOP
        }
    }

    single { CrashReporterInitializer(get(), get()) }
}
