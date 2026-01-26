# Firebase Installations Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a `shared-identity` module that provides stable device installation IDs for correlating OpenTelemetry traces with Firebase Analytics events.

**Architecture:** New KMP module `shared-identity` with expect/actual pattern. Android uses Firebase Installations SDK; iOS uses locally-persisted UUID. Both `shared-analytics` and `shared-agent-api` depend on this module.

**Tech Stack:** Kotlin Multiplatform, Firebase Installations SDK (Android), NSUserDefaults (iOS), Koin DI

---

## Task 1: Add Firebase Installations dependency to version catalog

**Files:**
- Modify: `gradle/libs.versions.toml:158-160`

**Step 1: Add the library entry**

Add after `firebase-analytics` line:

```toml
firebase-installations = { module = "com.google.firebase:firebase-installations-ktx" }
```

**Step 2: Verify syntax**

Run: `./gradlew --version`
Expected: Gradle version info (confirms no syntax errors in version catalog)

**Step 3: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: add firebase-installations to version catalog"
```

---

## Task 2: Create shared-identity module structure

**Files:**
- Modify: `settings.gradle.kts:33`
- Create: `shared-identity/build.gradle.kts`

**Step 1: Add module to settings.gradle.kts**

Add after `include(":shared-domain")`:

```kotlin
include(":shared-identity")
```

**Step 2: Create build.gradle.kts**

Create `shared-identity/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.identity"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
            compilationName = "unitTest"
            defaultSourceSetName = "androidUnitTest"
        }
    }

    // iOS targets disabled - enable when needed
    // listOf(iosArm64(), iosSimulatorArm64()).forEach {
    //     it.binaries.framework { baseName = "SharedIdentity" }
    // }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(platform(libs.firebase.bom))
            implementation(libs.firebase.installations)
        }
    }
}
```

**Step 3: Create source directories**

```bash
mkdir -p shared-identity/src/commonMain/kotlin/io/github/devmugi/cv/agent/identity
mkdir -p shared-identity/src/commonTest/kotlin/io/github/devmugi/cv/agent/identity
mkdir -p shared-identity/src/androidMain/kotlin/io/github/devmugi/cv/agent/identity
mkdir -p shared-identity/src/iosMain/kotlin/io/github/devmugi/cv/agent/identity
```

**Step 4: Verify module is recognized**

Run: `./gradlew projects`
Expected: Output includes `:shared-identity`

**Step 5: Commit**

```bash
git add settings.gradle.kts shared-identity/
git commit -m "build: add shared-identity module skeleton"
```

---

## Task 3: Create InstallationIdentity interface

**Files:**
- Create: `shared-identity/src/commonMain/kotlin/io/github/devmugi/cv/agent/identity/InstallationIdentity.kt`

**Step 1: Write the interface**

```kotlin
package io.github.devmugi.cv.agent.identity

/**
 * Provides a stable installation identifier for this app instance.
 *
 * The ID is:
 * - Stable across app updates
 * - Reset on app reinstall or data clear
 * - Used for correlating analytics and tracing data
 *
 * Platform implementations:
 * - Android: Firebase Installation ID
 * - iOS: Locally-persisted UUID
 */
interface InstallationIdentity {
    /**
     * Returns the installation ID for this app instance.
     * The ID is cached after first retrieval.
     *
     * @throws InstallationIdentityException if ID cannot be retrieved
     */
    suspend fun getInstallationId(): String
}

/**
 * Exception thrown when installation ID cannot be retrieved.
 */
class InstallationIdentityException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-identity:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-identity/src/commonMain/
git commit -m "feat(identity): add InstallationIdentity interface"
```

---

## Task 4: Create test implementation for commonTest

**Files:**
- Create: `shared-identity/src/commonTest/kotlin/io/github/devmugi/cv/agent/identity/TestInstallationIdentity.kt`

**Step 1: Write test implementation**

```kotlin
package io.github.devmugi.cv.agent.identity

/**
 * Test implementation of [InstallationIdentity] with configurable behavior.
 */
