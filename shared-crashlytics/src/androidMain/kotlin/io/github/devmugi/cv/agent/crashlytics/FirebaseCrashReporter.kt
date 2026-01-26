package io.github.devmugi.cv.agent.crashlytics

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

/**
 * Android implementation using Firebase Crashlytics.
 */
internal class FirebaseCrashReporter : CrashReporter {
    private val crashlytics = Firebase.crashlytics

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }
}
