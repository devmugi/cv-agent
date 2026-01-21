# KMP Module Structure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Restructure cv-agent from single `composeApp` module into 4 modules: `shared`, `sharedDesignSystem`, `sharedUi`, `androidApp`.

**Architecture:** Bottom-up module creation. Create leaf modules first (`sharedDesignSystem`), then modules that depend on them (`shared`, `sharedUi`), finally the app module (`androidApp`). Each module verified independently before proceeding.

**Tech Stack:** Kotlin Multiplatform 2.3.0, Compose Multiplatform 1.11.0-alpha01, Gradle 8.14, Koin 3.5.6

---

## Task 1: Create sharedDesignSystem Module

**Files:**
- Create: `sharedDesignSystem/build.gradle.kts`
- Create: `sharedDesignSystem/src/commonMain/kotlin/io/github/devmugi/cv/agent/designsystem/theme/Color.kt`
- Create: `sharedDesignSystem/src/commonMain/kotlin/io/github/devmugi/cv/agent/designsystem/theme/Typography.kt`
- Create: `sharedDesignSystem/src/commonMain/kotlin/io/github/devmugi/cv/agent/designsystem/theme/Shape.kt`
- Create: `sharedDesignSystem/src/commonMain/kotlin/io/github/devmugi/cv/agent/designsystem/theme/Dimens.kt`
- Create: `sharedDesignSystem/src/commonMain/kotlin/io/github/devmugi/cv/agent/designsystem/theme/Theme.kt`
- Modify: `settings.gradle.kts`

**Step 1: Update settings.gradle.kts to include sharedDesignSystem**

Add after `include(":composeApp")`:
```kotlin
include(":sharedDesignSystem")
```

**Step 2: Create sharedDesignSystem/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedDesignSystem"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
    }
}
```

**Step 3: Create directory structure**

```bash
mkdir -p sharedDesignSystem/src/commonMain/kotlin/io/github/devmugi/cv/agent/designsystem/theme
```

**Step 4: Copy theme files with updated package**

Copy from `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/` to `sharedDesignSystem/src/commonMain/kotlin/io/github/devmugi/cv/agent/designsystem/theme/`:
- Color.kt
- Typography.kt
- Shape.kt
- Dimens.kt
- Theme.kt

Update package declaration in each file from:
```kotlin
package io.github.devmugi.cv.agent.ui.theme
```
to:
```kotlin
package io.github.devmugi.cv.agent.designsystem.theme
```

**Step 5: Verify sharedDesignSystem builds**

Run: `./gradlew :sharedDesignSystem:assemble`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add sharedDesignSystem settings.gradle.kts
git commit -m "feat: create sharedDesignSystem module with theme files"
```

---

## Task 2: Create shared Module (Business Logic)

