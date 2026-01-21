package io.github.devmugi.cv.agent

/**
 * Android implementation of GroqConfig.
 * Uses GroqConfigProvider which should be initialized by the consuming application
 * with the API key from its BuildConfig.
 *
 * In the consuming app's Application.onCreate() or before using the API:
 * ```
 * GroqConfigProvider.initialize(BuildConfig.GROQ_API_KEY)
 * ```
 */
actual object GroqConfig {
    actual val apiKey: String
        get() = GroqConfigProvider.apiKey
}
