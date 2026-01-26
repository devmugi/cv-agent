# Crashlytics Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add Firebase Crashlytics for crash reporting with trace correlation via installation_id.

**Architecture:** New `shared-crashlytics` KMP module with `CrashReporter` interface. Android uses Firebase Crashlytics. iOS uses NOOP. `CrashReporterInitializer` sets installation_id as user ID for trace correlation.

**Tech Stack:** Firebase Crashlytics, Kotlin Multiplatform, Koin DI

---

## Task 1: Add Gradle Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`

**Step 1: Add crashlytics version and library**

In `libs.versions.toml`, add to `[versions]` section after `google-services`:

```toml
firebase-crashlytics-plugin = "3.0.3"
```

Add to `[libraries]` section after `firebase-installations`:

```toml
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics-ktx" }
```

Add to `[plugins]` section after `google-services`:

```toml
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-plugin" }
```

**Step 2: Verify syntax**

Run: `./gradlew --version`
Expected: No TOML parsing errors

**Step 3: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: add Firebase Crashlytics dependencies to version catalog"
```

---

## Task 2: Create Module Skeleton

**Files:**
- Create: `shared-crashlytics/build.gradle.kts`
- Modify: `settings.gradle.kts`

**Step 1: Create build.gradle.kts**

Create `shared-crashlytics/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.crashlytics"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
            compilationName = "unitTest"
            defaultSourceSetName = "androidUnitTest"
        }
    }

    // iOS targets disabled - enable when needed
    // listOf(iosArm64(), iosSimulatorArm64()).forEach {
    //     it.binaries.framework { baseName = "SharedCrashlytics" }
    // }

    sourceSets {
        commonMain.dependencies {
            api(projects.sharedIdentity)
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.firebase.crashlytics)
        }
    }
}
```

**Step 2: Add module to settings.gradle.kts**

Add after `include(":shared-analytics")`:

```kotlin
include(":shared-crashlytics")
```

**Step 3: Create source directories**

Run:
```bash
mkdir -p shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics
mkdir -p shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics
mkdir -p shared-crashlytics/src/androidMain/kotlin/io/github/devmugi/cv/agent/crashlytics
mkdir -p shared-crashlytics/src/iosMain/kotlin/io/github/devmugi/cv/agent/crashlytics
```

**Step 4: Verify module compiles**

Run: `./gradlew :shared-crashlytics:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL (empty module)

**Step 5: Commit**

```bash
git add shared-crashlytics settings.gradle.kts
git commit -m "build: add shared-crashlytics module skeleton"
```

---

## Task 3: Create CrashReporter Interface

**Files:**
- Create: `shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporter.kt`

**Step 1: Write the interface**

Create `CrashReporter.kt`:

```kotlin
package io.github.devmugi.cv.agent.crashlytics

/**
 * Crash reporting interface for recording exceptions and diagnostic info.
 * Platform-specific implementations provided via [createPlatformCrashReporter].
 *
 * Android: Firebase Crashlytics
 * iOS: NoOp (future implementation)
 */
interface CrashReporter {
    /**
     * Record a non-fatal exception for crash reporting.
     */
    fun recordException(throwable: Throwable)

    /**
     * Log a message that will appear in the crash report timeline.
     */
    fun log(message: String)

    /**
     * Set a custom key-value pair for crash context.
     */
    fun setCustomKey(key: String, value: String)

    /**
     * Set the user ID for crash correlation.
     * Used to link crashes with OpenTelemetry traces via installation_id.
     */
    fun setUserId(userId: String)

    companion object {
        /**
         * No-op implementation for testing and platforms without crash reporting.
         */
        val NOOP: CrashReporter = NoOpCrashReporter()
    }
}

/**
 * No-op implementation that discards all crash reports.
 * Used for testing and as default when crash reporting is unavailable.
 */
internal class NoOpCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) {}
    override fun log(message: String) {}
    override fun setCustomKey(key: String, value: String) {}
    override fun setUserId(userId: String) {}
}
```