class TestInstallationIdentity(
    private val id: String = "test-installation-id"
) : InstallationIdentity {

    var getIdCallCount = 0
        private set

    var shouldThrow: InstallationIdentityException? = null

    override suspend fun getInstallationId(): String {
        getIdCallCount++
        shouldThrow?.let { throw it }
        return id
    }

    fun reset() {
        getIdCallCount = 0
        shouldThrow = null
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-identity:compileTestKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-identity/src/commonTest/
git commit -m "test(identity): add TestInstallationIdentity for testing"
```

---

## Task 5: Implement Android FirebaseInstallationIdentity

**Files:**
- Create: `shared-identity/src/androidMain/kotlin/io/github/devmugi/cv/agent/identity/FirebaseInstallationIdentity.kt`

**Step 1: Write the implementation**

```kotlin
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
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-identity:compileAndroidMain`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-identity/src/androidMain/
git commit -m "feat(identity): add FirebaseInstallationIdentity for Android"
```

---

## Task 6: Create platform factory with expect/actual

**Files:**
- Create: `shared-identity/src/commonMain/kotlin/io/github/devmugi/cv/agent/identity/IdentityFactory.kt`
- Create: `shared-identity/src/androidMain/kotlin/io/github/devmugi/cv/agent/identity/IdentityFactory.android.kt`
- Create: `shared-identity/src/iosMain/kotlin/io/github/devmugi/cv/agent/identity/IdentityFactory.ios.kt`

**Step 1: Write expect declaration**

Create `shared-identity/src/commonMain/kotlin/io/github/devmugi/cv/agent/identity/IdentityFactory.kt`:

```kotlin
package io.github.devmugi.cv.agent.identity

/**
 * Creates the platform-specific [InstallationIdentity] implementation.
 *
 * @param context Platform context (Android Context, null for iOS)
 * @return Platform-appropriate InstallationIdentity implementation
 */
expect fun createPlatformInstallationIdentity(context: Any?): InstallationIdentity
```

**Step 2: Write Android actual**

Create `shared-identity/src/androidMain/kotlin/io/github/devmugi/cv/agent/identity/IdentityFactory.android.kt`:

```kotlin
package io.github.devmugi.cv.agent.identity

import android.content.Context

/**
 * Android implementation - uses Firebase Installations.
 */
actual fun createPlatformInstallationIdentity(context: Any?): InstallationIdentity {
    requireNotNull(context) { "Android Context required for InstallationIdentity" }
    return FirebaseInstallationIdentity(context as Context)
}
```

**Step 3: Write iOS actual (placeholder)**

Create `shared-identity/src/iosMain/kotlin/io/github/devmugi/cv/agent/identity/IdentityFactory.ios.kt`:

```kotlin
package io.github.devmugi.cv.agent.identity

/**
 * iOS implementation - uses locally persisted UUID.
 * TODO: Implement LocalInstallationIdentity when iOS targets enabled
 */
actual fun createPlatformInstallationIdentity(context: Any?): InstallationIdentity {
    return object : InstallationIdentity {
        override suspend fun getInstallationId(): String {
            throw NotImplementedError("iOS InstallationIdentity not yet implemented")
        }
    }
}
```

**Step 4: Verify compilation**

Run: `./gradlew :shared-identity:compileAndroidMain`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared-identity/src/
git commit -m "feat(identity): add platform factory with expect/actual"
```

---

## Task 7: Add shared-identity dependency to shared-agent-api

**Files:**
- Modify: `shared-agent-api/build.gradle.kts:32`

**Step 1: Add dependency**

Add after `api(projects.sharedDomain)`:

```kotlin
api(projects.sharedIdentity)
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-agent-api:compileAndroidMain`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-agent-api/build.gradle.kts
git commit -m "build: add shared-identity dependency to shared-agent-api"
```

---

## Task 8: Add installation ID to OpenTelemetryAgentTracer

**Files:**
- Modify: `shared-agent-api/src/androidMain/kotlin/io/github/devmugi/cv/agent/api/tracing/OpenTelemetryAgentTracer.kt`

**Step 1: Add installationId parameter to startLlmSpan**

Update the `startLlmSpan` method signature (line 37) to include installation ID:

```kotlin
override fun startLlmSpan(
    model: String,
    systemPrompt: String,
    messages: List<ChatMessage>,
    temperature: Double,
    maxTokens: Int,
    sessionId: String?,
    turnNumber: Int?,
    promptMetadata: PromptMetadata?,
    installationId: String? = null
): TracingSpan {
```

**Step 2: Add installation ID attribute**

Add after the session tracking block (around line 57):

```kotlin
// Device identification
installationId?.let { spanBuilder.setAttribute("device.installation_id", it) }
```

**Step 3: Update AgentTracer interface**

Update `shared-agent-api/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/tracing/AgentTracer.kt` to include the new parameter:

```kotlin
fun startLlmSpan(
    model: String,
    systemPrompt: String,
    messages: List<ChatMessage>,
    temperature: Double,
    maxTokens: Int,
    sessionId: String? = null,
    turnNumber: Int? = null,
    promptMetadata: PromptMetadata? = null,
    installationId: String? = null
): TracingSpan
```

**Step 4: Verify compilation**

Run: `./gradlew :shared-agent-api:compileAndroidMain`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared-agent-api/src/
git commit -m "feat(tracing): add installation_id attribute to LLM spans"
```

---

## Task 9: Add shared-identity dependency to shared-analytics

**Files:**
- Modify: `shared-analytics/build.gradle.kts:30`

**Step 1: Add dependency**

Add in `commonMain.dependencies`:

```kotlin
commonMain.dependencies {
    api(projects.sharedIdentity)
}
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-analytics:compileAndroidMain`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-analytics/build.gradle.kts
git commit -m "build: add shared-identity dependency to shared-analytics"
```

---

## Task 10: Update FirebaseAnalyticsWrapper to use InstallationIdentity

**Files:**
- Modify: `shared-analytics/src/androidMain/kotlin/io/github/devmugi/cv/agent/analytics/FirebaseAnalyticsWrapper.kt`

**Step 1: Add InstallationIdentity parameter**

Update constructor and add initialization:

```kotlin
package io.github.devmugi.cv.agent.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.devmugi.cv.agent.identity.InstallationIdentity
import kotlinx.coroutines.runBlocking

/**
 * Firebase Analytics implementation of [Analytics].
 * Wraps Firebase Analytics SDK to provide analytics tracking.
 */
class FirebaseAnalyticsWrapper(
    context: Context,
    private val installationIdentity: InstallationIdentity? = null
) : Analytics {

    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    init {
        // Set installation ID as user property for correlation with traces
        installationIdentity?.let { identity ->
            runBlocking {
                try {
                    val id = identity.getInstallationId()
                    firebaseAnalytics.setUserProperty("installation_id", id)
                } catch (e: Exception) {
                    // Log but don't fail - analytics can work without this
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
```

**Step 2: Update AnalyticsFactory.android.kt**

Update to pass InstallationIdentity:

```kotlin
package io.github.devmugi.cv.agent.analytics

import android.content.Context
import io.github.devmugi.cv.agent.identity.InstallationIdentity

/**
 * Android implementation of [createPlatformAnalytics].
 *
 * @param context Android Context (required for Firebase Analytics)
 * @param installationIdentity Optional identity for correlation
 * @return FirebaseAnalyticsWrapper if context provided, NOOP otherwise
 */
actual fun createPlatformAnalytics(context: Any?): Analytics {
    return if (context != null) {
        FirebaseAnalyticsWrapper(context as Context)
    } else {
        Analytics.NOOP
    }
}

/**
 * Creates Analytics with InstallationIdentity for trace correlation.
 */
fun createPlatformAnalyticsWithIdentity(
    context: Context,
    installationIdentity: InstallationIdentity
): Analytics {
    return FirebaseAnalyticsWrapper(context, installationIdentity)
}
```

**Step 3: Verify compilation**

Run: `./gradlew :shared-analytics:compileAndroidMain`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-analytics/src/
git commit -m "feat(analytics): integrate InstallationIdentity for trace correlation"
```

---

## Task 11: Wire up InstallationIdentity in DI

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/di/AnalyticsModule.kt`

**Step 1: Update AnalyticsModule**

```kotlin
package io.github.devmugi.cv.agent.di

import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.BuildConfig
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.createPlatformAnalyticsWithIdentity
import io.github.devmugi.cv.agent.identity.InstallationIdentity
import io.github.devmugi.cv.agent.identity.createPlatformInstallationIdentity
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val TAG = "AnalyticsModule"

/**
 * Android-specific DI module for Firebase Analytics and Installation Identity.
 *
 * When ENABLE_ANALYTICS is true (release builds), events are sent to Firebase.
 * When false (debug builds), NOOP implementation is used.
 */
val analyticsModule = module {
    // Installation Identity - shared across analytics and tracing
    single<InstallationIdentity> {
        createPlatformInstallationIdentity(androidContext())
    }

    single<Analytics> {
        if (BuildConfig.ENABLE_ANALYTICS) {
            Logger.d(TAG) { "Firebase Analytics ENABLED" }
            createPlatformAnalyticsWithIdentity(androidContext(), get())
        } else {
            Logger.d(TAG) { "Firebase Analytics DISABLED (using NOOP)" }
            Analytics.NOOP
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :android-app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/di/
git commit -m "feat(di): wire InstallationIdentity in AnalyticsModule"
```

---

## Task 12: Build and verify integration

**Step 1: Full Android build**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Run quality checks**

Run: `./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL (or fix any lint/detekt issues)

**Step 3: Final commit**

```bash
git add -A
git commit -m "feat(identity): complete Firebase Installations integration

- Add shared-identity module with InstallationIdentity interface
- Implement FirebaseInstallationIdentity for Android
- Add installation_id attribute to OpenTelemetry traces
- Set installation_id user property in Firebase Analytics
- Wire up DI in AnalyticsModule

This enables correlation between Phoenix traces and Firebase Analytics
using a stable device installation ID."
```

---

## Summary

| Task | Description | Est. Steps |
|------|-------------|------------|
| 1 | Add firebase-installations to version catalog | 3 |
| 2 | Create shared-identity module structure | 5 |
| 3 | Create InstallationIdentity interface | 3 |
| 4 | Create test implementation | 3 |
| 5 | Implement FirebaseInstallationIdentity | 3 |
| 6 | Create platform factory expect/actual | 5 |
| 7 | Add dependency to shared-agent-api | 3 |
| 8 | Add installation ID to tracer | 5 |
| 9 | Add dependency to shared-analytics | 3 |
| 10 | Update FirebaseAnalyticsWrapper | 4 |
| 11 | Wire up DI | 3 |
| 12 | Build and verify | 3 |

**Total: 12 tasks, ~43 steps**
