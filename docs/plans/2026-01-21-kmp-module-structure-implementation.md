# KMP Module Structure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Restructure cv-agent into 5 modules with kebab-case naming and separate Android/iOS entry points.

**Architecture:** Add `shared-domain` module for types/interfaces to break circular dependency. `shared` exports all modules for iOS framework. `android-app` is Android-only entry point.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Gradle with typesafe project accessors.

---

## Task 1: Rename existing modules to kebab-case

**Files:**
- Rename: `sharedDesignSystem/` → `shared-design-system/`
- Rename: `sharedUi/` → `shared-ui/`
- Modify: `settings.gradle.kts`

**Step 1: Rename sharedDesignSystem directory**

```bash
mv sharedDesignSystem shared-design-system
```

**Step 2: Rename sharedUi directory**

```bash
mv sharedUi shared-ui
```

**Step 3: Update settings.gradle.kts**

Replace:
```kotlin
include(":composeApp")
include(":sharedDesignSystem")
include(":shared")
include(":sharedUi")
```

With:
```kotlin
include(":shared-design-system")
include(":shared-domain")
include(":shared-ui")
include(":shared")
include(":android-app")
```

**Step 4: Verify rename worked**

```bash
ls -la
```

Expected: See `shared-design-system/`, `shared-ui/` directories

**Step 5: Commit**

```bash
git add -A && git commit -m "build: rename modules to kebab-case"
```

---

## Task 2: Create shared-domain module

**Files:**
- Create: `shared-domain/build.gradle.kts`
- Create: `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/CVData.kt`
- Create: `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/CVReference.kt`
- Create: `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/Message.kt`
- Create: `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/ChatState.kt`
- Create: `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/TimeUtils.kt`
- Create: `shared-domain/src/androidMain/kotlin/io/github/devmugi/cv/agent/domain/TimeUtils.android.kt`
- Create: `shared-domain/src/iosMain/kotlin/io/github/devmugi/cv/agent/domain/TimeUtils.ios.kt`
- Create: `shared-domain/src/jvmMain/kotlin/io/github/devmugi/cv/agent/domain/TimeUtils.jvm.kt`

**Step 1: Create build.gradle.kts**

Create `shared-domain/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedDomain"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
```

**Step 2: Create directory structure**

```bash
mkdir -p shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models
mkdir -p shared-domain/src/commonTest/kotlin/io/github/devmugi/cv/agent/domain
mkdir -p shared-domain/src/androidMain/kotlin/io/github/devmugi/cv/agent/domain
mkdir -p shared-domain/src/iosMain/kotlin/io/github/devmugi/cv/agent/domain
mkdir -p shared-domain/src/jvmMain/kotlin/io/github/devmugi/cv/agent/domain
```

**Step 3: Create CVData.kt**

Copy content from `shared/src/commonMain/kotlin/.../data/models/CVData.kt` to `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/CVData.kt`, updating package to `io.github.devmugi.cv.agent.domain.models`.

**Step 4: Create CVReference.kt**

Create `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/CVReference.kt`:

```kotlin
package io.github.devmugi.cv.agent.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class CVReference(
    val id: String,
    val type: String,
    val label: String
)
```

**Step 5: Create TimeUtils.kt (expect)**

Create `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/TimeUtils.kt`:

```kotlin
package io.github.devmugi.cv.agent.domain

expect fun currentTimeMillis(): Long
```

**Step 6: Create TimeUtils.android.kt**

Create `shared-domain/src/androidMain/kotlin/io/github/devmugi/cv/agent/domain/TimeUtils.android.kt`:

```kotlin
package io.github.devmugi.cv.agent.domain

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
```

**Step 7: Create TimeUtils.ios.kt**

Create `shared-domain/src/iosMain/kotlin/io/github/devmugi/cv/agent/domain/TimeUtils.ios.kt`:

```kotlin
package io.github.devmugi.cv.agent.domain

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
```

