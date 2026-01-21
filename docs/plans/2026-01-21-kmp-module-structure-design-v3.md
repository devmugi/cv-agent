# KMP Module Structure Design v3

## Overview

Restructure cv-agent into 5 modules with clear separation of concerns, separate Android/iOS entry points, and single iOS framework export.

**Key changes from v2:**
- Added `shared-domain` module to break circular dependency
- iOS entry point lives in `shared` module's `iosMain`
- `android-app` is Android-only

## Module Structure

```
cv-agent/
├── shared-design-system/        # Design tokens (no deps)
│   └── src/commonMain/          # Theme, Colors, Typography, Shapes, Dimens
│
├── shared-domain/               # Types and interfaces (no deps)
│   └── src/commonMain/          # CVData, Message, ChatState, ChatViewModel interface
│
├── shared-ui/                   # Pure presentation components
│   └── src/
│       ├── commonMain/          # Components, Screens
│       └── commonTest/          # Compose UI tests
│
├── shared/                      # Implementation + iOS entry point
│   └── src/
│       ├── commonMain/          # API client, repositories, ViewModel impl
│       ├── commonTest/          # Business logic tests
│       ├── androidMain/         # Android-specific (OkHttp, BuildConfig)
│       └── iosMain/             # iOS entry point (MainViewController)
│
├── android-app/                 # Android entry point only
│   └── src/
│       ├── main/                # MainActivity, Application, Koin setup
│       └── androidInstrumentedTest/  # E2E tests
│
└── iosApp/                      # iOS entry point (Xcode project)
```

## Dependency Graph

```
shared-design-system     shared-domain
        ↑                     ↑
        └───── shared-ui ─────┘
                   ↑
                shared (exports all for iOS)
               ↙     ↘
       android-app    iosApp (Xcode, uses Shared.framework)
```

## Module Dependencies

| Module | Depends On | Dependency Type |
|--------|-----------|-----------------|
| `shared-design-system` | (none) | — |
| `shared-domain` | (none) | — |
| `shared-ui` | `shared-design-system`, `shared-domain` | `implementation()` |
| `shared` | `shared-design-system`, `shared-domain`, `shared-ui` | `api()` (for iOS export) |
| `android-app` | `shared` | `implementation()` |

## Key Design Decisions

### 1. Kebab-case Module Names

Following Gradle's official recommendation.

### 2. `shared-domain` Module

Contains only types and interfaces - no implementation:
- `CVData`, `PersonalInfo`, `WorkExperience`, etc. (data models)
- `Message`, `ChatState`, `CVReference` (UI state)
- `ChatViewModel` interface (or abstract class with StateFlow)

This breaks the circular dependency between `shared` and `shared-ui`.

### 3. iOS Entry Point in `shared`

`MainViewController.kt` lives in `shared/src/iosMain/`. Since `shared` depends on `shared-ui`, it has access to `ChatScreen` and can compose the iOS entry point.

### 4. Android Entry Point Separate

`android-app` is a pure Android application module. It depends on `shared` which transitively provides everything.

### 5. Single iOS Framework

`shared` exports all modules (`shared-design-system`, `shared-domain`, `shared-ui`) so iOS sees one `Shared.framework`.

## Gradle Configuration

### settings.gradle.kts

```kotlin
rootProject.name = "cv-agent"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":shared-design-system")
include(":shared-domain")
include(":shared-ui")
include(":shared")
include(":android-app")
```

### Module build.gradle.kts Examples

#### shared-domain/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
```

#### shared-ui/build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.sharedDesignSystem)
            implementation(projects.sharedDomain)
            // Compose dependencies...
        }
    }
}
```

#### shared/build.gradle.kts

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(projects.sharedDesignSystem)
            export(projects.sharedDomain)
            export(projects.sharedUi)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.sharedDesignSystem)
            api(projects.sharedDomain)
            api(projects.sharedUi)
            // Implementation dependencies...
        }
    }
}
```

#### android-app/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
}

dependencies {
    implementation(projects.shared)
    // Android-specific dependencies...
}
```

## File Migration Plan

### To `shared-domain/src/commonMain/kotlin/.../domain/`

From `shared/data/models/`:
- `CVData.kt` (all CV data classes)
- `CVReference.kt`

From `shared/agent/`:
- `Message.kt`
- `ChatState.kt`

New file:
- `ChatViewModelContract.kt` (interface or abstract class)

### To `shared-ui/src/commonMain/kotlin/.../ui/`

Already there:
- `components/` - all UI components
- `ChatScreen.kt`

Update imports to use `shared-domain` types.

### To `shared/src/commonMain/kotlin/.../`

Keep:
- `api/` - GroqApiClient and models
- `agent/ChatViewModel.kt` - implementation (implements interface from shared-domain)
- `agent/ReferenceExtractor.kt`
- `agent/SystemPromptBuilder.kt`
- `data/repository/` - CVRepository, CVDataLoader
- `di/` - Koin modules

### To `shared/src/iosMain/kotlin/.../`

Move from `composeApp/src/iosMain/`:
- `MainViewController.kt`
- `GroqConfig.ios.kt`
- `HttpEngineFactory.ios.kt`
- `TimeUtils.ios.kt`

### To `android-app/src/main/kotlin/.../`

Move from `composeApp/src/androidMain/`:
- `MainActivity.kt`
- `CVAgentApplication.kt`

Keep in `shared/src/androidMain/`:
- `GroqConfig.android.kt`
- `HttpEngineFactory.android.kt`
- `TimeUtils.android.kt`

### To `android-app/src/androidInstrumentedTest/`

Move from `composeApp/src/androidInstrumentedTest/`:
- `ChatE2ETest.kt`
- `MainApp.kt`
- `RetryRule.kt`

### Deleted after migration

- `composeApp/` - entire directory

## Verification

### Build Order

```bash
./gradlew :shared-design-system:assemble
./gradlew :shared-domain:assemble
./gradlew :shared-ui:assemble
./gradlew :shared:assemble
./gradlew :android-app:assembleDebug
./gradlew ktlintCheck detekt
./gradlew allTests
```

### Runtime Checklist

1. Launch Android app - chat UI loads
2. Send message - API works, response displays
3. iOS build succeeds with single Shared.framework
