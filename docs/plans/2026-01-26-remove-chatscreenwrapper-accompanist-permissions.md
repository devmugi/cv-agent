# Remove ChatScreenWrapper, Use Accompanist Permissions

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove the `ChatScreenWrapper` indirection and use Accompanist's `rememberPermissionState` for microphone permission handling directly in `AppContent`.

**Why:** Simplifies the architecture by eliminating a wrapper layer. Accompanist provides a cleaner, more reactive API for permission handling than the manual `rememberLauncherForActivityResult` + pending flag approach.

---

## Task 1: Add Accompanist Permissions Dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `android-app/build.gradle.kts`

**Step 1: Add to version catalog**

In `gradle/libs.versions.toml`, add to `[libraries]` section:

```toml
accompanist-permissions = { module = "com.google.accompanist:accompanist-permissions", version = "0.34.0" }
```

**Step 2: Add dependency to android-app**

In `android-app/build.gradle.kts`, add:

```kotlin
implementation(libs.accompanist.permissions)
```

**Step 3: Verify build**

Run: `./gradlew :android-app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml android-app/build.gradle.kts
git commit -m "build(android): add Accompanist permissions dependency"
```

---

## Task 2: Update AppContent with Accompanist Permission Handling

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

**Step 1: Add imports**

Add these imports to MainActivity.kt:

```kotlin
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
```

**Step 2: Update AppContent signature**

Add `@OptIn(ExperimentalPermissionsApi::class)` annotation to `AppContent`.

**Step 3: Add permission state in AppContent**

At the start of `AppContent`, add:

```kotlin
val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
```

**Step 4: Collect voice state**

Move from ChatScreenWrapper into AppContent:

```kotlin
val voiceState by voiceController.state.collectAsState()
```

**Step 5: Add voice error handling**

Add LaunchedEffect for voice errors:

```kotlin
LaunchedEffect(voiceState) {
    if (voiceState is VoiceInputState.Error) {
        toastState.show((voiceState as VoiceInputState.Error).message)
        voiceController.clearError()
    }
}
```

**Step 6: Add permission denial toast**

Add LaunchedEffect for permission denial:

```kotlin
LaunchedEffect(micPermissionState.status) {
    if (!micPermissionState.status.isGranted &&
        micPermissionState.status.shouldShowRationale) {
        toastState.show("Microphone permission required for voice input")
    }
}
```

**Step 7: Replace ChatScreenWrapper with ChatScreen**

In the `Screen.Chat` case, replace `ChatScreenWrapper(...)` with direct `ChatScreen(...)` call:

```kotlin
Screen.Chat -> ChatScreen(
    state = state,
    toastState = toastState,
    onSendMessage = viewModel::sendMessage,
    analytics = analytics,
    onSuggestionClick = viewModel::onSuggestionClicked,
    onCopyMessage = viewModel::onMessageCopied,
    onShareMessage = { /* TODO */ },
    onLikeMessage = viewModel::onMessageLiked,
    onDislikeMessage = viewModel::onMessageDisliked,
    onRegenerateMessage = viewModel::onRegenerateClicked,
    onClearHistory = viewModel::clearHistory,
    onNavigateToCareerTimeline = { onScreenChange(Screen.CareerTimeline) },
    onNavigateToProject = { projectId ->
        careerProjectsMap[projectId]?.let { project ->
            viewModel.onProjectSuggestionClicked(projectId, 0)
            onProjectSelect(project)
            onScreenChange(Screen.ProjectDetails)
        }
    },
    // Voice input
    isRecording = voiceState is VoiceInputState.Recording,
    isTranscribing = voiceState is VoiceInputState.Transcribing,
    onRecordingStart = { voiceController.startRecording() },
    onRecordingStop = {
        voiceController.stopRecordingAndTranscribe { transcribedText ->
            if (transcribedText.isNotBlank()) {
                viewModel.sendMessage(transcribedText)
            }
        }
    },
    onRequestMicPermission = { micPermissionState.launchPermissionRequest() },
    hasMicPermission = micPermissionState.status.isGranted
)
```

**Step 8: Remove ChatScreenWrapper import**

Remove from imports:

```kotlin
import io.github.devmugi.cv.agent.ui.ChatScreenWrapper
```

**Step 9: Verify build**

Run: `./gradlew :android-app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 10: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt
git commit -m "refactor(android): use Accompanist permissions in AppContent"
```

---

## Task 3: Delete ChatScreenWrapper

**Files:**
- Delete: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/ui/ChatScreenWrapper.kt`

**Step 1: Delete the file**

```bash
rm android-app/src/main/kotlin/io/github/devmugi/cv/agent/ui/ChatScreenWrapper.kt
```

**Step 2: Verify build**

Run: `./gradlew :android-app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add -A
git commit -m "refactor(android): remove ChatScreenWrapper"
```

---

## Task 4: Run Quality Checks and Full Build

**Step 1: Run quality checks**

Run: `./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL

**Step 2: Build debug APK**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Squash commits (optional)**

If desired, squash the 3 commits into one:

```bash
git rebase -i HEAD~3
# Squash into: "refactor(android): replace ChatScreenWrapper with Accompanist permissions"
```

---

## Summary

| Task | Description | Verification |
|------|-------------|--------------|
| 1 | Add Accompanist dependency | Build compiles |
| 2 | Update AppContent with permission handling | Build compiles |
| 3 | Delete ChatScreenWrapper | Build compiles |
| 4 | Quality checks and full build | All checks pass |

**Manual testing required:**
- Permission request flow on real device
- Permission denial shows toast
- Voice recording works after permission granted
- Re-requesting permission after denial
