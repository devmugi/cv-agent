# Phase 1: Project Setup & Infrastructure - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Establish buildable KMP project foundation with quality gates, design system, and placeholder app

**Architecture:** Single composeApp module with shared Kotlin/Compose code, Material3 dark theme with portfolio branding, Detekt/Ktlint quality gates

**Tech Stack:** Kotlin 2.3.0, Compose Multiplatform 1.10.0, Ktor 3.0+, Material3, Detekt, Ktlint

---

## Task 1: Update Version Catalog with Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`

**Step 1: Add Ktor and serialization versions**

Open `gradle/libs.versions.toml` and add these versions after existing versions:

```toml
ktor = "3.0.3"
kotlinx-serialization = "1.7.3"
kotlinx-coroutines = "1.10.2"
multiplatform-markdown = "0.29.0"
mockk = "1.13.14"
turbine = "1.2.0"
detekt = "1.23.7"
ktlint-gradle = "12.1.2"
```

**Step 2: Add library dependencies**

In the `[libraries]` section, add:

```toml
# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }

# Serialization
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# Coroutines
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }

# Markdown
multiplatform-markdown-renderer = { module = "com.mikepenz:multiplatform-markdown-renderer", version.ref = "multiplatform-markdown" }

# Testing
mockk-common = { module = "io.mockk:mockk-common", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
```

**Step 3: Add plugin dependencies**

In the `[plugins]` section, add:

```toml
kotlinx-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint-gradle" }
```

**Step 4: Verify syntax**

Run: `./gradlew projects`
Expected: SUCCESS (no TOML syntax errors)

**Step 5: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "Add dependencies for Phase 1

Add Ktor, Kotlinx Serialization, Coroutines, Markdown renderer, and quality tools (Detekt, Ktlint) to version catalog.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Configure Detekt Quality Tool

**Files:**
- Create: `detekt.yml`
- Modify: `build.gradle.kts`

**Step 1: Create detekt.yml configuration**

Create `detekt.yml` in project root:

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
    excludes: ['**/ui/**']
  PackageNaming:
    active: true

style:
  active: true
  MaxLineLength:
    active: true
    maxLineLength: 120
    excludeCommentStatements: true
```

**Step 2: Update root build.gradle.kts with Detekt**

Add Detekt plugin and configuration to `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.detekt)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}
```

**Step 3: Verify Detekt configuration**

Run: `./gradlew detekt`
Expected: SUCCESS (may have warnings on template code, that's OK)

**Step 4: Commit**

```bash
git add detekt.yml build.gradle.kts
git commit -m "Configure Detekt for code quality

Add detekt.yml with complexity, naming, and style rules. Configure Detekt plugin in root build file.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Configure Ktlint Quality Tool

**Files:**
- Modify: `build.gradle.kts`

**Step 1: Add Ktlint plugin and configuration**

Update `build.gradle.kts` to add Ktlint:

```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
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
```

**Step 2: Add qualityCheck task**

Add this task at the end of `build.gradle.kts`:

```kotlin
tasks.register("qualityCheck") {
    dependsOn("ktlintCheck", "detekt")
    group = "verification"
    description = "Run all quality checks (Ktlint + Detekt)"
}
```

**Step 3: Verify Ktlint configuration**

Run: `./gradlew ktlintCheck`
Expected: SUCCESS or FAILED (may fail on template code, will fix later)

**Step 4: Commit**

```bash
git add build.gradle.kts
git commit -m "Configure Ktlint for code formatting

Add Ktlint plugin with Android style, exclusions for generated code, and qualityCheck task.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Update composeApp Build Configuration

**Files:**
- Modify: `composeApp/build.gradle.kts`

**Step 1: Add serialization plugin**

In `composeApp/build.gradle.kts`, update plugins block:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
}
```

**Step 2: Add common dependencies**

Update `commonMain.dependencies`:

