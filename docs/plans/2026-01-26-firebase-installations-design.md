# Firebase Installations Integration Design

## Overview

Add a `shared-identity` module that provides a stable device installation ID for correlating OpenTelemetry traces with Firebase Analytics events.

## Goals

- Single device ID available to both tracing and analytics systems
- Correlate LLM traces in Phoenix with analytics events in Firebase
- Clean architecture: separate module avoids coupling tracing to analytics

## Module Architecture

### New Module: `shared-identity`

```
shared-identity/
├── src/commonMain/kotlin/io/github/devmugi/cv/agent/identity/
│   └── InstallationIdentity.kt      # Interface + factory
├── src/androidMain/kotlin/io/github/devmugi/cv/agent/identity/
│   ├── FirebaseInstallationIdentity.kt  # Firebase Installations impl
│   └── IdentityFactory.android.kt
└── src/iosMain/kotlin/io/github/devmugi/cv/agent/identity/
    ├── LocalInstallationIdentity.kt     # UUID persisted to NSUserDefaults
    └── IdentityFactory.ios.kt
```

### Dependency Flow

```
shared-identity (NEW - no dependencies)
       ↑
       ├──────────────┬────────────────┐
       │              │                │
shared-analytics  shared-agent-api   (future modules)
```

## Interface Design

```kotlin
interface InstallationIdentity {
    suspend fun getInstallationId(): String
}
```

Lazy suspend function approach - callers handle async nature, no startup cost.

## Platform Implementations

### Android: Firebase Installations

```kotlin
class FirebaseInstallationIdentity(context: Context) : InstallationIdentity {

    private val firebaseInstallations = FirebaseInstallations.getInstance()

    override suspend fun getInstallationId(): String {
        return suspendCancellableCoroutine { continuation ->
            firebaseInstallations.id
                .addOnSuccessListener { id -> continuation.resume(id) }
                .addOnFailureListener { e -> continuation.resumeWithException(e) }
        }
    }
}
```

**Behavior:**
- Stable across app updates
- Changes on reinstall or clear app data
- Managed by Firebase (automatic rotation handling)

### iOS: Local UUID

```kotlin
class LocalInstallationIdentity : InstallationIdentity {

    private val key = "cv_agent_installation_id"

    override suspend fun getInstallationId(): String {
        val defaults = NSUserDefaults.standardUserDefaults

        defaults.stringForKey(key)?.let { return it }

        val newId = NSUUID().UUIDString
        defaults.setObject(newId, forKey = key)
        defaults.synchronize()
        return newId
    }
}
```

**Behavior:**
- Generates UUID on first call, persists to NSUserDefaults
- Survives app updates, cleared on uninstall
- Mirrors Firebase Installations behavior for correlation purposes

## Integration

### OpenTelemetry Tracing (shared-agent-api)

Update `OpenTelemetryAgentTracer` to include installation ID as span attribute:

```kotlin
class OpenTelemetryAgentTracer(
    private val installationIdentity: InstallationIdentity
) : AgentTracer {

    override suspend fun traceCompletion(...) {
        val installationId = installationIdentity.getInstallationId()
        span.setAttribute("device.installation_id", installationId)
        // ... rest of tracing logic
    }
}
```

### Firebase Analytics (shared-analytics)

Set installation ID as user property for segmentation:

```kotlin
class FirebaseAnalyticsWrapper(
    context: Context,
    private val installationIdentity: InstallationIdentity
) : Analytics {

    suspend fun initialize() {
        val id = installationIdentity.getInstallationId()
        firebaseAnalytics.setUserProperty("installation_id", id)
    }
}
```

### Koin DI (AppModule.kt)

```kotlin
single<InstallationIdentity> { createPlatformInstallationIdentity() }
```

## Build Configuration

### shared-identity/build.gradle.kts

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
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "SharedIdentity" }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(platform(libs.firebase.bom))
            implementation(libs.firebase.installations)
        }
    }
}
```

### libs.versions.toml Addition

```toml
firebase-installations = { module = "com.google.firebase:firebase-installations-ktx" }
```

No version needed - managed by Firebase BOM.

## Files to Create

1. `shared-identity/build.gradle.kts`
2. `shared-identity/src/commonMain/kotlin/io/github/devmugi/cv/agent/identity/InstallationIdentity.kt`
3. `shared-identity/src/androidMain/kotlin/io/github/devmugi/cv/agent/identity/FirebaseInstallationIdentity.kt`
4. `shared-identity/src/androidMain/kotlin/io/github/devmugi/cv/agent/identity/IdentityFactory.android.kt`
5. `shared-identity/src/iosMain/kotlin/io/github/devmugi/cv/agent/identity/LocalInstallationIdentity.kt`
6. `shared-identity/src/iosMain/kotlin/io/github/devmugi/cv/agent/identity/IdentityFactory.ios.kt`

## Files to Modify

1. `settings.gradle.kts` - add `include(":shared-identity")`
2. `gradle/libs.versions.toml` - add firebase-installations library
3. `shared-agent-api/build.gradle.kts` - add dependency on shared-identity
4. `shared-analytics/build.gradle.kts` - add dependency on shared-identity
5. `shared-agent-api/.../tracing/OpenTelemetryAgentTracer.kt` - add installation_id attribute
6. `shared-analytics/.../FirebaseAnalyticsWrapper.kt` - set installation_id user property
7. `shared/src/commonMain/.../di/AppModule.kt` - wire up InstallationIdentity
