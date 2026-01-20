# KMP Project Restructure Design

## Goal
Restructure cv-agent to follow official KMP patterns with layered modules for better separation of concerns.

## Final Project Structure

```
cv-agent/
├── shared/                    # Business logic (no UI dependencies)
│   └── src/
│       ├── commonMain/        # Data models, API client, repositories
│       ├── androidMain/       # Android-specific implementations
│       └── iosMain/           # iOS-specific implementations
│
├── sharedDesignSystem/        # Design tokens (standalone)
│   └── src/
│       └── commonMain/        # Theme, colors, typography, shapes
│
├── shared-ui/                 # UI components (depends on sharedDesignSystem)
│   └── src/
│       └── commonMain/        # Components, screens
│
├── androidApp/                # Android entry point (thin)
│   └── src/
│       └── main/              # MainActivity, Application
│
└── iosApp/                    # iOS entry point (Xcode project)
```

## Module Dependencies

```
        androidApp        iosApp
           │    \        /    │
           │     shared-ui    │
           │         │        │
           │   sharedDesignSystem
           │                  │
           └──── shared ──────┘
```

- `sharedDesignSystem` - standalone, no project dependencies
- `shared` - standalone, no UI dependencies
- `shared-ui` - depends on `sharedDesignSystem`
- `androidApp` / `iosApp` - depend on `shared-ui` + `shared`

## File Migration Plan

### To `shared/` (business logic)

From `composeApp/src/commonMain`:
- `api/` → `shared/src/commonMain/api/`
- `agent/` → `shared/src/commonMain/agent/`
- `data/` → `shared/src/commonMain/data/`
- `di/` → `shared/src/commonMain/di/`
- `GroqConfig.kt` → `shared/src/commonMain/`

From `composeApp/src/androidMain`:
- `GroqConfig.android.kt` → `shared/src/androidMain/`
- `di/HttpEngineFactory.android.kt` → `shared/src/androidMain/di/`

From `composeApp/src/iosMain`:
- `GroqConfig.ios.kt` → `shared/src/iosMain/`
- `di/HttpEngineFactory.ios.kt` → `shared/src/iosMain/di/`

### To `sharedDesignSystem/` (design tokens)

From `composeApp/src/commonMain/ui/theme/`:
- `Color.kt` → `sharedDesignSystem/src/commonMain/theme/`
- `Typography.kt` → `sharedDesignSystem/src/commonMain/theme/`
- `Shape.kt` → `sharedDesignSystem/src/commonMain/theme/`
- `Dimens.kt` → `sharedDesignSystem/src/commonMain/theme/`
- `Theme.kt` → `sharedDesignSystem/src/commonMain/theme/`

### To `shared-ui/` (components & screens)

From `composeApp/src/commonMain/ui/`:
- `components/` → `shared-ui/src/commonMain/ui/components/`
- `ChatScreen.kt` → `shared-ui/src/commonMain/ui/`

### To `androidApp/` (entry point)

From `composeApp/src/androidMain`:
- `MainActivity.kt` → `androidApp/src/main/`
- `CVAgentApplication.kt` → `androidApp/src/main/`

## Gradle Configuration

### shared/build.gradle.kts
Dependencies: ktor, kotlinx-serialization, coroutines, koin-core

### sharedDesignSystem/build.gradle.kts
Dependencies: compose-runtime, compose-foundation, compose-material3

### shared-ui/build.gradle.kts
Dependencies: project(":sharedDesignSystem"), compose-*, lifecycle

### androidApp/build.gradle.kts
Dependencies: project(":shared"), project(":shared-ui")

### settings.gradle.kts
```kotlin
include(":shared")
include(":sharedDesignSystem")
include(":shared-ui")
include(":androidApp")
```

## Verification

### Build Order
```bash
./gradlew :shared:assemble
./gradlew :sharedDesignSystem:assemble
./gradlew :shared-ui:assemble
./gradlew :androidApp:assembleDebug
./gradlew qualityCheck
./gradlew :shared:allTests
```

### Runtime Checks
1. Launch Android app - verify chat UI loads
2. Send a message - verify API communication works
3. Check theme - verify colors/typography applied correctly

### Common Issues
- Update import paths after moving files
- Ensure Koin DI wiring works across modules
- Keep `cv_data.json` in `shared` module
- Update iOS framework exports