```kotlin
commonMain.dependencies {
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.components.resources)
    implementation(compose.preview)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)

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

**Step 3: Add platform-specific Ktor engines**

Update `androidMain.dependencies`:

```kotlin
androidMain.dependencies {
    implementation(compose.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ktor.client.okhttp)
}
```

Add after `androidMain` section:

```kotlin
iosMain.dependencies {
    implementation(libs.ktor.client.darwin)
}
```

**Step 4: Sync and verify dependencies**

Run: `./gradlew composeApp:dependencies`
Expected: All dependencies resolve successfully

**Step 5: Commit**

```bash
git add composeApp/build.gradle.kts
git commit -m "Add Phase 1 dependencies to composeApp

Add Ktor, Kotlinx Serialization, Coroutines, and Markdown renderer with platform-specific engines.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Create Package Structure

**Files:**
- Create directories under `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/`

**Step 1: Create empty package directories**

Run:
```bash
mkdir -p composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models
mkdir -p composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository
mkdir -p composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/models
mkdir -p composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent
mkdir -p composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components
mkdir -p composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme
mkdir -p composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/util
```

**Step 2: Create .gitkeep files (optional)**

This ensures empty directories are tracked:

```bash
touch composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/.gitkeep
touch composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/.gitkeep
touch composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/models/.gitkeep
touch composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/.gitkeep
touch composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/.gitkeep
touch composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/util/.gitkeep
```

**Step 3: Verify structure**

Run: `find composeApp/src/commonMain/kotlin/io/github/devmugi -type d`
Expected: All directories listed

**Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi
git commit -m "Create package structure for CV Agent

Add empty directories for data, api, agent, ui, and util packages.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Implement Color Definitions

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Color.kt`

**Step 1: Create Color.kt file**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Color.kt`:

```kotlin
package io.github.devmugi.cv.agent.ui.theme

import androidx.compose.material3.darkColorScheme
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

**Step 2: Build to verify compilation**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Color.kt
git commit -m "Add color definitions for design system

Define brand colors (dark navy, gold) and Material3 dark color scheme matching portfolio theme.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 7: Implement Typography Definitions

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Typography.kt`

**Step 1: Create Typography.kt file**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Typography.kt`:

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

**Step 2: Build to verify compilation**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Typography.kt
git commit -m "Add typography definitions for design system

Define Material3 typography scale with display, title, body, and label styles.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 8: Implement Shape Definitions

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Shape.kt`

**Step 1: Create Shape.kt file**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Shape.kt`:

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

**Step 2: Build to verify compilation**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Shape.kt
git commit -m "Add shape definitions for design system

Define rounded corner shapes for chips, cards, and dialogs.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 9: Implement Spacing Definitions

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Dimens.kt`

**Step 1: Create Dimens.kt file**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Dimens.kt`:

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

**Step 2: Build to verify compilation**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Dimens.kt
git commit -m "Add spacing dimensions for design system

Define consistent spacing values (4dp to 32dp) for UI layout.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 10: Implement Theme Composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Theme.kt`

**Step 1: Create Theme.kt file**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Theme.kt`:

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

**Step 2: Build to verify compilation**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Theme.kt
git commit -m "Add CVAgentTheme composable

Create theme wrapper combining color scheme, typography, and shapes.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 11: Create ChatScreen Placeholder

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`

**Step 1: Create ChatScreen.kt file**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`:

```kotlin
package io.github.devmugi.cv.agent.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

**Step 2: Build to verify compilation**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt
git commit -m "Add ChatScreen placeholder with themed UI

Create main screen with TopBar and centered placeholder text using CVAgentTheme.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 12: Update Android MainActivity

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

**Step 1: Update MainActivity to use ChatScreen**

Replace the entire content of `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`:

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

**Step 2: Build to verify compilation**

Run: `./gradlew composeApp:assembleDebug`
Expected: SUCCESS, APK created

**Step 3: Commit**

```bash
git add composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/MainActivity.kt
git commit -m "Update MainActivity to use ChatScreen

Replace template code with ChatScreen entry point.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 13: Update iOS MainViewController

**Files:**
- Modify: `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt`

**Step 1: Update MainViewController to use ChatScreen**

Replace the entire content of `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt`:

```kotlin
package io.github.devmugi.cv.agent

import androidx.compose.ui.window.ComposeUIViewController
import io.github.devmugi.cv.agent.ui.ChatScreen

