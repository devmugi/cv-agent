package io.github.devmugi.cv.agent

import platform.Foundation.NSBundle

actual object GroqConfig {
    actual val apiKey: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("GROQ_API_KEY") as? String ?: ""
}
