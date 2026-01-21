# Arcane Design System Migration - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Migrate cv-agent from shared-design-system to Arcane Design System with full component replacement.

**Architecture:** Delete shared-design-system module, add Arcane dependencies to shared-ui, replace Material3 components with Arcane equivalents, implement toast-based error handling.

**Tech Stack:** Arcane Design System (arcane-foundation 0.1.1, arcane-components 0.1.1), Kotlin Multiplatform, Compose Multiplatform

---

## Task 1: Update build configuration

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `shared-ui/build.gradle.kts`
- Modify: `shared/build.gradle.kts`

**Step 1: Remove shared-design-system from settings.gradle.kts**

```kotlin
// settings.gradle.kts - Remove this line:
// include(":shared-design-system")
```

**Step 2: Update shared-ui/build.gradle.kts**

Remove sharedDesignSystem dependency and add Arcane:

```kotlin
commonMain.dependencies {
    // Remove: implementation(projects.sharedDesignSystem)

    // Add Arcane dependencies:
    implementation("io.github.devmugi.design.arcane:arcane-foundation:0.1.1")
    implementation("io.github.devmugi.design.arcane:arcane-components:0.1.1")

    // Keep all other dependencies unchanged
}
```

**Step 3: Update shared/build.gradle.kts**

Remove sharedDesignSystem from commonMain and iOS exports:

```kotlin
sourceSets {
    commonMain.dependencies {
        // Remove: api(projects.sharedDesignSystem)
        api(projects.sharedDomain)
        api(projects.sharedUi)
        // ... rest unchanged
    }
}

// In iOS framework config, remove:
// export(projects.sharedDesignSystem)
```

**Step 4: Verify build compiles**

