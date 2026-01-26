package io.github.devmugi.cv.agent.di

import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.BuildConfig
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.createPlatformAnalyticsWithIdentity
import io.github.devmugi.cv.agent.identity.InstallationIdentity
import io.github.devmugi.cv.agent.identity.createPlatformInstallationIdentity
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val TAG = "AnalyticsModule"

/**
 * Android-specific DI module for Firebase Analytics and Installation Identity.
 *
 * When ENABLE_ANALYTICS is true (release builds), events are sent to Firebase.
 * When false (debug builds), NOOP implementation is used.
 */
val analyticsModule = module {
    // Installation Identity - shared across analytics and tracing
    single<InstallationIdentity> {
        createPlatformInstallationIdentity(androidContext())
    }

    single<Analytics> {
        if (BuildConfig.ENABLE_ANALYTICS) {
            Logger.d(TAG) { "Firebase Analytics ENABLED" }
            createPlatformAnalyticsWithIdentity(androidContext(), get())
        } else {
            Logger.d(TAG) { "Firebase Analytics DISABLED (using NOOP)" }
            Analytics.NOOP
        }
    }
}