**Files:**
- Create: `shared/build.gradle.kts`
- Create: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/api/*`
- Create: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/*`
- Create: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/*`
- Create: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/*`
- Create: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/GroqConfig.kt`
- Create: `shared/src/androidMain/kotlin/io/github/devmugi/cv/agent/*`
- Create: `shared/src/iosMain/kotlin/io/github/devmugi/cv/agent/*`
- Create: `shared/src/commonMain/composeResources/files/cv_data.json`
- Modify: `settings.gradle.kts`

**Step 1: Update settings.gradle.kts**

Add:
```kotlin
include(":shared")
```

**Step 2: Create shared/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "io.github.devmugi.cv.agent.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":sharedDesignSystem"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":sharedDesignSystem"))

            // Compose (for ViewModel)
            implementation(libs.compose.runtime)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // DI
            implementation(libs.koin.core)

            // Resources
            implementation(libs.compose.components.resources)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

**Step 3: Create directory structure**

```bash
mkdir -p shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/{api/models,agent,data/models,data/repository,di}
mkdir -p shared/src/commonMain/composeResources/files
mkdir -p shared/src/androidMain/kotlin/io/github/devmugi/cv/agent/{di,agent}
mkdir -p shared/src/iosMain/kotlin/io/github/devmugi/cv/agent/{di,agent}
mkdir -p shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/{api/models,agent,data}
```

**Step 4: Copy commonMain business logic files**

Copy these files (keeping same package `io.github.devmugi.cv.agent.*`):

From `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/`:
- `api/GroqApiClient.kt` → `shared/src/commonMain/.../api/`
- `api/GroqApiException.kt` → `shared/src/commonMain/.../api/`
- `api/models/ChatMessage.kt` → `shared/src/commonMain/.../api/models/`
- `api/models/ChatRequest.kt` → `shared/src/commonMain/.../api/models/`
- `api/models/StreamChunk.kt` → `shared/src/commonMain/.../api/models/`
- `agent/ChatState.kt` → `shared/src/commonMain/.../agent/`
- `agent/ChatViewModel.kt` → `shared/src/commonMain/.../agent/`
- `agent/Message.kt` → `shared/src/commonMain/.../agent/`
- `agent/ReferenceExtractor.kt` → `shared/src/commonMain/.../agent/`
- `agent/SystemPromptBuilder.kt` → `shared/src/commonMain/.../agent/`
- `data/models/CVData.kt` → `shared/src/commonMain/.../data/models/`
- `data/models/CVReference.kt` → `shared/src/commonMain/.../data/models/`
- `data/repository/CVDataLoader.kt` → `shared/src/commonMain/.../data/repository/`
- `data/repository/CVRepository.kt` → `shared/src/commonMain/.../data/repository/`
- `di/AppModule.kt` → `shared/src/commonMain/.../di/`
- `di/HttpEngineFactory.kt` → `shared/src/commonMain/.../di/`
- `GroqConfig.kt` → `shared/src/commonMain/.../`

**Step 5: Copy androidMain files**

From `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/`:
- `GroqConfig.android.kt` → `shared/src/androidMain/.../`
- `di/HttpEngineFactory.android.kt` → `shared/src/androidMain/.../di/`
- `agent/TimeUtils.android.kt` → `shared/src/androidMain/.../agent/`

**Step 6: Copy iosMain files**

From `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/`:
- `GroqConfig.ios.kt` → `shared/src/iosMain/.../`
- `di/HttpEngineFactory.ios.kt` → `shared/src/iosMain/.../di/`
- `agent/TimeUtils.ios.kt` → `shared/src/iosMain/.../agent/`

**Step 7: Copy cv_data.json**

```bash
cp composeApp/src/commonMain/composeResources/files/cv_data.json shared/src/commonMain/composeResources/files/
```

**Step 8: Copy commonTest files**

From `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/`:
- `api/GroqApiClientTest.kt` → `shared/src/commonTest/.../api/`
- `api/models/ApiModelsTest.kt` → `shared/src/commonTest/.../api/models/`
- `agent/ChatViewModelTest.kt` → `shared/src/commonTest/.../agent/`
- `agent/MessageTest.kt` → `shared/src/commonTest/.../agent/`
- `agent/ReferenceExtractorTest.kt` → `shared/src/commonTest/.../agent/`
- `agent/SystemPromptBuilderTest.kt` → `shared/src/commonTest/.../agent/`
- `data/CVDataLoaderTest.kt` → `shared/src/commonTest/.../data/`

**Step 9: Update CVDataLoader resource path if needed**

Check that resource loading uses Compose Resources API correctly:
```kotlin
Res.readBytes("files/cv_data.json")
```

**Step 10: Verify shared module builds**

Run: `./gradlew :shared:assemble`
Expected: BUILD SUCCESSFUL

**Step 11: Run shared module tests**

Run: `./gradlew :shared:testDebugUnitTest`
Expected: All tests pass

**Step 12: Commit**

```bash
git add shared settings.gradle.kts
git commit -m "feat: create shared module with business logic"
```

---

## Task 3: Create sharedUi Module

**Files:**
- Create: `sharedUi/build.gradle.kts`
- Create: `sharedUi/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/*`
- Create: `sharedUi/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`
- Create: `sharedUi/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/*`
- Modify: `settings.gradle.kts`

**Step 1: Update settings.gradle.kts**

Add:
```kotlin
include(":sharedUi")
```

**Step 2: Create sharedUi/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedUi"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":sharedDesignSystem"))

            // Compose
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)

            // Lifecycle (for collectAsState)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Markdown
            implementation(libs.multiplatform.markdown.renderer)
            implementation(libs.multiplatform.markdown.renderer.m3)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(libs.compose.ui.test)
        }
    }
}
```

**Step 3: Create directory structure**

```bash
mkdir -p sharedUi/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components
mkdir -p sharedUi/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components
```

**Step 4: Copy UI component files**

Copy from `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/`:
- `components/ErrorMessage.kt`
- `components/MessageBubble.kt`
- `components/MessageInput.kt`
- `components/ReferenceChip.kt`
- `components/SuggestionChip.kt`
- `components/SuggestionChipsGrid.kt`
- `components/TooltipProvider.kt`
- `components/TopBar.kt`
- `components/WelcomeSection.kt`
- `ChatScreen.kt`

to `sharedUi/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/`

**Step 5: Update imports in copied files**

Replace in all copied files:
```kotlin
import io.github.devmugi.cv.agent.ui.theme.*
```
with:
```kotlin
import io.github.devmugi.cv.agent.designsystem.theme.*
```

**Step 6: Modify ChatScreen to accept ViewModel as parameter**

Update `ChatScreen.kt` signature from:
```kotlin
@Composable
fun ChatScreen(viewModel: ChatViewModel = koinViewModel()) {
```
to:
```kotlin
@Composable
fun ChatScreen(
    state: ChatState,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
```

This makes ChatScreen a pure presentation component. Update the function body to use `state` parameter instead of `viewModel.state.collectAsState()` and call `onSendMessage` instead of `viewModel.sendMessage()`.

**Step 7: Copy UI test files**

Copy from `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/`:
- `ErrorMessageTest.kt`
- `MessageBubbleTest.kt`
- `MessageInputTest.kt`
- `ReferenceChipTest.kt`
- `SuggestionChipTest.kt`
- `SuggestionChipsGridTest.kt`
- `TooltipProviderTest.kt`
- `TopBarTest.kt`

to `sharedUi/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/`

Update theme imports in test files as well.

**Step 8: Verify sharedUi builds**

Run: `./gradlew :sharedUi:assemble`
Expected: BUILD SUCCESSFUL

**Step 9: Commit**

```bash
git add sharedUi settings.gradle.kts
git commit -m "feat: create sharedUi module with UI components"
```

---

## Task 4: Update shared Module to Export sharedUi

**Files:**
- Modify: `shared/build.gradle.kts`

**Step 1: Add sharedUi as api dependency**

In `shared/build.gradle.kts`, update the iOS framework export and commonMain dependencies:

```kotlin
listOf(
    iosArm64(),
    iosSimulatorArm64()
).forEach { iosTarget ->
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
        // ... rest of dependencies
    }
}
```

**Step 2: Verify shared module still builds**

Run: `./gradlew :shared:assemble`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/build.gradle.kts
git commit -m "feat: export sharedUi from shared module for iOS"
```

---

## Task 5: Create androidApp Module

**Files:**
- Create: `androidApp/build.gradle.kts`
- Create: `androidApp/src/main/AndroidManifest.xml`
- Create: `androidApp/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`
- Create: `androidApp/src/main/kotlin/io/github/devmugi/cv/agent/CVAgentApplication.kt`
- Create: `androidApp/src/main/res/*` (copy from composeApp)
- Modify: `settings.gradle.kts`

**Step 1: Update settings.gradle.kts**

Add:
```kotlin
include(":androidApp")
```

**Step 2: Create androidApp/build.gradle.kts**

```kotlin
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree.instrumentedTest)
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":sharedUi"))

            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
        }
    }
}

android {
    namespace = "io.github.devmugi.cv.agent"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.devmugi.cv.agent"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
        all {
            val apiKey = localProperties.getProperty("GROQ_API_KEY")
                ?: project.findProperty("GROQ_API_KEY")?.toString()
                ?: ""
            buildConfigField("String", "GROQ_API_KEY", "\"$apiKey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
```

**Step 3: Create directory structure**

```bash
mkdir -p androidApp/src/main/kotlin/io/github/devmugi/cv/agent
mkdir -p androidApp/src/main/res
mkdir -p androidApp/src/androidInstrumentedTest/kotlin/io/github/devmugi/cv/agent
```

**Step 4: Copy AndroidManifest.xml**

```bash
cp composeApp/src/androidMain/AndroidManifest.xml androidApp/src/main/
```

**Step 5: Copy Android resources**

```bash
cp -r composeApp/src/androidMain/res/* androidApp/src/main/res/
```

**Step 6: Create MainActivity.kt with ViewModel wiring**

Create `androidApp/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`:

```kotlin
package io.github.devmugi.cv.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.designsystem.theme.CVAgentTheme
import io.github.devmugi.cv.agent.ui.ChatScreen
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CVAgentTheme {
                val state by viewModel.state.collectAsState()
                ChatScreen(
                    state = state,
                    onSendMessage = viewModel::sendMessage
                )
            }
        }
    }
}
```

**Step 7: Create CVAgentApplication.kt**

Create `androidApp/src/main/kotlin/io/github/devmugi/cv/agent/CVAgentApplication.kt`:

```kotlin
package io.github.devmugi.cv.agent

import android.app.Application
import io.github.devmugi.cv.agent.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class CVAgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@CVAgentApplication)
            modules(appModule)
        }
    }
}
```

**Step 8: Copy E2E test files**

Copy from `composeApp/src/androidInstrumentedTest/kotlin/io/github/devmugi/cv/agent/`:
- `ChatE2ETest.kt`
- `RetryRule.kt`
- `MainApp.kt`

to `androidApp/src/androidInstrumentedTest/kotlin/io/github/devmugi/cv/agent/`

Update imports to use new module structure.

**Step 9: Verify androidApp builds**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 10: Commit**

```bash
git add androidApp settings.gradle.kts
git commit -m "feat: create androidApp module with entry point"
```

---

## Task 6: Remove composeApp Module

**Files:**
- Delete: `composeApp/` directory
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts` (root)

**Step 1: Remove composeApp from settings.gradle.kts**

Remove the line:
```kotlin
include(":composeApp")
```

**Step 2: Delete composeApp directory**

```bash
rm -rf composeApp
```

**Step 3: Update root build.gradle.kts if needed**

Ensure no references to `:composeApp` remain.

**Step 4: Verify full project builds**

Run: `./gradlew assemble`
Expected: BUILD SUCCESSFUL

**Step 5: Run all tests**

Run: `./gradlew :shared:testDebugUnitTest`
Expected: All tests pass

**Step 6: Commit**

```bash
git add -A
git commit -m "refactor: remove composeApp module, migration complete"
```

---

## Task 7: Quality Checks and Final Verification

**Step 1: Run ktlint**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL (no violations)

If violations exist, run: `./gradlew ktlintFormat` and commit fixes.

**Step 2: Run detekt**

Run: `./gradlew detekt`
Expected: BUILD SUCCESSFUL (no violations)

**Step 3: Verify build order works**

Run in sequence:
```bash
./gradlew :sharedDesignSystem:assemble
./gradlew :shared:assemble
./gradlew :sharedUi:assemble
./gradlew :androidApp:assembleDebug
```
Expected: All BUILD SUCCESSFUL

**Step 4: Run unit tests**

Run: `./gradlew :shared:testDebugUnitTest`
Expected: All tests pass

**Step 5: Commit any fixes**

```bash
git add -A
git commit -m "fix: address quality check violations"
```

---

## Verification Checklist

After all tasks complete:

1. [ ] `./gradlew :sharedDesignSystem:assemble` - SUCCESSFUL
2. [ ] `./gradlew :shared:assemble` - SUCCESSFUL
3. [ ] `./gradlew :sharedUi:assemble` - SUCCESSFUL
4. [ ] `./gradlew :androidApp:assembleDebug` - SUCCESSFUL
5. [ ] `./gradlew ktlintCheck` - SUCCESSFUL
6. [ ] `./gradlew detekt` - SUCCESSFUL
7. [ ] `./gradlew :shared:testDebugUnitTest` - All tests pass
8. [ ] No `composeApp` directory exists
9. [ ] `settings.gradle.kts` includes only: `shared`, `sharedDesignSystem`, `sharedUi`, `androidApp`