**Step 2: Verify compiles**

Run: `./gradlew :shared-crashlytics:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporter.kt
git commit -m "feat(crashlytics): add CrashReporter interface with NoOp implementation"
```

---

## Task 4: Create TestCrashReporter

**Files:**
- Create: `shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/TestCrashReporter.kt`

**Step 1: Write test double**

Create `TestCrashReporter.kt`:

```kotlin
package io.github.devmugi.cv.agent.crashlytics

/**
 * Test implementation of [CrashReporter] with observable state.
 */
class TestCrashReporter : CrashReporter {
    val recordedExceptions = mutableListOf<Throwable>()
    val loggedMessages = mutableListOf<String>()
    val customKeys = mutableMapOf<String, String>()
    var userId: String? = null
        private set

    override fun recordException(throwable: Throwable) {
        recordedExceptions.add(throwable)
    }

    override fun log(message: String) {
        loggedMessages.add(message)
    }

    override fun setCustomKey(key: String, value: String) {
        customKeys[key] = value
    }

    override fun setUserId(userId: String) {
        this.userId = userId
    }

    fun reset() {
        recordedExceptions.clear()
        loggedMessages.clear()
        customKeys.clear()
        userId = null
    }
}
```

**Step 2: Verify compiles**

Run: `./gradlew :shared-crashlytics:compileTestKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/TestCrashReporter.kt
git commit -m "test(crashlytics): add TestCrashReporter for unit testing"
```

---

## Task 5: Create Factory with expect/actual

**Files:**
- Create: `shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporterFactory.kt`
- Create: `shared-crashlytics/src/iosMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporterFactory.ios.kt`

**Step 1: Write expect declaration**

Create `CrashReporterFactory.kt` in commonMain:

```kotlin
package io.github.devmugi.cv.agent.crashlytics

/**
 * Creates the platform-specific [CrashReporter] implementation.
 *
 * @return Platform-appropriate CrashReporter implementation
 */
expect fun createPlatformCrashReporter(): CrashReporter
```

**Step 2: Write iOS actual (NoOp)**

Create `CrashReporterFactory.ios.kt` in iosMain:

```kotlin
package io.github.devmugi.cv.agent.crashlytics

/**
 * iOS implementation - returns NoOp (future: Firebase Crashlytics iOS).
 */
actual fun createPlatformCrashReporter(): CrashReporter = CrashReporter.NOOP
```

**Step 3: Verify compiles (will fail - need Android actual)**

Run: `./gradlew :shared-crashlytics:compileKotlinAndroid`
Expected: FAIL - missing actual for Android

**Step 4: Commit partial progress**

```bash
git add shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporterFactory.kt
git add shared-crashlytics/src/iosMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporterFactory.ios.kt
git commit -m "feat(crashlytics): add CrashReporterFactory expect declaration and iOS actual"
```

---

## Task 6: Create Firebase Android Implementation

**Files:**
- Create: `shared-crashlytics/src/androidMain/kotlin/io/github/devmugi/cv/agent/crashlytics/FirebaseCrashReporter.kt`
- Create: `shared-crashlytics/src/androidMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporterFactory.android.kt`

**Step 1: Write FirebaseCrashReporter**

Create `FirebaseCrashReporter.kt` in androidMain:

```kotlin
package io.github.devmugi.cv.agent.crashlytics

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

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
```

**Step 2: Write Android factory actual**

Create `CrashReporterFactory.android.kt` in androidMain:

```kotlin
package io.github.devmugi.cv.agent.crashlytics

/**
 * Android implementation - uses Firebase Crashlytics.
 */
actual fun createPlatformCrashReporter(): CrashReporter = FirebaseCrashReporter()
```

**Step 3: Verify compiles**

Run: `./gradlew :shared-crashlytics:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-crashlytics/src/androidMain/kotlin/io/github/devmugi/cv/agent/crashlytics/
git commit -m "feat(crashlytics): add FirebaseCrashReporter Android implementation"
```

