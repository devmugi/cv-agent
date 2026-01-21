# KMP Module Structure Design v2

## Overview

Restructure cv-agent into 4 modules with clear separation of concerns, explicit dependency injection, and single iOS framework export.

**Key change from v1:** Module names follow Gradle's official recommendation of **kebab-case** (lowercase with hyphens).

## Module Structure

```
cv-agent/
├── shared/                      # Business logic + iOS framework umbrella
│   └── src/
│       ├── commonMain/          # API client, repositories, ViewModels, models
│       ├── commonTest/          # Unit tests for business logic
│       ├── androidMain/         # Android-specific (OkHttp, BuildConfig)
│       └── iosMain/             # iOS-specific (Darwin HTTP client)
│
├── shared-design-system/        # Design tokens (no tests)
│   └── src/
│       └── commonMain/          # Theme, Colors, Typography, Shapes, Dimens
│
├── shared-ui/                   # Pure presentation components
│   └── src/
│       ├── commonMain/          # Components, Screens (receive VMs as params)
│       └── commonTest/          # Compose UI tests
│
├── android-app/                 # Android entry point + DI wiring
│   └── src/
│       ├── main/                # MainActivity, Application, Koin setup
│       └── androidInstrumentedTest/  # E2E tests
│
└── iosApp/                      # iOS entry point (Xcode project, not Gradle)
```

## Dependency Graph

```
           android-app ─────────────────── iosApp
               │                              │
               ├── shared-ui                  │ (via Shared.framework)
               │       │                      │
               │   shared-design-system       │
               │                              │
               └────── shared ────────────────┘
                   (exports all to iOS)
```

## Module Dependencies

| Module | Depends On | Dependency Type |
|--------|-----------|-----------------|
| `shared-design-system` | (none) | — |
| `shared-ui` | `shared-design-system` | `implementation()` |
| `shared` | `shared-design-system`, `shared-ui` | `api()` (for iOS export) |
| `android-app` | `shared`, `shared-ui` | `implementation()` |

## Key Design Decisions

### 1. Kebab-case Module Names

Following [Gradle's official recommendation](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html):

> "Use lower case hyphenation for all project names: all letters are lowercase, and words are separated with a dash (-) character."

### 2. Screen-ViewModel Wiring

Screens receive ViewModels as **constructor parameters** (not via `koinViewModel()` inside screens). This keeps `shared-ui` as pure presentation with no business logic dependencies.

```kotlin
// In shared-ui
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
)

// In android-app
val viewModel: ChatViewModel = koinViewModel()
ChatScreen(viewModel = viewModel)
```

### 3. Separate Design System Module

`shared-design-system` remains a separate module for:
- Clean architectural boundary
- Future-proofing for additional UI modules
- Clear separation of design tokens from components

### 4. iOS Framework Strategy

Single umbrella framework exported from `shared` module. iOS sees one `Shared.framework` containing all Kotlin code.

### 5. Test Organization

- `shared/src/commonTest/` - business logic tests
- `shared-ui/src/commonTest/` - Compose UI tests
- `android-app/src/androidInstrumentedTest/` - E2E tests
- `shared-design-system` - no tests (static theme values)

### 6. Resource Location

`cv_data.json` lives in `shared/src/commonMain/resources/`.

## Gradle Configuration

### settings.gradle.kts

```kotlin
rootProject.name = "cv-agent"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":shared")
include(":shared-design-system")
include(":shared-ui")
include(":android-app")
```

### Typesafe Project Accessors

With kebab-case names, Gradle generates camelCase accessors:

```kotlin
// In shared/build.gradle.kts
dependencies {
    api(projects.sharedDesignSystem)  // :shared-design-system
    api(projects.sharedUi)            // :shared-ui
}

// In android-app/build.gradle.kts
dependencies {
    implementation(projects.shared)
    implementation(projects.sharedUi)
}
```

### shared/build.gradle.kts (iOS export)

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(projects.sharedDesignSystem)
            export(projects.sharedUi)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.sharedDesignSystem)
            api(projects.sharedUi)
            // ... other dependencies
        }
    }
}
```

## File Migration Plan

### To `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/`
- `api/` - GroqApiClient, ChatMessage, API models
- `agent/` - ChatViewModel, ChatState, Message, ReferenceExtractor, SystemPromptBuilder
- `data/` - CVData models, CVRepository, CVDataLoader
- `di/` - Koin modules (AppModule, HttpEngineFactory)
- `GroqConfig.kt` - expect declaration

### To `shared/src/androidMain/` and `iosMain/`
- Platform-specific `actual` implementations (GroqConfig, HttpEngineFactory, TimeUtils)

### To `shared/src/commonMain/resources/`
- `cv_data.json`

### To `shared-design-system/src/commonMain/kotlin/.../theme/`
- `Color.kt`
- `Typography.kt`
- `Shape.kt`
- `Dimens.kt`
- `Theme.kt`

### To `shared-ui/src/commonMain/kotlin/.../ui/`
- `components/` - MessageBubble, MessageInput, TopBar, etc.
- `ChatScreen.kt` - takes ChatViewModel as parameter

### To `android-app/src/main/kotlin/`
- `MainActivity.kt` - wires ViewModel to ChatScreen
- `CVAgentApplication.kt` - initializes Koin

### To `android-app/src/androidInstrumentedTest/`
- E2E tests (ChatE2ETest, RetryRule, etc.)

### Deleted after migration
- `composeApp/` - entire directory removed once migration verified

## Wiring Example

### Koin module in `shared`

```kotlin
val sharedModule = module {
    single { CVRepository(get()) }
    single { GroqApiClient(get()) }
    viewModel { ChatViewModel(get(), get()) }
}
```

### MainActivity in `android-app`

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CVAgentTheme {
                val viewModel: ChatViewModel = koinViewModel()
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}
```

### Koin initialization in `android-app`

```kotlin
class CVAgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CVAgentApplication)
            modules(sharedModule)
        }
    }
}
```

## Implementation Steps

### 1. Rename existing modules

```bash
mv sharedDesignSystem shared-design-system
mv sharedUi shared-ui
```

### 2. Update settings.gradle.kts

Change includes to kebab-case names.

### 3. Update all build.gradle.kts files

Fix project references to use new names and typesafe accessors.

### 4. Create `android-app` from `composeApp`

- Keep only: `MainActivity`, `CVAgentApplication`, AndroidManifest, E2E tests
- Remove duplicated code (already in `shared` and `shared-ui`)

### 5. Update imports in Kotlin files

Any hardcoded module references.

### 6. Delete `composeApp`

After verifying everything works.

## Verification

### Build Order

```bash
# 1. Base modules first
./gradlew :shared-design-system:assemble

# 2. UI module
./gradlew :shared-ui:assemble

# 3. Shared module (umbrella)
./gradlew :shared:assemble

# 4. Android app
./gradlew :android-app:assembleDebug

# 5. Quality checks
./gradlew ktlintCheck detekt

# 6. Tests
./gradlew :shared:allTests
./gradlew :shared-ui:allTests
./gradlew :android-app:connectedAndroidTest
```

### Runtime Checklist

1. Launch Android app - chat UI loads with theme applied
2. Send message - API call succeeds, response displays
3. Verify suggested questions - chips render and respond to taps
4. Check markdown - formatting renders correctly in responses

## Migration from v1

If work was started with v1 (camelCase naming):

| v1 Name | v2 Name |
|---------|---------|
| `sharedDesignSystem` | `shared-design-system` |
| `sharedUi` | `shared-ui` |
| `androidApp` | `android-app` |

Rename directories and update all references in `settings.gradle.kts` and `build.gradle.kts` files.
