package io.github.devmugi.cv.agent.identity

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android implementation using Firebase Installations SDK.
 *
 * The Firebase Installation ID (FID) is:
 * - Stable across app updates
 * - Reset when user reinstalls or clears app data
 * - Automatically rotated by Firebase for privacy (rare)
 *
 * @param context Android Context for Firebase initialization
 */
class FirebaseInstallationIdentity(context: Context) : InstallationIdentity {

    init {
        // Ensure Firebase is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    private val firebaseInstallations: FirebaseInstallations =
        FirebaseInstallations.getInstance()

    override suspend fun getInstallationId(): String {
        return suspendCancellableCoroutine { continuation ->
            firebaseInstallations.id
                .addOnSuccessListener { id ->
                    continuation.resume(id)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(
                        InstallationIdentityException(
                            "Failed to get Firebase Installation ID",
                            exception
                        )
                    )
                }
        }
    }
}