**Step 8: Create TimeUtils.jvm.kt**

Create `shared-domain/src/jvmMain/kotlin/io/github/devmugi/cv/agent/domain/TimeUtils.jvm.kt`:

```kotlin
package io.github.devmugi.cv.agent.domain

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
```

**Step 9: Create Message.kt**

Create `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/Message.kt`:

```kotlin
package io.github.devmugi.cv.agent.domain.models

import io.github.devmugi.cv.agent.domain.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

@OptIn(ExperimentalUuidApi::class)
data class Message(
    val id: String = Uuid.random().toString(),
    val role: MessageRole,
    val content: String,
    val references: List<CVReference> = emptyList(),
    val timestamp: Long = currentTimeMillis()
)
```

**Step 10: Create ChatState.kt**

Create `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/ChatState.kt`:

```kotlin
package io.github.devmugi.cv.agent.domain.models

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val error: ChatError? = null,
    val suggestions: List<String> = defaultSuggestions
)

sealed class ChatError {
    data class Network(val message: String) : ChatError()
    data class Api(val message: String) : ChatError()
    data object RateLimit : ChatError()
}

val defaultSuggestions = listOf(
    "What's Denys's experience?",
    "Tell me about his skills",
    "What projects has he worked on?",
    "What are his achievements?"
)
```

**Step 11: Verify module compiles**

```bash
./gradlew :shared-domain:assemble
```

Expected: BUILD SUCCESSFUL

**Step 12: Commit**

```bash
git add -A && git commit -m "feat: create shared-domain module with types"
```

---

## Task 3: Update shared-design-system for kebab-case

**Files:**
- Modify: `shared-design-system/build.gradle.kts` (if needed)

**Step 1: Verify build.gradle.kts is correct**

Ensure the framework baseName and configuration are correct.

**Step 2: Verify module compiles**

```bash
./gradlew :shared-design-system:assemble
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit if changes needed**

```bash
git add -A && git commit -m "build: update shared-design-system configuration"
```

---

## Task 4: Update shared-ui module

**Files:**
- Modify: `shared-ui/build.gradle.kts`
- Modify: All `.kt` files in `shared-ui/src/commonMain/kotlin/.../ui/`

**Step 1: Update build.gradle.kts dependencies**

Replace project dependencies in `shared-ui/build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation(projects.sharedDesignSystem)
    implementation(projects.sharedDomain)
    // Remove: implementation(project(":shared"))
}
```

**Step 2: Update imports in all UI files**

Replace:
- `io.github.devmugi.cv.agent.agent.ChatState` → `io.github.devmugi.cv.agent.domain.models.ChatState`
- `io.github.devmugi.cv.agent.agent.Message` → `io.github.devmugi.cv.agent.domain.models.Message`
- `io.github.devmugi.cv.agent.data.models.CVData` → `io.github.devmugi.cv.agent.domain.models.CVData`
- `io.github.devmugi.cv.agent.data.models.CVReference` → `io.github.devmugi.cv.agent.domain.models.CVReference`

**Step 3: Verify module compiles**

```bash
./gradlew :shared-ui:assemble
```

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add -A && git commit -m "refactor: update shared-ui to use shared-domain types"
```

---

## Task 5: Update shared module

**Files:**
- Modify: `shared/build.gradle.kts`
- Delete: Moved type files
- Modify: All files to update imports

**Step 1: Update build.gradle.kts**

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
        }
    }
}
```

**Step 2: Delete moved files**

```bash
rm shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/CVData.kt
rm shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/CVReference.kt
rm shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/Message.kt
rm shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatState.kt
rm -f shared/src/androidMain/kotlin/io/github/devmugi/cv/agent/agent/TimeUtils.android.kt
rm -f shared/src/iosMain/kotlin/io/github/devmugi/cv/agent/agent/TimeUtils.ios.kt
rm -f shared/src/jvmMain/kotlin/io/github/devmugi/cv/agent/agent/TimeUtils.jvm.kt
```

**Step 3: Update imports in all remaining files**

Update ChatViewModel.kt, ReferenceExtractor.kt, SystemPromptBuilder.kt, CVRepository.kt, etc.

**Step 4: Verify module compiles**

```bash
./gradlew :shared:assemble
```

Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add -A && git commit -m "refactor: update shared to use shared-domain types"
```

