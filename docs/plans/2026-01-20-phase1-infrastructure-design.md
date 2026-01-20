# Phase 1: Project Setup & Infrastructure - Design Document

**Project:** CV Agent
**Phase:** 1 - Project Setup & Infrastructure
**Date:** 2026-01-20
**Author:** Denys Honcharenko (with Claude Code)

---

## Overview

Phase 1 establishes the foundation for the CV Agent KMP application. This includes dependency management, package structure, design system, quality tools, and a basic working app that can build and run on both Android and iOS.

## Goals

- Buildable KMP project with quality gates configured
- Complete Material3 design system with portfolio branding
- Local quality checks (Detekt, Ktlint) passing
- Basic app launches with themed placeholder screen on both platforms

---

## 1. Current Project State

**Template Used:** Modern KMP Compose Multiplatform template

**Existing Configuration:**
- **Kotlin:** 2.3.0
- **Compose Multiplatform:** 1.10.0
- **Min SDK:** 24 (Android 7.0+)
- **Target SDK:** 36
- **iOS Targets:** iosArm64, iosSimulatorArm64
- **Module Structure:** Single `composeApp` module (combines shared + androidApp)
- **Dependency Management:** Version catalog (`libs.versions.toml`)

---

## 2. Dependencies to Add

### 2.1 Networking & Serialization

```toml
[versions]
ktor = "3.0.3"
kotlinx-serialization = "1.7.3"
kotlinx-coroutines = "1.10.2"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }

[plugins]
kotlinx-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### 2.2 Markdown Rendering

```toml
[versions]
multiplatform-markdown = "0.29.0"

[libraries]
multiplatform-markdown-renderer = { module = "com.mikepenz:multiplatform-markdown-renderer", version.ref = "multiplatform-markdown" }
```

### 2.3 Testing (for future phases)

```toml
[versions]
mockk = "1.13.14"
turbine = "1.2.0"

[libraries]
mockk-common = { module = "io.mockk:mockk-common", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
```

### 2.4 Quality Tools

```toml
[versions]
detekt = "1.23.7"
ktlint-gradle = "12.1.2"

[plugins]
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint-gradle" }
```

---

## 3. Package Structure

All code in `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/`:

```
io.github.devmugi.cv.agent/
├── data/               # Phase 2: CV data models
│   ├── models/        # CVData, PersonalInfo, etc.
│   └── repository/    # CVRepository, CVDataLoader
├── api/                # Phase 3: Groq API client
│   ├── GroqApiClient.kt
│   └── models/        # ChatMessage, API request/response
├── agent/              # Phase 3: Chat logic & state
│   ├── ChatViewModel.kt
│   └── ChatState.kt
├── ui/                 # Phase 4: Compose screens
│   ├── ChatScreen.kt
│   ├── components/    # Reusable UI components
│   │   ├── MessageBubble.kt
│   │   ├── MessageInput.kt
│   │   ├── SuggestedQuestionChip.kt
│   │   └── ReferenceChip.kt
│   └── theme/         # Phase 1: Design system
│       ├── Theme.kt
│       ├── Color.kt
│       ├── Typography.kt
│       └── Dimens.kt
└── util/               # Utilities as needed
```

**Phase 1 Creation:**
- Create all package directories (empty, except `ui/theme/`)
- Implement complete design system in `ui/theme/`

---

## 4. Design System Specification

### 4.1 Colors

**File:** `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Color.kt`

```kotlin
package io.github.devmugi.cv.agent.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Colors (Portfolio Theme)
val DarkNavy = Color(0xFF1A1D2E)
val DarkBlue = Color(0xFF16213E)
val DarkSurface = Color(0xFF1E2746)
val Gold = Color(0xFFF5A623)
val GoldBright = Color(0xFFFFC947)
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFB0B3C1)

