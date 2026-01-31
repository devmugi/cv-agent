# Phase 4: Testing (Screenshot Tests for Adaptive Layouts) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add screenshot tests for adaptive navigation components (BottomBar, Rail, Drawer) and WindowSizeClass variations.

**Architecture:** Create Roborazzi screenshot tests following existing `ScreenshotTest` base class patterns. Test all three navigation variants and selection states.

**Tech Stack:** Kotlin, Roborazzi, Robolectric, JUnit 4

---

## Files Overview

**Create:**
- `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/adaptive/AdaptiveNavigationScreenshots.kt`
- `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/adaptive/AdaptiveScaffoldScreenshots.kt`

---

## Task 1: Add AdaptiveNavigation Screenshot Tests

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/adaptive/AdaptiveNavigationScreenshots.kt`

**Step 1: Create adaptive directory**

```bash
mkdir -p shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/adaptive
```

**Step 2: Create AdaptiveNavigationScreenshots.kt**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.devmugi.cv.agent.ui.adaptive.AdaptiveScaffold
import io.github.devmugi.cv.agent.ui.adaptive.WindowWidthSizeClass
import io.github.devmugi.cv.agent.ui.navigation.Route
import io.github.devmugi.cv.agent.ui.screenshots.ScreenshotTest
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * Screenshot tests for adaptive navigation components.
 *
 * Tests the three navigation variants:
 * - Compact: Bottom navigation bar
 * - Medium: Navigation rail
 * - Expanded: Permanent navigation drawer
 */
class AdaptiveNavigationScreenshots : ScreenshotTest() {

    // ==================== Compact (Bottom Bar) ====================

    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun bottomBar_chatSelected() {
        snapshot("bottomBar_chatSelected") {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Compact,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun bottomBar_careerSelected() {
        snapshot("bottomBar_careerSelected") {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Compact,
                currentRoute = Route.CareerTimeline,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun bottomBar_chatSelected_dark() {
        snapshot("bottomBar_chatSelected", darkTheme = true) {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Compact,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    // ==================== Medium (Navigation Rail) ====================

    @Test
    @Config(qualifiers = "w700dp-h900dp-xhdpi")
    fun navigationRail_chatSelected() {
        snapshot("navigationRail_chatSelected") {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Medium,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-xhdpi")
    fun navigationRail_careerSelected() {
        snapshot("navigationRail_careerSelected") {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Medium,
                currentRoute = Route.CareerTimeline,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-xhdpi")
    fun navigationRail_chatSelected_dark() {
        snapshot("navigationRail_chatSelected", darkTheme = true) {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Medium,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    // ==================== Expanded (Navigation Drawer) ====================

    @Test
    @Config(qualifiers = "w1000dp-h800dp-xhdpi")
    fun navigationDrawer_chatSelected() {
        snapshot("navigationDrawer_chatSelected") {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Expanded,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp-xhdpi")
    fun navigationDrawer_careerSelected() {
        snapshot("navigationDrawer_careerSelected") {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Expanded,
                currentRoute = Route.CareerTimeline,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp-xhdpi")
    fun navigationDrawer_chatSelected_dark() {
        snapshot("navigationDrawer_chatSelected", darkTheme = true) {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Expanded,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    // ==================== Project Details (Career Highlighted) ====================

    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun bottomBar_projectDetails_careerHighlighted() {
        snapshot("bottomBar_projectDetails") {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Compact,
                currentRoute = Route.ProjectDetails("test-project"),
                onNavigate = {}
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}
```

**Step 3: Verify compilation**

Run: `./gradlew :shared-ui-screenshots:compileDebugUnitTestKotlin --quiet`
Expected: BUILD SUCCESSFUL

**Step 4: Record snapshots**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.AdaptiveNavigationScreenshots"`
Expected: Snapshots generated in `shared-ui-screenshots/src/test/snapshots/images/`

**Step 5: Commit**

