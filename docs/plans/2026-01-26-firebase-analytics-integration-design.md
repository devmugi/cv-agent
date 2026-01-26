# Firebase Analytics Integration Design

**Date:** 2026-01-26
**Status:** Ready for Implementation

## Overview

Extend the existing Firebase Analytics setup to actually call analytics events from UI and ViewModel. Events are already defined in `AnalyticsEvent.kt` but not being logged anywhere.

## Current State

- Firebase Analytics 22.4.0 (direct dependency)
- `AnalyticsEvent` sealed class with 15+ typed events defined
- `ChatViewModel` has Analytics injected but zero events logged
- UI components have no Analytics access

## Design Decisions

1. **Firebase BOM** - Use BOM v34.7.0 for version management
2. **Screen analytics** - Pass `Analytics` as parameter to composables (explicit, testable)
3. **Action callbacks** - Wrap in ViewModel methods that log before executing (centralized)

## Implementation

### 1. Firebase BOM Update

```toml
# libs.versions.toml
[versions]
firebase-bom = "34.7.0"

[libraries]
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebase-bom" }
firebase-analytics = { module = "com.google.firebase:firebase-analytics-ktx" }
```

```kotlin
// android-app/build.gradle.kts
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.analytics)
```

### 2. ChatViewModel Direct Events

Log these events directly where they occur:

| Method | Event | Parameters |
|--------|-------|------------|
| `sendMessage()` | `Chat.MessageSent` | messageLength, sessionId, turnNumber |
| `clearHistory()` | `Chat.HistoryCleared` | messageCount, sessionId |
| `streamResponse()` onComplete | `Chat.ResponseCompleted` | responseTimeMs, tokenCount, sessionId |
| `streamResponse()` onError | `Error.ErrorDisplayed` | errorType, errorMessage, sessionId |

Track `streamStartTime` at beginning of `streamResponse()` to calculate `responseTimeMs`.

### 3. ChatViewModel Callback Wrappers

Add wrapper methods for UI actions:

```kotlin
fun onMessageCopied(messageId: String)      // Chat.MessageCopied
fun onMessageLiked(messageId: String)       // Chat.MessageLiked
fun onMessageDisliked(messageId: String)    // Chat.MessageDisliked
fun onRegenerateClicked(messageId: String)  // Chat.RegenerateClicked + retry()
fun onProjectSuggestionClicked(projectId: String, position: Int)  // Chat.SuggestionClicked
```

Wire these in MainActivity when creating ChatScreen callbacks.

### 4. Screen Analytics Parameter

Add `analytics: Analytics = Analytics.NOOP` parameter to:

| Screen | Event |
|--------|-------|
| `ChatScreen` | `Navigation.ScreenView(CHAT)` |
| `CareerProjectsTimelineScreen` | `Navigation.ScreenView(CAREER_TIMELINE)` |
| `CareerProjectDetailsScreen` | `Navigation.ScreenView(PROJECT_DETAILS)` |

Log in `LaunchedEffect(Unit)` on first composition.

### 5. TopBar Link Tracking

Add `analytics: Analytics = Analytics.NOOP` to `CVAgentTopBar`.

Log `Link.ExternalLinkClicked` for each contact link:
- LinkedIn, GitHub, Email, Phone, CV Website, CV PDF

Log `Navigation.ScreenView(CAREER_TIMELINE, previousScreen=CHAT)` on Career button click.

Thread analytics from ChatScreen to TopBar.

### 6. Career Screen Project Tracking

**CareerProjectsTimelineScreen:**
- Log `Navigation.ProjectSelected(projectId, source=TIMELINE)` on project click

**CareerProjectDetailsScreen:**
- Log `Link.ProjectLinkClicked(projectId, linkType, url)` on project link clicks

## Files to Modify

| File | Changes |
|------|---------|
| `libs.versions.toml` | Add firebase-bom version, update firebase-analytics |
| `android-app/build.gradle.kts` | Use BOM platform |
| `ChatViewModel.kt` | Add logging calls + callback wrapper methods |
| `ChatScreen.kt` | Add analytics parameter, log ScreenView |
| `CVAgentTopBar.kt` | Add analytics parameter, log link clicks |
| `CareerProjectsTimelineScreen.kt` | Add analytics parameter, log ScreenView + ProjectSelected |
| `CareerProjectDetailsScreen.kt` | Add analytics parameter, log ScreenView + ProjectLinkClicked |
| `MainActivity.kt` | Wire analytics to screens and callbacks |

## Event Summary

| Category | Events | Count |
|----------|--------|-------|
| Chat | MessageSent, HistoryCleared, ResponseCompleted, MessageCopied, MessageLiked, MessageDisliked, RegenerateClicked, SuggestionClicked | 8 |
| Navigation | ScreenView (3 screens), ProjectSelected | 4 |
| Link | ExternalLinkClicked (6 types), ProjectLinkClicked | 7 |
| Error | ErrorDisplayed | 1 |
| **Total** | | **20** |

## Implementation Order

1. Update Firebase BOM in `libs.versions.toml` + `android-app/build.gradle.kts`
2. Add logging to `ChatViewModel` (direct events + callback wrappers)
3. Add Analytics parameter to `ChatScreen` + wire in MainActivity
4. Add Analytics parameter to `CVAgentTopBar` + thread from ChatScreen
5. Add Analytics parameter to Career screens + wire in navigation
6. Build and test on Android