// Material3 Dark Color Scheme
val CVAgentColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = DarkNavy,
    primaryContainer = Gold.copy(alpha = 0.12f),
    onPrimaryContainer = GoldBright,

    secondary = LightGray,
    onSecondary = DarkNavy,

    background = DarkNavy,
    onBackground = White,

    surface = DarkSurface,
    onSurface = White,

    surfaceVariant = DarkBlue,
    onSurfaceVariant = LightGray,

    error = Color(0xFFCF6679),
    onError = Color(0xFF000000)
)
```

### 4.2 Typography

**File:** `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Typography.kt`

```kotlin
package io.github.devmugi.cv.agent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CVAgentTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 64.sp
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp
    )
)
```

### 4.3 Shapes

**File:** `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Shape.kt`

```kotlin
package io.github.devmugi.cv.agent.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CVAgentShapes = Shapes(
    small = RoundedCornerShape(8.dp),      // Chips
    medium = RoundedCornerShape(16.dp),    // Cards, message bubbles
    large = RoundedCornerShape(24.dp)      // Dialogs, sheets
)
```

### 4.4 Spacing

**File:** `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Dimens.kt`

```kotlin
package io.github.devmugi.cv.agent.ui.theme

import androidx.compose.ui.unit.dp

object Dimens {
    val spaceXSmall = 4.dp
    val spaceSmall = 8.dp
    val spaceMedium = 16.dp
    val spaceLarge = 24.dp
    val spaceXLarge = 32.dp
}
```

### 4.5 Theme Composable

**File:** `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Theme.kt`

```kotlin
package io.github.devmugi.cv.agent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun CVAgentTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CVAgentColorScheme,
        typography = CVAgentTypography,
        shapes = CVAgentShapes,
        content = content
    )
}
```

---

## 5. Quality Tools Configuration

### 5.1 Detekt

**File:** `detekt.yml` (root directory)

```yaml
build:
  maxIssues: 0
  excludeCorrectable: false
  weights:
    complexity: 2
    LongParameterList: 1
    style: 1
    comments: 0

config:
  validation: true
  warningsAsErrors: true

complexity:
  active: true
  CyclomaticComplexMethod:
    active: true
    threshold: 15
  LongMethod:
    active: true
    threshold: 60
  LongParameterList:
    active: true
    functionThreshold: 6
    constructorThreshold: 7
  NestedBlockDepth:
    active: true
    threshold: 4

naming:
  active: true
  FunctionNaming:
    active: true
    excludes: ['**/ui/**'] # Allow PascalCase for Composables
  PackageNaming:
    active: true

style:
  active: true
  MaxLineLength:
    active: true
    maxLineLength: 120
    excludeCommentStatements: true
```

**Gradle Configuration:**

Add to `build.gradle.kts` (root):

```kotlin
plugins {
    alias(libs.plugins.detekt)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    baseline = file("$projectDir/detekt-baseline.xml")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}
```

### 5.2 Ktlint

**Gradle Configuration:**

Add to `build.gradle.kts` (root):

```kotlin
plugins {
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set("1.5.0")
    android.set(true)
    verbose.set(true)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
```

### 5.3 Gradle Tasks

**Custom task in root `build.gradle.kts`:**

```kotlin
tasks.register("qualityCheck") {
    dependsOn("ktlintCheck", "detekt")
    group = "verification"
    description = "Run all quality checks (Ktlint + Detekt)"
}
```

**Usage:**
```bash
./gradlew ktlintCheck      # Check formatting
./gradlew ktlintFormat     # Auto-fix formatting
./gradlew detekt           # Static analysis
./gradlew qualityCheck     # Run both
```

---

## 6. Placeholder App Implementation

### 6.1 Main Screen

**File:** `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`

```kotlin
package io.github.devmugi.cv.agent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.devmugi.cv.agent.ui.theme.CVAgentTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    CVAgentTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "<DH/> CV Agent",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CV Agent - Coming Soon",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

### 6.2 Android Entry Point

**File:** `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

```kotlin
package io.github.devmugi.cv.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.devmugi.cv.agent.ui.ChatScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatScreen()
        }
    }
}
```

### 6.3 iOS Entry Point

**File:** `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt`

```kotlin
package io.github.devmugi.cv.agent

import androidx.compose.ui.window.ComposeUIViewController
import io.github.devmugi.cv.agent.ui.ChatScreen

fun MainViewController() = ComposeUIViewController {
    ChatScreen()
}
```

**iOS Swift bridge (already exists in `iosApp/iosApp/ContentView.swift`):**

Update to call `MainViewController()`:

```swift
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
```

### 6.4 Resources Setup

**Create directory structure:**
```
composeApp/src/commonMain/composeResources/
└── files/
    └── cv_data.json  # Placeholder: {}