fun MainViewController() = ComposeUIViewController {
    ChatScreen()
}
```

**Step 2: Build to verify compilation**

Run: `./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64`
Expected: SUCCESS, framework created

**Step 3: Commit**

```bash
git add composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt
git commit -m "Update iOS MainViewController to use ChatScreen

Replace template code with ChatScreen entry point for iOS.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 14: Update iOS Swift ContentView

**Files:**
- Modify: `iosApp/iosApp/ContentView.swift`

**Step 1: Update ContentView.swift to call MainViewController**

Replace the entire content of `iosApp/iosApp/ContentView.swift`:

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

**Step 2: Build iOS framework**

Run: `./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add iosApp/iosApp/ContentView.swift
git commit -m "Update iOS ContentView to use MainViewController

Connect Swift UI to Kotlin MainViewController for Compose integration.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 15: Create Resources Directory and Placeholder Data

**Files:**
- Create: `composeApp/src/commonMain/composeResources/files/cv_data.json`

**Step 1: Create resources directory structure**

Run:
```bash
mkdir -p composeApp/src/commonMain/composeResources/files
```

**Step 2: Create placeholder cv_data.json**

Create `composeApp/src/commonMain/composeResources/files/cv_data.json`:

```json
{
  "placeholder": "CV data will be added in Phase 2"
}
```

**Step 3: Verify resources are accessible**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: SUCCESS

**Step 4: Commit**

```bash
git add composeApp/src/commonMain/composeResources
git commit -m "Add resources directory with placeholder CV data

Create composeResources structure and placeholder JSON for Phase 2.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 16: Clean Up Template Code

**Files:**
- Delete: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/App.kt`
- Delete: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/Greeting.kt`
- Delete: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/Platform.kt`
- Delete: `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/Platform.android.kt`
- Delete: `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/Platform.ios.kt`
- Delete: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ComposeAppCommonTest.kt`

**Step 1: Remove template files**

Run:
```bash
rm composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/App.kt
rm composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/Greeting.kt
rm composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/Platform.kt
rm composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/Platform.android.kt
rm composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/Platform.ios.kt
rm composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ComposeAppCommonTest.kt
```

**Step 2: Build to verify no dependencies on removed files**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add -A
git commit -m "Remove template code files

Delete unused App.kt, Greeting.kt, Platform files, and template test.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 17: Run Ktlint Format and Fix Issues

**Files:**
- Potentially all Kotlin files

**Step 1: Run ktlintFormat**

Run: `./gradlew ktlintFormat`
Expected: Formats code, SUCCESS

**Step 2: Verify no violations remain**

Run: `./gradlew ktlintCheck`
Expected: SUCCESS

**Step 3: If files were modified, commit**

```bash
git add -A
git commit -m "Format code with Ktlint

Apply Kotlin code style formatting across project.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

Note: If no files were modified, skip the commit step.

---

## Task 18: Run Detekt and Fix Issues

**Files:**
- Potentially any Kotlin files with violations

**Step 1: Run Detekt**

Run: `./gradlew detekt`
Expected: SUCCESS or FAILED with specific violations

**Step 2: Fix any violations reported**

Review output and fix issues:
- Line length > 120: Break long lines
- Complexity issues: Simplify if needed (unlikely in simple code)
- Naming violations: Fix according to conventions

**Step 3: Re-run Detekt**

Run: `./gradlew detekt`
Expected: SUCCESS

**Step 4: If fixes were needed, commit**

```bash
git add -A
git commit -m "Fix Detekt violations

Address code quality issues identified by static analysis.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

Note: If no violations or fixes needed, skip this step.

---

## Task 19: Run Quality Check

**Files:**
- N/A

**Step 1: Run combined quality check**

Run: `./gradlew qualityCheck`
Expected: SUCCESS (both Ktlint and Detekt pass)

**Step 2: If failures occur, diagnose**

Review errors:
- Ktlint: Run `./gradlew ktlintFormat` again
- Detekt: Review violations and fix

**Step 3: Verify success**

Output should show:
```
BUILD SUCCESSFUL
```

Note: No commit needed, this is verification only.

---

## Task 20: Build Android APK

**Files:**
- N/A