Run: `./gradlew :shared-ui:compileKotlinIosSimulatorArm64 --quiet`
Expected: BUILD SUCCESSFUL (with unresolved reference errors for design system imports - that's OK)

**Step 5: Commit**

```bash
git add settings.gradle.kts shared-ui/build.gradle.kts shared/build.gradle.kts
git commit -m "build: add Arcane dependencies, remove shared-design-system"
```

---

## Task 2: Delete shared-design-system module

**Files:**
- Delete: `shared-design-system/` (entire directory)

**Step 1: Delete the module directory**

```bash
rm -rf shared-design-system
```

**Step 2: Verify deletion**

```bash
ls shared-design-system 2>&1 | grep "No such file"
```

**Step 3: Commit**

```bash
git add -A
git commit -m "build: delete shared-design-system module"
```

---

## Task 3: Migrate WelcomeSection.kt

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/WelcomeSection.kt`

**Step 1: Update imports and theme references**

Replace MaterialTheme with ArcaneTheme:

```kotlin
// Remove:
// import androidx.compose.material3.MaterialTheme

// Add:
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

// Replace in code:
// MaterialTheme.colorScheme.onSurface -> ArcaneTheme.colors.text
// MaterialTheme.colorScheme.onSurfaceVariant -> ArcaneTheme.colors.textSecondary
// MaterialTheme.typography.headlineMedium -> ArcaneTheme.typography.displaySmall
// MaterialTheme.typography.bodyLarge -> ArcaneTheme.typography.bodyLarge
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-ui:compileKotlinIosSimulatorArm64 --quiet`
Expected: Compiles (may have other errors from unmigrated files)

**Step 3: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/WelcomeSection.kt
git commit -m "refactor: migrate WelcomeSection to Arcane theme"
```

---

## Task 4: Migrate SuggestionChip.kt

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChip.kt`

**Step 1: Replace Surface with ArcaneButton**

```kotlin
// Remove:
// import androidx.compose.material3.MaterialTheme
// import androidx.compose.material3.Surface

// Add:
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.arcane.design.components.controls.ArcaneButton
import io.github.devmugi.arcane.design.components.controls.ArcaneButtonStyle

// Replace Surface with ArcaneButton(style = ArcaneButtonStyle.Outlined)
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-ui:compileKotlinIosSimulatorArm64 --quiet`

**Step 3: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChip.kt
git commit -m "refactor: migrate SuggestionChip to ArcaneButton"
```

---

## Task 5: Migrate ReferenceChip.kt

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ReferenceChip.kt`

**Step 1: Replace imports and Surface with ArcaneSurface**

```kotlin
// Remove:
// import io.github.devmugi.cv.agent.designsystem.theme.ReferenceChipBg
// import androidx.compose.material3.MaterialTheme
// import androidx.compose.material3.Surface

// Add:
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant

// Replace:
// color = ReferenceChipBg -> use ArcaneSurface with SurfaceVariant.Raised
// MaterialTheme.shapes.small -> ArcaneRadius.Small
// MaterialTheme.typography.labelSmall -> ArcaneTheme.typography.labelSmall
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-ui:compileKotlinIosSimulatorArm64 --quiet`

**Step 3: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ReferenceChip.kt
git commit -m "refactor: migrate ReferenceChip to ArcaneSurface"
```

---

## Task 6: Migrate MessageBubble.kt

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageBubble.kt`

**Step 1: Replace Surface with ArcaneSurface**

```kotlin
// Remove:
// import androidx.compose.material3.MaterialTheme
// import androidx.compose.material3.Surface

// Add:
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant

// For user messages: SurfaceVariant.Base
// For assistant messages: SurfaceVariant.Raised
// Replace color references with ArcaneTheme.colors equivalents
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-ui:compileKotlinIosSimulatorArm64 --quiet`

**Step 3: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageBubble.kt
git commit -m "refactor: migrate MessageBubble to ArcaneSurface"
```

---

## Task 7: Migrate MessageInput.kt

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageInput.kt`

**Step 1: Replace TextField and IconButton with Arcane components**

```kotlin
// Remove:
// import androidx.compose.material3.TextField
// import androidx.compose.material3.TextFieldDefaults
// import androidx.compose.material3.IconButton
// import androidx.compose.material3.IconButtonDefaults
// import androidx.compose.material3.MaterialTheme

// Add:
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.arcane.design.components.controls.ArcaneTextField
import io.github.devmugi.arcane.design.components.controls.ArcaneButton
import io.github.devmugi.arcane.design.components.controls.ArcaneButtonStyle

// Replace TextField with ArcaneTextField
// Replace IconButton with ArcaneButton (icon content)
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-ui:compileKotlinIosSimulatorArm64 --quiet`

**Step 3: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageInput.kt
git commit -m "refactor: migrate MessageInput to ArcaneTextField and ArcaneButton"
```

---

## Task 8: Migrate TopBar.kt

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/TopBar.kt`

**Step 1: Replace TopAppBar with ArcaneSurface + Row**

```kotlin
// Remove:
// import androidx.compose.material3.TopAppBar
// import androidx.compose.material3.TopAppBarDefaults
// import androidx.compose.material3.MaterialTheme

// Add:
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant

// Replace TopAppBar with ArcaneSurface containing Row with title Text
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-ui:compileKotlinIosSimulatorArm64 --quiet`

**Step 3: Commit**

```bash
git add shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/TopBar.kt
git commit -m "refactor: migrate TopBar to ArcaneSurface"
```

---

## Task 9: Delete ErrorMessage.kt and update ChatScreen

**Files:**
- Delete: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ErrorMessage.kt`
- Modify: `shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`

**Step 1: Delete ErrorMessage.kt**

```bash
rm shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ErrorMessage.kt
```

**Step 2: Update ChatScreen.kt**

Add toastState parameter, wrap with ArcaneTheme, add LaunchedEffect for errors:

```kotlin
// Add imports:
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastState
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastStyle

// Add parameter:
@Composable
fun ChatScreen(
    state: ChatState,
    toastState: ArcaneToastState,  // NEW
    onSendMessage: (String) -> Unit,
    ...
)

// Add LaunchedEffect for error handling:
LaunchedEffect(state.error) {
    state.error?.let { error ->
        toastState.show(
            message = when (error) {
                is ChatError.Network -> "Network error: ${error.reason}"
                is ChatError.Api -> error.message
                is ChatError.RateLimit -> "Rate limit exceeded"
            },
            style = ArcaneToastStyle.Error
        )
    }
}

// Remove ErrorMessage from LazyColumn items
```

**Step 3: Verify compilation**

Run: `./gradlew :shared-ui:compileKotlinIosSimulatorArm64 --quiet`

**Step 4: Commit**

```bash
git add -A
git commit -m "refactor: replace ErrorMessage with ArcaneToast in ChatScreen"
```

---

## Task 10: Update MainActivity.kt (Android)

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

**Step 1: Wrap with ArcaneTheme and ArcaneToastHost**

```kotlin
// Add imports:
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastHost
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.components.feedback.ToastPosition

// In setContent:
setContent {
    val toastState = rememberArcaneToastState()

    ArcaneTheme {
        ArcaneToastHost(
            state = toastState,
            position = ToastPosition.BottomCenter
        ) {
            // ... existing ChatScreen call with toastState parameter
            ChatScreen(
                state = state,
                toastState = toastState,
                ...
            )
        }
    }
}
```

**Step 2: Verify Android build**

Run: `./gradlew :android-app:assembleDebug --quiet`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt
git commit -m "feat: integrate ArcaneTheme and ArcaneToastHost in MainActivity"
```

---

## Task 11: Update MainViewController.kt (iOS)

**Files:**
- Modify: `shared/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt`

**Step 1: Wrap with ArcaneTheme and ArcaneToastHost**

```kotlin
// Add imports:
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.arcane.design.components.feedback.ArcaneToastHost
import io.github.devmugi.arcane.design.components.feedback.rememberArcaneToastState
import io.github.devmugi.arcane.design.components.feedback.ToastPosition

// Update MainViewController composable with same pattern as Android
```

**Step 2: Verify iOS build**

Run: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --quiet`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt
git commit -m "feat: integrate ArcaneTheme and ArcaneToastHost in iOS MainViewController"
```

---

## Task 12: Update tests and fix any remaining issues

**Files:**
- Modify: `shared-ui/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/*.kt`
- Delete: `shared-ui/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/ErrorMessageTest.kt`

**Step 1: Delete ErrorMessageTest.kt**

```bash
rm shared-ui/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/ErrorMessageTest.kt
```

**Step 2: Update other tests if they reference removed components**

Check and fix any test imports or references to old design system.

**Step 3: Run all tests**

Run: `./gradlew allTests`
Expected: BUILD SUCCESSFUL

**Step 4: Run quality checks**

Run: `./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add -A
git commit -m "test: update tests for Arcane migration"
```

---

## Task 13: Runtime verification

**Step 1: Install and run Android app**

Run: `./gradlew :android-app:installDebug`
Launch app and verify:
- Dark navy background (#181B2E)
- Purple accent (#8B5CF6)
- Messages display correctly
- Input field works
- Toast appears on error

**Step 2: Build iOS framework**

Run: `./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64`
Expected: BUILD SUCCESSFUL

**Step 3: Final commit if any fixes needed**

```bash
git add -A
git commit -m "fix: runtime verification fixes for Arcane migration"
```

---

## Verification Summary

| Check | Command | Expected |
|-------|---------|----------|
| Tests pass | `./gradlew allTests` | BUILD SUCCESSFUL |
| Quality checks | `./gradlew qualityCheck` | No errors |
| Android runs | `./gradlew :android-app:installDebug` | App launches |
| iOS builds | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` | Framework created |
