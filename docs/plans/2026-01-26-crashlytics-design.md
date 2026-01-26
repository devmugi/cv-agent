# Crashlytics Integration Design

## Overview

Add Firebase Crashlytics for crash reporting with trace correlation via installation_id.

**Goals:**
- Production crash monitoring
- Development crash debugging (enabled in all builds)
- Correlation with OpenTelemetry traces via shared installation_id

## Module Structure

New module: `shared-crashlytics`

```
shared-crashlytics/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/
    │   ├── CrashReporter.kt
    │   ├── CrashReporterFactory.kt
    │   ├── CrashReporterInitializer.kt
    │   └── CrashlyticsModule.kt
    ├── androidMain/kotlin/io/github/devmugi/cv/agent/crashlytics/
    │   ├── FirebaseCrashReporter.kt
    │   └── CrashReporterFactory.android.kt
    ├── iosMain/kotlin/io/github/devmugi/cv/agent/crashlytics/
    │   └── CrashReporterFactory.ios.kt
    └── commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/
        └── TestCrashReporter.kt
```

**Dependencies:**
- `shared-crashlytics` depends on `shared-identity`
- `android-app` depends on `shared-crashlytics`

## Interface Design

```kotlin
interface CrashReporter {
    fun recordException(throwable: Throwable)
    fun log(message: String)
    fun setCustomKey(key: String, value: String)
    fun setUserId(userId: String)

    companion object {
        val NOOP: CrashReporter = NoOpCrashReporter()
    }
}
```

## Android Implementation

```kotlin
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

## Trace Correlation

`CrashReporterInitializer` wires installation_id as Crashlytics user ID:

```kotlin
class CrashReporterInitializer(
    private val crashReporter: CrashReporter,
    private val installationIdentity: InstallationIdentity
) {
    suspend fun initialize() {
        val installationId = installationIdentity.getInstallationId()
        crashReporter.setUserId(installationId)
    }
}
```

Called from MainActivity at startup:
```kotlin
lifecycleScope.launch {
    get<CrashReporterInitializer>().initialize()
}
```

## Gradle Changes

**libs.versions.toml:**
```toml
[versions]
firebase-crashlytics-plugin = "3.0.3"

[libraries]
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics-ktx" }

[plugins]
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-plugin" }
```

**Root build.gradle.kts:**
```kotlin
alias(libs.plugins.firebase.crashlytics) apply false
```

**android-app/build.gradle.kts:**
```kotlin
plugins {
    alias(libs.plugins.firebase.crashlytics)
}

dependencies {
    implementation(projects.sharedCrashlytics)
}
```

## Configuration

- **Debug builds:** Crashlytics enabled (catches development crashes)
- **Release builds:** Crashlytics enabled (production monitoring)

No separate feature flag - always active.

## Verification

1. Build: `./gradlew :shared-crashlytics:compileAndroidMain`
2. Run app and force test crash
3. Check Firebase Console Crashlytics dashboard (~5 min delay)
4. Verify installation_id appears as User ID in crash report
