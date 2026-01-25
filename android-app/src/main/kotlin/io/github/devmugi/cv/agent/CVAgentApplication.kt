package io.github.devmugi.cv.agent

import android.app.Application
import io.github.devmugi.cv.agent.di.appModule
import io.github.devmugi.cv.agent.di.tracingModule
import io.github.devmugi.cv.agent.di.viewModelModule
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
            modules(appModule, viewModelModule, tracingModule)
        }
    }
}