---

## Task 6: Move iOS entry point to shared module

**Files:**
- Move: `composeApp/src/iosMain/.../MainViewController.kt` → `shared/src/iosMain/.../MainViewController.kt`

**Step 1: Copy MainViewController.kt to shared**

```bash
cp composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt \
   shared/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt
```

**Step 2: Update imports**

Update to use shared-domain types.

**Step 3: Verify shared module compiles**

```bash
./gradlew :shared:assemble
```

**Step 4: Commit**

```bash
git add -A && git commit -m "feat: move iOS entry point to shared module"
```

---

## Task 7: Create android-app module

**Files:**
- Create: `android-app/build.gradle.kts`
- Create: `android-app/src/main/AndroidManifest.xml`
- Move: MainActivity.kt, CVAgentApplication.kt, res/

**Step 1: Create directory structure**

```bash
mkdir -p android-app/src/main/kotlin/io/github/devmugi/cv/agent
mkdir -p android-app/src/androidTest/kotlin/io/github/devmugi/cv/agent
```

**Step 2: Create build.gradle.kts**

Standard Android app build file depending on `:shared`.

**Step 3: Create AndroidManifest.xml**

Standard manifest with MainActivity and CVAgentApplication.

**Step 4: Copy and update MainActivity.kt**

Update imports and resource references.

**Step 5: Copy CVAgentApplication.kt**

**Step 6: Copy res directory**

```bash
cp -r composeApp/src/androidMain/res android-app/src/main/
```

**Step 7: Copy E2E tests**

```bash
cp -r composeApp/src/androidInstrumentedTest/kotlin android-app/src/androidTest/
```

**Step 8: Verify module compiles**

```bash
./gradlew :android-app:assembleDebug
```

**Step 9: Commit**

```bash
git add -A && git commit -m "feat: create android-app module"
```

---

## Task 8: Delete composeApp module

**Step 1: Remove composeApp directory**

```bash
rm -rf composeApp
```

**Step 2: Verify full build**

```bash
./gradlew clean assemble
```

**Step 3: Commit**

```bash
git add -A && git commit -m "build: remove deprecated composeApp module"
```

---

## Task 9: Update quality checks

**Step 1: Update root build.gradle.kts**

Ensure quality checks include new modules.

**Step 2: Run quality checks**

```bash
./gradlew ktlintCheck detekt
```

**Step 3: Fix any lint issues**

**Step 4: Commit**

```bash
git add -A && git commit -m "build: update quality checks for new modules"
```

---

## Task 10: Run all tests

**Step 1: Run all tests**

```bash
./gradlew :shared-domain:allTests
./gradlew :shared:allTests
./gradlew :shared-ui:allTests
./gradlew build
```

**Step 2: Commit final state**

```bash
git add -A && git commit -m "test: verify all tests pass"
```

---

## Task 11: Runtime verification

**Step 1: Install and run Android app**

```bash
./gradlew :android-app:installDebug
```

**Step 2: Manual verification checklist**

- [ ] App launches with theme
- [ ] Welcome screen shows suggestions
- [ ] Messages send and receive
- [ ] Markdown renders
- [ ] Error handling works

---

## Summary

Final module structure:

```
cv-agent/
├── shared-design-system/    # Theme only
├── shared-domain/           # Types and interfaces
├── shared-ui/               # UI components
├── shared/                  # Business logic + iOS entry
├── android-app/             # Android entry point
└── iosApp/                  # Xcode project
```
