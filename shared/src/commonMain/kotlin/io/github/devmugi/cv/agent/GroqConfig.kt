package io.github.devmugi.cv.agent

/**
 * Configuration for Groq API access.
 * The API key is provided through platform-specific implementations:
 * - Android: From the app's BuildConfig
 * - iOS: From the app's Info.plist
 */
expect object GroqConfig {
    val apiKey: String
}

/**
 * Allows the consuming application to provide the API key.
 * This is used on Android where the library doesn't have access to BuildConfig.
 */
object GroqConfigProvider {
    private var _apiKey: String = ""

    fun initialize(apiKey: String) {
        _apiKey = apiKey
    }

    val apiKey: String
        get() = _apiKey
}
