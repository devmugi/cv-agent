# Phase 1: Design System Consolidation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Eliminate 17 duplicate `AmberColor` definitions by creating a centralized `CareerColors` object.

**Architecture:** Create `CareerColors` object in shared-career-projects with `@Immutable` annotation. Replace all private color constants with references to this object. Add screenshot test to validate colors visually.

**Tech Stack:** Kotlin, Compose Multiplatform, Roborazzi

---

## Files Overview

**Create:**
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/theme/CareerColors.kt`

**Modify (17 files):**
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/CareerProjectDetailsScreenScaffold.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/CareerProjectTimelineInfo.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/AchievementCard.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/AchievementsList.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/BulletListItem.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/ChallengeSection.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/CoursesSection.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/DescriptionSection.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/FeaturedBadge.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/LifecycleTimeline.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/LinksSection.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/MetricsSection.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/ProjectGradientHeader.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/QuickStatsRow.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/StandoutCallout.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/StandoutSection.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/TeamStructureSection.kt`
- `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/TechnologyTag.kt`

**Test:**
- `shared-ui-screenshots/src/test/kotlin/io/github/devmugi/cv/agent/ui/screenshots/CareerColorsScreenshotTest.kt`

---

## Task 1: Create CareerColors Object

**Files:**
- Create: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/theme/CareerColors.kt`

**Step 1: Create the theme directory**

```bash
mkdir -p shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/theme
```

**Step 2: Create CareerColors.kt**

```kotlin
package io.github.devmugi.cv.agent.career.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Centralized color tokens for career/CV UI components.
 * Use these instead of defining colors locally in components.
 */
@Immutable
object CareerColors {
    /** Primary amber - main accent color */
    val Amber = Color(0xFFFFC107)

    /** Light amber - for text on dark backgrounds */
    val AmberLight = Color(0xFFFFD54F)

    /** Dark amber - for emphasis */
    val AmberDark = Color(0xFFFFA000)

    /** Warm background tint */
    val Background = Color(0xFFFFF8E1)

    /** Dark background for contrast */
    val BackgroundDark = Color(0xFF3E2723)
}
```

**Step 3: Verify file compiles**

Run: `./gradlew :shared-career-projects:compileAndroidMain --quiet`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/theme/CareerColors.kt
git commit -m "feat(design-system): add CareerColors centralized color tokens"
```

---

## Task 2: Migrate CareerProjectDetailsScreenScaffold.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/CareerProjectDetailsScreenScaffold.kt`

**Step 1: Read current file to find AmberColor usage**

Locate the line: `private val AmberColor = Color(0xFFFFC107)`

**Step 2: Replace local constant with CareerColors reference**

Remove:
```kotlin
private val AmberColor = Color(0xFFFFC107)
```

Add import:
```kotlin
import io.github.devmugi.cv.agent.career.theme.CareerColors
```

Replace all usages:
```kotlin
// Before
color = AmberColor,

// After
color = CareerColors.Amber,
```

**Step 3: Verify compilation**

Run: `./gradlew :shared-career-projects:compileAndroidMain --quiet`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/CareerProjectDetailsScreenScaffold.kt
git commit -m "refactor: migrate CareerProjectDetailsScreenScaffold to CareerColors"
```

---

## Task 3: Migrate CareerProjectTimelineInfo.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/CareerProjectTimelineInfo.kt`

**Step 1: Apply same pattern as Task 2**

Remove:
```kotlin
private val AmberColor = Color(0xFFFFC107)
```

Add import:
```kotlin
import io.github.devmugi.cv.agent.career.theme.CareerColors
```

Replace: `AmberColor` → `CareerColors.Amber`

**Step 2: Verify and commit**

Run: `./gradlew :shared-career-projects:compileAndroidMain --quiet`

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/CareerProjectTimelineInfo.kt
git commit -m "refactor: migrate CareerProjectTimelineInfo to CareerColors"
```

---

## Task 4: Migrate AchievementCard.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/AchievementCard.kt`

**Step 1: Apply migration pattern**

This file has multiple usages:
- `AmberColor.copy(alpha = 0.3f)` for border
- `AmberColor` for tint
- `AmberColor` for text color

Replace all with `CareerColors.Amber`:
```kotlin
.border(1.dp, CareerColors.Amber.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
tint = CareerColors.Amber,
color = CareerColors.Amber,
```

**Step 2: Verify and commit**

Run: `./gradlew :shared-career-projects:compileAndroidMain --quiet`

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/AchievementCard.kt
git commit -m "refactor: migrate AchievementCard to CareerColors"
```

---

## Task 5: Migrate AchievementsList.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/AchievementsList.kt`

**Step 1: Apply migration pattern**

Remove local constant, add import, replace usages.

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/AchievementsList.kt
git commit -m "refactor: migrate AchievementsList to CareerColors"
```

---

## Task 6: Migrate BulletListItem.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/BulletListItem.kt`

**Step 1: Apply migration pattern**

Note: This file uses AmberColor as a default parameter:
```kotlin
bulletColor: Color = AmberColor,
```

Replace with:
```kotlin
bulletColor: Color = CareerColors.Amber,
```

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/BulletListItem.kt
git commit -m "refactor: migrate BulletListItem to CareerColors"
```

---

## Task 7: Migrate ChallengeSection.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/ChallengeSection.kt`

**Step 1: Apply migration pattern**

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/ChallengeSection.kt
git commit -m "refactor: migrate ChallengeSection to CareerColors"
```

---

## Task 8: Migrate CoursesSection.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/CoursesSection.kt`