---

## Task 7: Create CrashReporterInitializer

**Files:**
- Create: `shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporterInitializer.kt`
- Create: `shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporterInitializerTest.kt`

**Step 1: Write the failing test**

Create `CrashReporterInitializerTest.kt`:

```kotlin
package io.github.devmugi.cv.agent.crashlytics

import io.github.devmugi.cv.agent.identity.TestInstallationIdentity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CrashReporterInitializerTest {

    @Test
    fun `initialize sets installation id as user id`() = runTest {
        val testCrashReporter = TestCrashReporter()
        val testIdentity = TestInstallationIdentity("test-install-123")
        val initializer = CrashReporterInitializer(testCrashReporter, testIdentity)

        initializer.initialize()

        assertEquals("test-install-123", testCrashReporter.userId)
    }

    @Test
    fun `initialize calls getInstallationId once`() = runTest {
        val testCrashReporter = TestCrashReporter()
        val testIdentity = TestInstallationIdentity("any-id")
        val initializer = CrashReporterInitializer(testCrashReporter, testIdentity)

        initializer.initialize()

        assertEquals(1, testIdentity.getIdCallCount)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared-crashlytics:testAndroidUnitTest --tests "*CrashReporterInitializerTest*"`
Expected: FAIL - CrashReporterInitializer not found

**Step 3: Write CrashReporterInitializer**

Create `CrashReporterInitializer.kt` in commonMain:

```kotlin
package io.github.devmugi.cv.agent.crashlytics

import io.github.devmugi.cv.agent.identity.InstallationIdentity

/**
 * Initializes CrashReporter with installation ID for trace correlation.
 * Call [initialize] once at app startup after DI is ready.
 */
class CrashReporterInitializer(
    private val crashReporter: CrashReporter,
    private val installationIdentity: InstallationIdentity
) {
    /**
     * Sets the installation ID as user ID for crash-trace correlation.
     * Suspend function - call from a coroutine scope.
     */
    suspend fun initialize() {
        val installationId = installationIdentity.getInstallationId()
        crashReporter.setUserId(installationId)
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :shared-crashlytics:testAndroidUnitTest --tests "*CrashReporterInitializerTest*"`
Expected: 2 tests PASSED

**Step 5: Commit**

```bash
git add shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporterInitializer.kt
git add shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashReporterInitializerTest.kt
git commit -m "feat(crashlytics): add CrashReporterInitializer with trace correlation"
```

---

## Task 8: Create Koin Module

**Files:**
- Create: `shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsModule.kt`

**Step 1: Write the Koin module**

Create `CrashlyticsModule.kt`:

```kotlin
package io.github.devmugi.cv.agent.crashlytics

import org.koin.dsl.module

/**
 * Koin module for crash reporting dependencies.
 *
 * Provides:
 * - [CrashReporter] - platform-specific crash reporter
 * - [CrashReporterInitializer] - initializer for trace correlation
 *
 * Note: [InstallationIdentity] must be provided by another module (analyticsModule).
 */
val crashlyticsModule = module {
    single<CrashReporter> { createPlatformCrashReporter() }
    single { CrashReporterInitializer(get(), get()) }
}
```

**Step 2: Verify compiles**

Run: `./gradlew :shared-crashlytics:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsModule.kt
git commit -m "feat(crashlytics): add Koin DI module"
```

---

## Task 9: Wire Module into Android App

