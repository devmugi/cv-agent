package io.github.devmugi.cv.agent

import android.app.Application
import io.github.devmugi.cv.agent.crashlytics.CrashReporter
import io.github.devmugi.cv.agent.crashlytics.CrashReporterInitializer
import io.github.devmugi.cv.agent.crashlytics.createCrashlyticsLogWriter
import io.github.devmugi.cv.agent.di.analyticsModule
import io.github.devmugi.cv.agent.di.appModule
import io.github.devmugi.cv.agent.di.crashlyticsModule
import io.github.devmugi.cv.agent.di.tracingModule
import io.github.devmugi.cv.agent.di.viewModelModule
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class CVAgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the API key for the shared module
        GroqConfigProvider.initialize(BuildConfig.GROQ_API_KEY)

        startKoin {
            androidLogger()
            androidContext(this@CVAgentApplication)
            allowOverride(true) // Allow tracingModule to override appModule definitions
            modules(appModule, viewModelModule, tracingModule, analyticsModule, crashlyticsModule)
        }

        // Initialize crash reporter and breadcrumb logging (only when crashlytics is enabled)
        if (BuildConfig.ENABLE_CRASHLYTICS) {
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
                try {
                    val crashReporter = get<CrashReporter>()
                    get<CrashReporterInitializer>().initialize()

                    // Add Crashlytics breadcrumb logging
                    val logWriter = createCrashlyticsLogWriter(
                        crashReporter = crashReporter,
                        isDebug = BuildConfig.DEBUG
                    )
                    Logger.addLogWriter(logWriter)
                } catch (e: Exception) {
                    Logger.e("CVAgentApplication") { "Failed to initialize crash reporter: ${e.message}" }
                }
            }
        }
    }
}