```

**File:** `composeApp/src/commonMain/composeResources/files/cv_data.json`

```json
{
  "placeholder": "CV data will be added in Phase 2"
}
```

---

## 7. Build Configuration Updates

### 7.1 Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}

ktlint {
    version.set("1.5.0")
    android.set(true)
    verbose.set(true)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

tasks.register("qualityCheck") {
    dependsOn("ktlintCheck", "detekt")
    group = "verification"
    description = "Run all quality checks"
}
```

### 7.2 `composeApp/build.gradle.kts`

Add to plugins:
```kotlin
plugins {
    // ... existing plugins
    alias(libs.plugins.kotlinx.serialization)
}
```

Add to `commonMain.dependencies`:
```kotlin
commonMain.dependencies {
    // ... existing dependencies

    // Networking
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Markdown
    implementation(libs.multiplatform.markdown.renderer)
}
```

Add platform-specific Ktor engines:
```kotlin
androidMain.dependencies {
    // ... existing dependencies
    implementation(libs.ktor.client.okhttp)
}

iosMain.dependencies {
    implementation(libs.ktor.client.darwin)
}
```

---

## 8. Acceptance Criteria

After completing Phase 1 implementation, verify:

### 8.1 Build Success
- [ ] `./gradlew :composeApp:assembleDebug` succeeds (Android APK created)
- [ ] `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` succeeds (iOS framework built)

### 8.2 Quality Gates
- [ ] `./gradlew ktlintCheck` passes with no violations
- [ ] `./gradlew detekt` passes with no issues
- [ ] `./gradlew qualityCheck` passes (both tools green)

### 8.3 App Launch
- [ ] Android app launches on emulator/device
- [ ] Shows "CV Agent - Coming Soon" text in gold color
- [ ] TopBar displays "<DH/> CV Agent" with correct styling
- [ ] Background is dark navy color
- [ ] iOS app launches on simulator
- [ ] Shows identical UI to Android

### 8.4 Structure
- [ ] All package directories created under `io.github.devmugi.cv.agent`
- [ ] Design system files exist and compile: `Theme.kt`, `Color.kt`, `Typography.kt`, `Dimens.kt`
- [ ] `ChatScreen.kt` exists and compiles
- [ ] Resources directory created with placeholder `cv_data.json`

### 8.5 Configuration
- [ ] `libs.versions.toml` updated with all Phase 1 dependencies
- [ ] `detekt.yml` exists and configured
- [ ] Ktlint plugin configured in root `build.gradle.kts`
- [ ] All dependencies resolve successfully

---

## 9. CI/CD (Deferred)

**Decision:** Skip GitHub Actions setup in Phase 1. iOS tooling complexity and signing make automated CI premature.

**Local workflow:**
```bash
# Before commit
./gradlew ktlintFormat       # Fix formatting
./gradlew qualityCheck       # Verify quality gates

# Build verification
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

**Future:** Add GitHub Actions workflow in later phases when:
- API integration is testable
- Unit tests exist to run
- iOS signing is configured

---

## 10. Implementation Order

Recommended sequence for implementing Phase 1:

1. **Update `libs.versions.toml`** with all dependencies
2. **Update root `build.gradle.kts`** with Detekt and Ktlint plugins
3. **Update `composeApp/build.gradle.kts`** with serialization plugin and dependencies
4. **Create `detekt.yml`** configuration file
5. **Create package structure** (empty directories)
6. **Implement design system:**
   - `Color.kt`
   - `Typography.kt`
   - `Shape.kt`
   - `Dimens.kt`
   - `Theme.kt`
7. **Create `ChatScreen.kt`** placeholder
8. **Update Android `MainActivity`**
9. **Update iOS `MainViewController.kt`** and Swift bridge
10. **Create resources directory** with placeholder `cv_data.json`
11. **Run quality checks:**
    - `./gradlew ktlintFormat` (fix any issues)
    - `./gradlew detekt` (fix any issues)
12. **Build and test:**
    - Android APK
    - iOS framework
    - Launch on both platforms

---

## 11. Next Steps After Phase 1

Once Phase 1 acceptance criteria are met:

**Phase 2:** Data Layer & CV Models
- Implement all data models in `data/models/`
- Populate `cv_data.json` with complete CV data
- Create `CVDataLoader` and `CVRepository`
- Write unit tests for data layer

**Estimated Effort:** Phase 1 should take 2-4 hours for implementation and verification.

---

## Document Metadata

| Version | Date | Status | Author |
|---------|------|--------|--------|
| 1.0 | 2026-01-20 | Approved | Denys Honcharenko |

---

**End of Design Document**