**Step 1: Clean build**

Run: `./gradlew clean`
Expected: SUCCESS

**Step 2: Build debug APK**

Run: `./gradlew composeApp:assembleDebug`
Expected: SUCCESS, APK at `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

**Step 3: Verify APK exists**

Run: `ls -lh composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Expected: File exists, ~10-20MB

Note: No commit needed, this is verification only.

---

## Task 21: Build iOS Framework

**Files:**
- N/A

**Step 1: Build iOS framework for simulator**

Run: `./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64`
Expected: SUCCESS, framework created

**Step 2: Verify framework exists**

Run: `ls -lh composeApp/build/bin/iosSimulatorArm64/debugFramework/`
Expected: ComposeApp.framework directory exists

Note: No commit needed, this is verification only.

---

## Task 22: Final Verification and Documentation

**Files:**
- Create: `docs/verification/phase1-completion.md`

**Step 1: Create verification checklist document**

Create `docs/verification/phase1-completion.md`:

```markdown
# Phase 1 Completion Verification

**Date:** 2026-01-20
**Status:** ✅ COMPLETE

## Build Success

- ✅ `./gradlew composeApp:assembleDebug` - Android APK created
- ✅ `./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64` - iOS framework built

## Quality Gates

- ✅ `./gradlew ktlintCheck` - No violations
- ✅ `./gradlew detekt` - No issues
- ✅ `./gradlew qualityCheck` - Both tools pass

## Structure

- ✅ Package directories created under `io.github.devmugi.cv.agent`
- ✅ Design system implemented: Color.kt, Typography.kt, Shape.kt, Dimens.kt, Theme.kt
- ✅ ChatScreen.kt created and compiles
- ✅ Resources directory with placeholder cv_data.json

## Configuration

- ✅ libs.versions.toml updated with all dependencies
- ✅ detekt.yml created and configured
- ✅ Ktlint configured in build.gradle.kts
- ✅ All dependencies resolve successfully

## Next Steps

Ready for Phase 2: Data Layer & CV Models
- Implement data models
- Populate cv_data.json
- Write unit tests
```

**Step 2: Commit verification document**

```bash
git add docs/verification
git commit -m "Add Phase 1 completion verification

Document successful completion of all acceptance criteria.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Acceptance Criteria Checklist

After completing all tasks, verify:

### Build Success
- [ ] `./gradlew composeApp:assembleDebug` succeeds
- [ ] `./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64` succeeds

### Quality Gates
- [ ] `./gradlew ktlintCheck` passes with no violations
- [ ] `./gradlew detekt` passes with no issues
- [ ] `./gradlew qualityCheck` passes

### App Launch (Manual Testing)
- [ ] Android app launches on emulator/device
- [ ] Shows "CV Agent - Coming Soon" text in gold color
- [ ] TopBar displays "<DH/> CV Agent" with correct styling
- [ ] Background is dark navy color
- [ ] iOS app launches on simulator (if available)
- [ ] Shows identical UI to Android

### Structure
- [ ] All package directories exist
- [ ] Design system files compile
- [ ] ChatScreen compiles
- [ ] Resources directory exists with cv_data.json

### Configuration
- [ ] libs.versions.toml has all dependencies
- [ ] detekt.yml exists
- [ ] Ktlint configured
- [ ] Dependencies resolve

---

## Estimated Time

- **Task 1-4:** 15 minutes (dependency configuration)
- **Task 5:** 2 minutes (directory creation)
- **Task 6-10:** 15 minutes (design system)
- **Task 11-15:** 15 minutes (screens and resources)
- **Task 16:** 2 minutes (cleanup)
- **Task 17-19:** 10 minutes (quality checks)
- **Task 20-22:** 10 minutes (verification)

**Total:** ~70 minutes (~1.2 hours)

---

## Notes

- All commits follow conventional commit format
- Each task is atomic and can be verified independently
- Quality gates run after implementation is complete
- Manual app launch testing is final verification step
- Use `./gradlew --stop` if Gradle daemon causes issues

---

**Plan Complete - Ready for Execution**

Use `@superpowers:executing-plans` (parallel session) or `@superpowers:subagent-driven-development` (this session) to implement.