```bash
git add shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/adaptive/
git add shared-ui-screenshots/src/test/snapshots/
git commit -m "test(adaptive): add adaptive navigation screenshot tests"
```

---

## Task 2: Add AdaptiveScaffold Layout Screenshot Tests

**Files:**
- Create: `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/adaptive/AdaptiveScaffoldScreenshots.kt`

**Step 1: Create AdaptiveScaffoldScreenshots.kt**

```kotlin
package io.github.devmugi.cv.agent.ui.screenshots.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.ui.adaptive.AdaptiveScaffold
import io.github.devmugi.cv.agent.ui.adaptive.WindowWidthSizeClass
import io.github.devmugi.cv.agent.ui.navigation.Route
import io.github.devmugi.cv.agent.ui.screenshots.ScreenshotTest
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * Screenshot tests for AdaptiveScaffold layout variations.
 *
 * Tests the complete scaffold with content at each breakpoint.
 */
class AdaptiveScaffoldScreenshots : ScreenshotTest() {

    @Test
    @Config(qualifiers = "w400dp-h800dp-xhdpi")
    fun scaffold_compact_withContent() {
        snapshot("scaffold_compact_withContent") {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Compact,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                ContentPlaceholder("Compact Layout")
            }
        }
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp-xhdpi")
    fun scaffold_medium_withContent() {
        snapshot("scaffold_medium_withContent") {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Medium,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                ContentPlaceholder("Medium Layout")
            }
        }
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp-xhdpi")
    fun scaffold_expanded_withContent() {
        snapshot("scaffold_expanded_withContent") {
            AdaptiveScaffold(
                windowSizeClass = WindowWidthSizeClass.Expanded,
                currentRoute = Route.Chat,
                onNavigate = {}
            ) {
                ContentPlaceholder("Expanded Layout")
            }
        }
    }

    @Composable
    private fun ContentPlaceholder(label: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ArcaneTheme.colors.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = ArcaneTheme.typography.headlineMedium,
                color = ArcaneTheme.colors.onSurface
            )
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :shared-ui-screenshots:compileDebugUnitTestKotlin --quiet`
Expected: BUILD SUCCESSFUL

**Step 3: Record snapshots**

Run: `./gradlew :shared-ui-screenshots:recordRoborazziDebug --tests "*.AdaptiveScaffoldScreenshots"`
Expected: Snapshots generated

**Step 4: Commit**

```bash
git add shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/adaptive/AdaptiveScaffoldScreenshots.kt
git add shared-ui-screenshots/src/test/snapshots/
git commit -m "test(adaptive): add AdaptiveScaffold layout screenshot tests"
```

---

## Task 3: Run Tests and Verification

**Step 1: Run all screenshot tests**

Run: `./gradlew :shared-ui-screenshots:testDebugUnitTest --quiet`
Expected: All tests pass

**Step 2: Verify snapshots were recorded**

Run: `ls -la shared-ui-screenshots/src/test/snapshots/images/ | grep -i adaptive | head -20`
Expected: Multiple snapshot files for adaptive tests

**Step 3: Run quality checks**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

---

## Task 4: Push and Summary

**Step 1: Push branch**

Run: `git push -u origin feature/phase-4-testing`

**Step 2: Verify commit log**

Run: `git log --oneline -10`

---

## Success Criteria Checklist

- [ ] AdaptiveNavigationScreenshots.kt created with 10 tests
- [ ] AdaptiveScaffoldScreenshots.kt created with 3 tests
- [ ] Bottom bar screenshots: Chat selected, Career selected, dark theme
- [ ] Navigation rail screenshots: Chat selected, Career selected, dark theme
- [ ] Navigation drawer screenshots: Chat selected, Career selected, dark theme
- [ ] ProjectDetails route highlights Career in nav
- [ ] All snapshots recorded
- [ ] All tests pass
- [ ] ktlint passes

---

*Plan created: 2026-01-31*
*Estimated commits: ~2-3*