**Files:**
- Modify: `build.gradle.kts` (root)
- Modify: `android-app/build.gradle.kts`
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/CVAgentApplication.kt`

**Step 1: Add crashlytics plugin to root build.gradle.kts**

Add after `alias(libs.plugins.detekt)`:

```kotlin
alias(libs.plugins.firebase.crashlytics) apply false
```

**Step 2: Add crashlytics plugin and dependency to android-app**

In `android-app/build.gradle.kts` plugins block, add after `alias(libs.plugins.google.services)`:

```kotlin
alias(libs.plugins.firebase.crashlytics)
```

In dependencies block, add after `implementation(projects.sharedAgentApi)`:

```kotlin
implementation(projects.sharedCrashlytics)
```

**Step 3: Register crashlyticsModule in CVAgentApplication**

In `CVAgentApplication.kt`, add import:

```kotlin
import io.github.devmugi.cv.agent.crashlytics.crashlyticsModule
```

Update modules() call to include crashlyticsModule:

```kotlin
modules(appModule, viewModelModule, tracingModule, analyticsModule, crashlyticsModule)
```

**Step 4: Verify compiles**

Run: `./gradlew :android-app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add build.gradle.kts android-app/build.gradle.kts android-app/src/main/kotlin/io/github/devmugi/cv/agent/CVAgentApplication.kt
git commit -m "build: wire shared-crashlytics module into Android app"
```

---

## Task 10: Initialize CrashReporter at Startup

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/CVAgentApplication.kt`

**Step 1: Add coroutine initialization**

In `CVAgentApplication.kt`, add imports:

```kotlin
import io.github.devmugi.cv.agent.crashlytics.CrashReporterInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
```

Add application scope and initialization in `onCreate()` after startKoin:

```kotlin
// Initialize crash reporter with installation ID for trace correlation
CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
    get<CrashReporterInitializer>().initialize()
}
```

**Step 2: Verify compiles**

Run: `./gradlew :android-app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/CVAgentApplication.kt
git commit -m "feat(crashlytics): initialize CrashReporter with installation ID at startup"
```

---

## Task 11: Run All Tests and Quality Checks

**Files:** None (verification only)

**Step 1: Run shared-crashlytics tests**

Run: `./gradlew :shared-crashlytics:testAndroidUnitTest`
Expected: All tests PASSED

**Step 2: Run quality checks**

Run: `./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL (no lint/detekt errors)

**Step 3: Build debug APK**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL, APK created

**Step 4: Commit any fixes if needed**

If quality checks fail, fix issues and commit:
```bash
git add -A
git commit -m "fix: address lint/detekt issues"
```

---

## Task 12: Manual Verification (Optional)

**Files:** None

**Step 1: Run app on device/emulator**

Install and run the debug APK.

**Step 2: Force a test crash**

Add temporary crash trigger (remove after testing):
```kotlin
throw RuntimeException("Test crash for Crashlytics")
```

**Step 3: Check Firebase Console**

1. Open Firebase Console > Crashlytics
2. Wait ~5 minutes for crash to appear
3. Verify installation_id appears as User ID in crash details

**Step 4: Remove test crash code**

Remove the temporary crash trigger and rebuild.

---

## Summary

| Task | Description | Commit Message |
|------|-------------|----------------|
| 1 | Add Gradle dependencies | `build: add Firebase Crashlytics dependencies to version catalog` |
| 2 | Create module skeleton | `build: add shared-crashlytics module skeleton` |
| 3 | Create CrashReporter interface | `feat(crashlytics): add CrashReporter interface with NoOp implementation` |
| 4 | Create TestCrashReporter | `test(crashlytics): add TestCrashReporter for unit testing` |
| 5 | Create Factory expect/actual | `feat(crashlytics): add CrashReporterFactory expect declaration and iOS actual` |
| 6 | Create Firebase implementation | `feat(crashlytics): add FirebaseCrashReporter Android implementation` |
| 7 | Create CrashReporterInitializer | `feat(crashlytics): add CrashReporterInitializer with trace correlation` |
| 8 | Create Koin module | `feat(crashlytics): add Koin DI module` |
| 9 | Wire into Android app | `build: wire shared-crashlytics module into Android app` |
| 10 | Initialize at startup | `feat(crashlytics): initialize CrashReporter with installation ID at startup` |
| 11 | Run tests and quality checks | (verification only) |
| 12 | Manual verification | (optional, no commit) |
