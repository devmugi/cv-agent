package io.github.devmugi.cv.agent.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.devmugi.cv.agent.identity.InstallationIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase Analytics implementation of [Analytics].
 * Wraps Firebase Analytics SDK to provide analytics tracking.
 */
class FirebaseAnalyticsWrapper(
    context: Context,
    private val installationIdentity: InstallationIdentity? = null
) : Analytics {

    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Set installation ID as user property for correlation with traces (async, non-blocking)
        installationIdentity?.let { identity ->
            scope.launch {
                try {
                    val id = identity.getInstallationId()
                    firebaseAnalytics.setUserProperty("installation_id", id)
                    Log.d("FirebaseAnalytics", "Installation ID set: ${id.take(8)}...")
                } catch (e: Exception) {
                    Log.w("FirebaseAnalytics", "Failed to set installation ID: ${e.message}")
                }
            }
        }
    }

    override fun logEvent(event: AnalyticsEvent) {
        val bundle = Bundle().apply {
            event.params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    null -> { /* Skip null values */ }
                }
            }
        }
        firebaseAnalytics.logEvent(event.name, bundle)
    }

    override fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }

    override fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    override fun setCurrentScreen(screenName: String, screenClass: String?) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}
