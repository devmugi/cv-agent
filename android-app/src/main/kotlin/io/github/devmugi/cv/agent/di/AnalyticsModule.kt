package io.github.devmugi.cv.agent.di

import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.BuildConfig
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.createPlatformAnalytics
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val TAG = "AnalyticsModule"

/**
 * Android-specific DI module for Firebase Analytics.
 *
 * When ENABLE_ANALYTICS is true (release builds), events are sent to Firebase.
 * When false (debug builds), NOOP implementation is used.
 */
val analyticsModule = module {
    single<Analytics> {
        if (BuildConfig.ENABLE_ANALYTICS) {
            Logger.d(TAG) { "Firebase Analytics ENABLED" }
            createPlatformAnalytics(androidContext())
        } else {
            Logger.d(TAG) { "Firebase Analytics DISABLED (using NOOP)" }
            Analytics.NOOP
        }
    }
}
