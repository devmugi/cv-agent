package io.github.devmugi.cv.agent

actual object GroqConfig {
    actual val apiKey: String
        get() = BuildConfig.GROQ_API_KEY
}
