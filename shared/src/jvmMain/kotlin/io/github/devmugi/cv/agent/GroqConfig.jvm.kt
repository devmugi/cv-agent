package io.github.devmugi.cv.agent

/**
 * JVM implementation of GroqConfig.
 * Uses GroqConfigProvider which should be initialized by the consuming application.
 *
 * Before using the API:
 * ```
 * GroqConfigProvider.initialize(yourApiKey)
 * ```
 */
actual object GroqConfig {
    actual val apiKey: String
        get() = GroqConfigProvider.apiKey
}
