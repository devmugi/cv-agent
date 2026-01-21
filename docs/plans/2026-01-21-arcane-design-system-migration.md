# Arcane Design System Migration

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Migrate cv-agent from `shared-design-system` to Arcane Design System with full component replacement.

**Architecture:** Delete shared-design-system module, update shared-ui to use Arcane libraries, replace Material3 components with Arcane equivalents, implement toast-based error handling.

**Tech Stack:** Arcane Design System (arcane-foundation, arcane-components), Kotlin Multiplatform, Compose Multiplatform

---

## Overview

### Current State
- `shared-design-system` module contains: CVAgentTheme, colors (Color.kt), typography (Typography.kt), shapes (Shape.kt), dimensions (Dimens.kt)
- `shared-ui` uses mostly Material3 directly, only imports ONE color (`ReferenceChipBg`)
- Inline error messages with retry button

### Target State
- No `shared-design-system` module
- `shared-ui` depends on Arcane libraries
- Full Arcane components (ArcaneTextField, ArcaneButton, ArcaneSurface, etc.)
- Toast-based error handling with ArcaneToast

---

## Dependencies

```kotlin
// shared-ui/build.gradle.kts
implementation("io.github.devmugi.design.arcane:arcane-foundation:0.1.1")
implementation("io.github.devmugi.design.arcane:arcane-components:0.1.1")
```

---

## Component Mappings

### File-by-file Migration

| File | Current | Arcane Replacement |
|------|---------|-------------------|
| `ChatScreen.kt` | `Scaffold` | Keep Scaffold, wrap with `ArcaneTheme` |
| `TopBar.kt` | `TopAppBar` | `ArcaneSurface` + custom Row layout |
| `MessageInput.kt` | `TextField`, `IconButton` | `ArcaneTextField`, `ArcaneButton` |
| `MessageBubble.kt` | `Surface` | `ArcaneSurface` (Raised for assistant, Base for user) |
| `ErrorMessage.kt` | `Surface`, `Button` | **Delete** - replace with `ArcaneToast` |
| `ReferenceChip.kt` | `Surface`, custom color | `ArcaneBadge` or `ArcaneSurface` |
| `SuggestionChip.kt` | `Surface` | `ArcaneButton` with Outlined style |
| `WelcomeSection.kt` | `Text` only | `Text` with `ArcaneTheme.typography` |

### Color Mappings

| Material3 | Arcane |
|-----------|--------|
| `colorScheme.primary` | `ArcaneTheme.colors.primary` |
| `colorScheme.surface` | `ArcaneTheme.colors.surface` |
| `colorScheme.onSurface` | `ArcaneTheme.colors.text` |
| `colorScheme.surfaceVariant` | `ArcaneTheme.colors.surfaceRaised` |
| `colorScheme.onSurfaceVariant` | `ArcaneTheme.colors.textSecondary` |
| `colorScheme.error` | `ArcaneTheme.colors.error` |
| `colorScheme.background` | `ArcaneTheme.colors.surface` |
| `ReferenceChipBg` | `ArcaneTheme.colors.surfaceRaised` |

### Typography Mappings

| Material3 | Arcane |
|-----------|--------|
| `titleLarge` | `ArcaneTheme.typography.headlineLarge` |
| `headlineMedium` | `ArcaneTheme.typography.displaySmall` |
| `bodyLarge` | `ArcaneTheme.typography.bodyLarge` |
| `bodyMedium` | `ArcaneTheme.typography.bodyMedium` |
| `labelSmall` | `ArcaneTheme.typography.labelSmall` |
| `labelMedium` | `ArcaneTheme.typography.labelMedium` |

---

## Error Handling Changes

### Before (Inline ErrorMessage)
```kotlin
// In ChatScreen LazyColumn
item {
    ErrorMessage(
        error = state.error,
        onRetry = onRetry
    )
}
```

### After (Toast-based)
```kotlin
// ChatScreen.kt
@Composable
fun ChatScreen(
    state: ChatState,
    toastState: ArcaneToastState,  // New parameter
    onRetry: () -> Unit,
    ...
) {
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            toastState.show(
                message = error.message,
                style = ArcaneToastStyle.Error
            )
        }
    }
    // Floating retry button when error present
}
```

### App-level Setup (MainActivity.kt / MainViewController.kt)
```kotlin
val toastState = rememberArcaneToastState()

ArcaneTheme {
    ArcaneToastHost(state = toastState, position = ToastPosition.BottomCenter) {
        ChatScreen(
            state = state,
            toastState = toastState,
            ...
        )
    }
}
```

---

## Module Structure Changes

### 1. Delete shared-design-system
- Remove from `settings.gradle.kts`
- Delete `/shared-design-system` directory

### 2. Update settings.gradle.kts
```kotlin
// Remove: include(":shared-design-system")
include(":shared-domain")
include(":shared-ui")
include(":shared")
include(":android-app")
```

### 3. Update shared-ui/build.gradle.kts
```kotlin
commonMain.dependencies {
    // Remove: implementation(projects.sharedDesignSystem)

    // Add Arcane:
    implementation("io.github.devmugi.design.arcane:arcane-foundation:0.1.1")
    implementation("io.github.devmugi.design.arcane:arcane-components:0.1.1")

    // Keep existing compose dependencies
    implementation(projects.sharedDomain)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)  // Keep for Scaffold
    implementation(libs.compose.ui)
}
```

### 4. Update shared/build.gradle.kts
```kotlin
sourceSets {
    commonMain.dependencies {
        // Remove: api(projects.sharedDesignSystem)
        api(projects.sharedDomain)
        api(projects.sharedUi)
    }
}

// iOS framework - remove sharedDesignSystem export
listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
        // Remove: export(projects.sharedDesignSystem)
        export(projects.sharedDomain)
        export(projects.sharedUi)
    }
}
```

---

## Files to Modify

### shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/

1. **ChatScreen.kt** - Add toastState parameter, remove ErrorMessage, add LaunchedEffect for errors
2. **components/TopBar.kt** - Replace TopAppBar with ArcaneSurface + Row
3. **components/MessageInput.kt** - Replace TextField with ArcaneTextField, IconButton with ArcaneButton
4. **components/MessageBubble.kt** - Replace Surface with ArcaneSurface
5. **components/ErrorMessage.kt** - **DELETE**
6. **components/ReferenceChip.kt** - Replace Surface with ArcaneSurface, remove ReferenceChipBg import
7. **components/SuggestionChip.kt** - Replace Surface with ArcaneButton (Outlined)
8. **components/WelcomeSection.kt** - Update typography references
9. **components/TooltipProvider.kt** - Update if needed for tooltip styling

### Entry Points

10. **android-app/.../MainActivity.kt** - Wrap with ArcaneTheme + ArcaneToastHost
11. **shared/.../MainViewController.kt** - Wrap with ArcaneTheme + ArcaneToastHost (iOS)

---

## Verification

| Step | Command | Expected |
|------|---------|----------|
| Tests pass | `./gradlew allTests` | BUILD SUCCESSFUL |
| Quality checks | `./gradlew qualityCheck` | No errors |
| Android runs | `./gradlew :android-app:installDebug` | App launches with Arcane theme |
| iOS builds | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` | Framework created |

### Visual Verification
- Background: Dark navy (#181B2E)
- Primary accent: Purple (#8B5CF6)
- Text: White on dark surfaces
- Buttons/inputs: Purple glow effects
- Toasts: Bottom center on errors
