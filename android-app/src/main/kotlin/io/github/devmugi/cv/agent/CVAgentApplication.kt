package io.github.devmugi.cv.agent

import android.app.Application
import io.github.devmugi.cv.agent.crashlytics.CrashReporterInitializer
import io.github.devmugi.cv.agent.crashlytics.crashlyticsModule
import io.github.devmugi.cv.agent.di.analyticsModule
import io.github.devmugi.cv.agent.di.appModule
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

        // Initialize crash reporter with installation ID for trace correlation
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            try {
                get<CrashReporterInitializer>().initialize()
            } catch (e: Exception) {
                Logger.e("CVAgentApplication") { "Failed to initialize crash reporter: ${e.message}" }
            }
        }
    }
}
