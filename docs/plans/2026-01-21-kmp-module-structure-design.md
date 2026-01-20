# KMP Module Structure Design (Refined)

## Overview

Restructure cv-agent into 4 modules with clear separation of concerns, explicit dependency injection, and single iOS framework export.

## Module Structure

```
cv-agent/
├── shared/                    # Business logic + iOS framework umbrella
│   └── src/
│       ├── commonMain/        # API client, repositories, ViewModels, models
│       ├── commonTest/        # Unit tests for business logic
│       ├── androidMain/       # Android-specific (OkHttp, BuildConfig)
│       └── iosMain/           # iOS-specific (Darwin HTTP client)
│
├── sharedDesignSystem/        # Design tokens (no tests)
│   └── src/
│       └── commonMain/        # Theme, Colors, Typography, Shapes, Dimens
│
├── sharedUi/                  # Pure presentation components
│   └── src/
│       ├── commonMain/        # Components, Screens (receive VMs as params)
│       └── commonTest/        # Compose UI tests
│
├── androidApp/                # Android entry point + DI wiring
│   └── src/
│       ├── main/              # MainActivity, Application, Koin setup
│       └── androidInstrumentedTest/  # E2E tests
│
└── iosApp/                    # iOS entry point (Xcode project)
```

## Dependency Graph

```
           androidApp ──────────────── iosApp
               │                          │
               ├── sharedUi               │ (via Shared.framework)
               │       │                  │
               │   sharedDesignSystem     │
               │                          │
               └────── shared ────────────┘
                   (exports all to iOS)
```

## Key Design Decisions

### 1. Screen-ViewModel Wiring
Screens receive ViewModels as **constructor parameters** (not via `koinViewModel()` inside screens). This keeps `sharedUi` as pure presentation with no business logic dependencies.

```kotlin
// In sharedUi
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
)

// In androidApp
val viewModel: ChatViewModel = koinViewModel()
ChatScreen(viewModel = viewModel)
```

### 2. Naming Convention
All modules use **camelCase**: `shared`, `sharedDesignSystem`, `sharedUi`, `androidApp`.

### 3. Separate Design System Module
`sharedDesignSystem` remains a separate module for:
- Clean architectural boundary
- Future-proofing for additional UI modules
- Clear separation of design tokens from components

### 4. iOS Framework Strategy
Single umbrella framework exported from `shared` module. iOS sees one `Shared.framework` containing all Kotlin code.

### 5. Test Organization
- `shared/src/commonTest/` - business logic tests
- `sharedUi/src/commonTest/` - Compose UI tests
- `androidApp/src/androidInstrumentedTest/` - E2E tests
- `sharedDesignSystem` - no tests (static theme values)

### 6. Resource Location
`cv_data.json` lives in `shared/src/commonMain/resources/`.

## Gradle Configuration

### settings.gradle.kts

```kotlin
rootProject.name = "CVAgent"
include(":shared")
include(":sharedDesignSystem")
include(":sharedUi")
include(":androidApp")
```

### Module Dependencies

| Module | Depends On | Dependency Type |
|--------|-----------|-----------------|
| `sharedDesignSystem` | (none) | — |
| `shared` | `sharedDesignSystem`, `sharedUi` | `api()` (for iOS export) |
| `sharedUi` | `sharedDesignSystem` | `implementation()` |
| `androidApp` | `shared`, `sharedUi` | `implementation()` |

### shared/build.gradle.kts (iOS export)

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":sharedDesignSystem"))
            export(project(":sharedUi"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":sharedDesignSystem"))
            api(project(":sharedUi"))
            // ... other dependencies
        }
    }
}
```

## File Migration Plan

### To `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/`
- `api/` - GroqApiClient, ChatMessage, API models
- `agent/` - ChatViewModel, ChatState, Message
- `data/` - CVData models, CVRepository, CVDataLoader
- `di/` - Koin modules for business logic
- `GroqConfig.kt` - expect declaration

### To `shared/src/androidMain/`
- `GroqConfig.android.kt` - actual (reads BuildConfig)
- `di/HttpEngineFactory.android.kt` - OkHttp engine

### To `shared/src/iosMain/`
- `GroqConfig.ios.kt` - actual (reads from bundle/environment)
- `di/HttpEngineFactory.ios.kt` - Darwin engine

### To `shared/src/commonMain/resources/`
- `cv_data.json`

### To `sharedDesignSystem/src/commonMain/kotlin/.../theme/`
- `Color.kt`
- `Typography.kt`
- `Shape.kt`
- `Dimens.kt`
- `Theme.kt`

### To `sharedUi/src/commonMain/kotlin/.../ui/`
- `components/` - MessageBubble, MessageInput, TopBar, etc.
- `ChatScreen.kt` - takes ChatViewModel as parameter

### To `androidApp/src/main/kotlin/`
- `MainActivity.kt` - wires ViewModel to ChatScreen
- `CVAgentApplication.kt` - initializes Koin with all modules

## Wiring Example

### Koin module in `shared`

```kotlin
val sharedModule = module {
    single { CVRepository(get()) }
    single { GroqApiClient(get()) }
    viewModel { ChatViewModel(get(), get()) }
}
```

### MainActivity in `androidApp`

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

### Koin initialization in `androidApp`

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

## Verification

### Build Order

```bash
# 1. Base modules first
./gradlew :sharedDesignSystem:assemble

# 2. Shared module
./gradlew :shared:assemble

# 3. UI module
./gradlew :sharedUi:assemble

# 4. Android app
./gradlew :androidApp:assembleDebug

# 5. Quality checks
./gradlew ktlintCheck detekt

# 6. Tests
./gradlew :shared:allTests
./gradlew :sharedUi:allTests
./gradlew :androidApp:connectedAndroidTest
```

### Runtime Checklist

1. Launch Android app - chat UI loads with theme applied
2. Send message - API call succeeds, response displays
3. Verify suggested questions - chips render and respond to taps
4. Check markdown - formatting renders correctly in responses

### Migration Pitfalls

- Update all import paths after moving files
- Ensure `cv_data.json` resource path is correct in `CVDataLoader`
- Verify Koin modules are properly composed in app initialization
- Update iOS Xcode project to reference new `Shared.framework` location