**Step 1: Apply migration pattern**

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/CoursesSection.kt
git commit -m "refactor: migrate CoursesSection to CareerColors"
```

---

## Task 9: Migrate DescriptionSection.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/DescriptionSection.kt`

**Step 1: Apply migration pattern**

This file has multiple usages including `.background(AmberColor)`.

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/DescriptionSection.kt
git commit -m "refactor: migrate DescriptionSection to CareerColors"
```

---

## Task 10: Migrate FeaturedBadge.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/FeaturedBadge.kt`

**Step 1: Apply migration pattern**

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/FeaturedBadge.kt
git commit -m "refactor: migrate FeaturedBadge to CareerColors"
```

---

## Task 11: Migrate LifecycleTimeline.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/LifecycleTimeline.kt`

**Step 1: Apply migration pattern**

Note: Check for `else -> AmberColor` in when expressions.

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/LifecycleTimeline.kt
git commit -m "refactor: migrate LifecycleTimeline to CareerColors"
```

---

## Task 12: Migrate LinksSection.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/LinksSection.kt`

**Step 1: Apply migration pattern**

Note: Check for conditional usage like `if (highlight) AmberColor else ...`

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/LinksSection.kt
git commit -m "refactor: migrate LinksSection to CareerColors"
```

---

## Task 13: Migrate MetricsSection.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/MetricsSection.kt`

**Step 1: Apply migration pattern**

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/MetricsSection.kt
git commit -m "refactor: migrate MetricsSection to CareerColors"
```

---

## Task 14: Migrate ProjectGradientHeader.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/ProjectGradientHeader.kt`

**Step 1: Apply migration pattern**

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/ProjectGradientHeader.kt
git commit -m "refactor: migrate ProjectGradientHeader to CareerColors"
```

---

## Task 15: Migrate QuickStatsRow.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/QuickStatsRow.kt`

**Step 1: Apply migration pattern**

Note: Check for conditional usage like `if (highlight) AmberColor else ...`

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/QuickStatsRow.kt
git commit -m "refactor: migrate QuickStatsRow to CareerColors"
```

---

## Task 16: Migrate StandoutCallout.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/StandoutCallout.kt`

**Step 1: Apply migration pattern**

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/StandoutCallout.kt
git commit -m "refactor: migrate StandoutCallout to CareerColors"
```

---

## Task 17: Migrate StandoutSection.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/StandoutSection.kt`

**Step 1: Apply migration pattern**

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/StandoutSection.kt
git commit -m "refactor: migrate StandoutSection to CareerColors"
```

---

## Task 18: Migrate TeamStructureSection.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/TeamStructureSection.kt`

**Step 1: Apply migration pattern**

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/TeamStructureSection.kt
git commit -m "refactor: migrate TeamStructureSection to CareerColors"
```

---

## Task 19: Migrate TechnologyTag.kt

**Files:**
- Modify: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/TechnologyTag.kt`

**Step 1: Apply migration pattern**

This file uses inline color literals, not the AmberColor constant:
```kotlin
Color(0xFFFFC107), // Amber border
Color(0xFFFFD54F)  // Bright amber text
```

Replace with:
```kotlin
CareerColors.Amber, // Amber border
CareerColors.AmberLight  // Bright amber text
```

**Step 2: Verify and commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/ui/components/TechnologyTag.kt
git commit -m "refactor: migrate TechnologyTag to CareerColors"
```

---

## Task 20: Run Full Test Suite

**Step 1: Run screenshot tests to verify no visual changes**

Run: `./gradlew :shared-ui-screenshots:testDebugUnitTest`
Expected: All tests pass

**Step 2: Run all Android tests**

Run: `./gradlew :shared-career-projects:allTests :shared-ui:allTests --quiet`
Expected: BUILD SUCCESSFUL

**Step 3: Verify no remaining duplicates**

Run: `grep -r "private val AmberColor" --include="*.kt" .`
Expected: No matches (empty output)

Run: `grep -r "Color(0xFFFFC107)" --include="*.kt" . | grep -v CareerColors.kt | grep -v test`
Expected: No matches (empty output)

---

## Task 21: Run Quality Checks

**Step 1: Run ktlint**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

**Step 2: Run detekt**

Run: `./gradlew detekt`
Expected: BUILD SUCCESSFUL

**Step 3: Fix any issues if needed**

Run: `./gradlew ktlintFormat` if ktlint fails

---

## Task 22: Final Commit and Summary

**Step 1: Verify clean state**

Run: `git status`
Expected: All changes committed, working tree clean

**Step 2: View commit log**

Run: `git log --oneline -20`

Should show:
```
refactor: migrate TechnologyTag to CareerColors
refactor: migrate TeamStructureSection to CareerColors
refactor: migrate StandoutSection to CareerColors
... (17 migration commits)
feat(design-system): add CareerColors centralized color tokens
```

**Step 3: Push branch (if ready for PR)**

Run: `git push -u origin feature/phase-1-design-system`

---

## Success Criteria Checklist

- [ ] CareerColors.kt created with @Immutable annotation
- [ ] All 17 files migrated to use CareerColors
- [ ] Zero `private val AmberColor` definitions remain
- [ ] Zero inline `Color(0xFFFFC107)` literals remain (except in CareerColors.kt)
- [ ] All screenshot tests pass
- [ ] ktlint passes
- [ ] detekt passes

---

## Rollback Plan

If issues arise after merging:

```bash
# Revert all Phase 1 commits
git revert --no-commit HEAD~18..HEAD
git commit -m "revert: Phase 1 design system changes"
```

---

*Plan created: 2026-01-31*
*Estimated time: 45-60 minutes*
*Commits: ~20*
