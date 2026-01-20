# Phase 4: UI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the complete chat UI connecting to the existing ChatViewModel with markdown support, reference chip tooltips, and smooth animations.

**Architecture:** Compose Multiplatform components observing ChatState via StateFlow. LazyColumn for messages with reverse layout. Individual composable components for each UI element.

**Tech Stack:** Compose Multiplatform, Material3, multiplatform-markdown-renderer, Koin DI

---

## Task 1: Add Missing Theme Colors

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Color.kt`

**Step 1: Add error container colors to theme**

Add these colors for error states and reference chip backgrounds:

```kotlin
// Add after line 13 (after LightGray)
val ErrorContainer = Color(0xFF3D2936)
val OnErrorContainer = Color(0xFFFFB4AB)
val ReferenceChipBg = Color(0xFF2A3A5C)
val CodeBlockBg = Color(0xFF252A3D)
```

**Step 2: Update color scheme with new tokens**

Update CVAgentColorScheme to include error container colors (replace lines 34-35):

```kotlin
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)
```

**Step 3: Run build to verify**

Run: `./gradlew compileKotlinIosSimulatorArm64 --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/theme/Color.kt
git commit -m "feat(ui): add error container and reference chip colors to theme"
```

---

## Task 2: Create TopBar Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/TopBar.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/TopBarTest.kt`

**Step 1: Write the failing test**

Create test file:

```kotlin
package io.github.devmugi.cv.agent.ui.components

import kotlin.test.Test
import kotlin.test.assertTrue

class TopBarTest {

    @Test
    fun topBarTitleContainsBrandName() {
        // TopBar should display "<DH/> CV Agent"
        val title = buildTopBarTitle()
        assertTrue(title.contains("<DH/>"))
        assertTrue(title.contains("CV Agent"))
    }
}

// Placeholder for compilation
fun buildTopBarTitle(): String = TODO("Not implemented")
```

**Step 2: Run test to verify it fails**

Run: `./gradlew allTests --no-daemon 2>&1 | grep -A5 "TopBarTest"`
Expected: FAIL with NotImplementedError

**Step 3: Write the TopBar component**

Create component file:

```kotlin
package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun buildTopBarTitle(): String = "<DH/> CV Agent"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CVAgentTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "<DH/>",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CV Agent",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew allTests --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/TopBar.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/TopBarTest.kt
git commit -m "feat(ui): add TopBar component with branded title"
```

---

## Task 3: Create SuggestionChip Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChip.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChipTest.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SuggestionChipTest {

    @Test
    fun suggestionChipCallbackReceivesCorrectText() {
        var clickedText: String? = null
        val testText = "What's Denys's experience?"

        // Simulate click callback
        val callback: (String) -> Unit = { clickedText = it }
        simulateSuggestionChipClick(testText, callback)

        assertEquals(testText, clickedText)
    }

    @Test
    fun suggestionChipTextIsDisplayed() {
        val text = "Tell me about skills"
        val displayText = getSuggestionChipDisplayText(text)
        assertEquals(text, displayText)
    }
}

// Placeholders
fun simulateSuggestionChipClick(text: String, onClick: (String) -> Unit): Unit = TODO()
fun getSuggestionChipDisplayText(text: String): String = TODO()
```

**Step 2: Run test to verify it fails**

Run: `./gradlew allTests --no-daemon 2>&1 | grep -A5 "SuggestionChipTest"`
Expected: FAIL

**Step 3: Write the SuggestionChip component**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun simulateSuggestionChipClick(text: String, onClick: (String) -> Unit) {
    onClick(text)
}

fun getSuggestionChipDisplayText(text: String): String = text

@Composable
fun SuggestionChip(
    text: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onClick(text) },
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew allTests --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChip.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChipTest.kt
git commit -m "feat(ui): add SuggestionChip component with outlined style"
```

---

## Task 4: Create SuggestionChipsGrid Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChipsGrid.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChipsGridTest.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SuggestionChipsGridTest {

    @Test
    fun gridLayoutReturnsCorrectRowCount() {
        val suggestions = listOf("A", "B", "C", "D")
        val rows = calculateGridRows(suggestions, columnsPerRow = 2)
        assertEquals(2, rows)
    }

    @Test
    fun gridLayoutHandlesOddCount() {
        val suggestions = listOf("A", "B", "C")
        val rows = calculateGridRows(suggestions, columnsPerRow = 2)
        assertEquals(2, rows) // 3 items = 2 rows with 2 columns
    }

    @Test
    fun emptyListReturnsZeroRows() {
        val rows = calculateGridRows(emptyList(), columnsPerRow = 2)
        assertEquals(0, rows)
    }
}

fun calculateGridRows(items: List<String>, columnsPerRow: Int): Int = TODO()
```

**Step 2: Run test to verify it fails**

Run: `./gradlew allTests --no-daemon 2>&1 | grep -A5 "SuggestionChipsGridTest"`
Expected: FAIL

**Step 3: Write the SuggestionChipsGrid component**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun calculateGridRows(items: List<String>, columnsPerRow: Int): Int {
    if (items.isEmpty() || columnsPerRow <= 0) return 0
    return (items.size + columnsPerRow - 1) / columnsPerRow
}

@Composable
fun SuggestionChipsGrid(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = androidx.compose.ui.Alignment.CenterHorizontally)
            ) {
                rowItems.forEach { suggestion ->
                    SuggestionChip(
                        text = suggestion,
                        onClick = onSuggestionClick
                    )
                }
            }
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew allTests --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChipsGrid.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/SuggestionChipsGridTest.kt
git commit -m "feat(ui): add SuggestionChipsGrid with 2x2 layout"
```

---

## Task 5: Create ErrorMessage Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ErrorMessage.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/ErrorMessageTest.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.agent.ChatError
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessageTest {

    @Test
    fun networkErrorDisplaysCorrectMessage() {
        val error = ChatError.Network("Connection failed")
        val message = getErrorDisplayMessage(error)
        assertEquals("Connection failed", message)
    }

    @Test
    fun rateLimitErrorDisplaysUserFriendlyMessage() {
        val error = ChatError.RateLimit
        val message = getErrorDisplayMessage(error)
        assertEquals("Too many requests. Please wait a moment.", message)
    }

    @Test
    fun apiErrorDisplaysMessage() {
        val error = ChatError.Api("Server error")
        val message = getErrorDisplayMessage(error)
        assertEquals("Server error", message)
    }
}

fun getErrorDisplayMessage(error: ChatError): String = TODO()
```

**Step 2: Run test to verify it fails**

Run: `./gradlew allTests --no-daemon 2>&1 | grep -A5 "ErrorMessageTest"`
Expected: FAIL

**Step 3: Write the ErrorMessage component**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.devmugi.cv.agent.agent.ChatError

fun getErrorDisplayMessage(error: ChatError): String = when (error) {
    is ChatError.Network -> error.message
    is ChatError.Api -> error.message
    ChatError.RateLimit -> "Too many requests. Please wait a moment."
}

@Composable
fun ErrorMessage(
    error: ChatError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = getErrorDisplayMessage(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Retry")
            }
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew allTests --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ErrorMessage.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/ErrorMessageTest.kt
git commit -m "feat(ui): add ErrorMessage component with retry button"
```

---

## Task 6: Create ReferenceChip with Tooltip

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ReferenceChip.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/ReferenceChipTest.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.data.models.CVReference
import kotlin.test.Test
import kotlin.test.assertEquals

class ReferenceChipTest {

    @Test
    fun referenceChipDisplaysTypeOnly() {
        val reference = CVReference(id = "kotlin", type = "Skill", label = "Kotlin")
        val display = formatReferenceChipText(reference)
        assertEquals("Skill: Kotlin", display)
    }

    @Test
    fun referenceChipDisplaysExperienceWithCompany() {
        val reference = CVReference(id = "takeaway", type = "Experience", label = "Takeaway")
        val display = formatReferenceChipText(reference)
        assertEquals("Experience: Takeaway", display)
    }

    @Test
    fun referenceChipWithEmptyLabelShowsTypeOnly() {
        val reference = CVReference(id = "summary", type = "Summary", label = "")
        val display = formatReferenceChipText(reference)
        assertEquals("Summary", display)
    }
}

fun formatReferenceChipText(reference: CVReference): String = TODO()
```

**Step 2: Run test to verify it fails**

Run: `./gradlew allTests --no-daemon 2>&1 | grep -A5 "ReferenceChipTest"`
Expected: FAIL

**Step 3: Write the ReferenceChip component**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.github.devmugi.cv.agent.data.models.CVReference
import io.github.devmugi.cv.agent.ui.theme.ReferenceChipBg

fun formatReferenceChipText(reference: CVReference): String {
    return if (reference.label.isNotEmpty()) {
        "${reference.type}: ${reference.label}"
    } else {
        reference.type
    }
}

@Composable
fun ReferenceChip(
    reference: CVReference,
    tooltipContent: String? = null,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { if (tooltipContent != null) showTooltip = !showTooltip },
            shape = MaterialTheme.shapes.small,
            color = ReferenceChipBg
        ) {
            Text(
                text = formatReferenceChipText(reference),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (showTooltip && tooltipContent != null) {
            Popup(
                onDismissRequest = { showTooltip = false }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = tooltipContent,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew allTests --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/ReferenceChip.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/ReferenceChipTest.kt
git commit -m "feat(ui): add ReferenceChip component with tooltip popup"
```

---

## Task 7: Create TooltipProvider for CV Data Lookup

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/TooltipProvider.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/TooltipProviderTest.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.data.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TooltipProviderTest {

    private val testCVData = CVData(
        personalInfo = PersonalInfo(
            name = "Test User",
            location = "Test City",
            email = "test@test.com",
            phone = "123",
            linkedin = "linkedin",
            github = "github",
            portfolio = "portfolio"
        ),
        summary = "Test summary",
        skills = listOf(
            SkillCategory(id = "android", category = "Android", skills = listOf("Kotlin", "Java"))
        ),
        experience = listOf(
            WorkExperience(
                id = "takeaway",
                title = "Senior Developer",
                company = "Takeaway",
                period = "2020-2023",
                description = "Built mobile apps",
                highlights = listOf("Led team"),
                technologies = listOf("Kotlin")
            )
        ),
        projects = listOf(
            Project(
                id = "cv-agent",
                name = "CV Agent",
                type = "Mobile",
                description = "Portfolio app",
                technologies = listOf("KMP")
            )
        ),
        achievements = listOf(
            Achievement(
                id = "award1",
                title = "Best App",
                year = "2022",
                description = "Won award"
            )
        ),
        education = Education(degree = "BSc", field = "CS", institution = "University")
    )

    @Test
    fun experienceTooltipReturnsCompanyAndPeriod() {
        val tooltip = getTooltipForReference(
            CVReference(id = "takeaway", type = "Experience", label = "Takeaway"),
            testCVData
        )
        assertEquals("Senior Developer at Takeaway (2020-2023)", tooltip)
    }

    @Test
    fun skillTooltipReturnsSkillList() {
        val tooltip = getTooltipForReference(
            CVReference(id = "android", type = "Skill", label = "Android"),
            testCVData
        )
        assertEquals("Android: Kotlin, Java", tooltip)
    }

    @Test
    fun unknownReferenceReturnsNull() {
        val tooltip = getTooltipForReference(
            CVReference(id = "unknown", type = "Unknown", label = "X"),
            testCVData
        )
        assertNull(tooltip)
    }
}

fun getTooltipForReference(reference: CVReference, cvData: CVData?): String? = TODO()
```

**Step 2: Run test to verify it fails**

Run: `./gradlew allTests --no-daemon 2>&1 | grep -A5 "TooltipProviderTest"`
Expected: FAIL

**Step 3: Write the TooltipProvider**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.models.CVReference

fun getTooltipForReference(reference: CVReference, cvData: CVData?): String? {
    if (cvData == null) return null

    return when (reference.type.lowercase()) {
        "experience" -> {
            cvData.experience.find { it.id == reference.id }?.let { exp ->
                "${exp.title} at ${exp.company} (${exp.period})"
            }
        }
        "skill" -> {
            cvData.skills.find { it.id == reference.id }?.let { skill ->
                "${skill.category}: ${skill.skills.joinToString(", ")}"
            }
        }
        "project" -> {
            cvData.projects.find { it.id == reference.id }?.let { project ->
                "${project.name}: ${project.description}"
            }
        }
        "achievement" -> {
            cvData.achievements.find { it.id == reference.id }?.let { achievement ->
                "${achievement.title} (${achievement.year})"
            }
        }
        "summary" -> cvData.summary.take(100) + if (cvData.summary.length > 100) "..." else ""
        else -> null
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew allTests --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/TooltipProvider.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/TooltipProviderTest.kt
git commit -m "feat(ui): add TooltipProvider for CV data lookup"
```

---

## Task 8: Create MessageBubble Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageBubble.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/MessageBubbleTest.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.agent.MessageRole
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageBubbleTest {

    @Test
    fun userMessageAlignmentIsEnd() {
        val alignment = getMessageAlignment(MessageRole.USER)
        assertEquals(MessageAlignment.END, alignment)
    }

    @Test
    fun assistantMessageAlignmentIsStart() {
        val alignment = getMessageAlignment(MessageRole.ASSISTANT)
        assertEquals(MessageAlignment.START, alignment)
    }

    @Test
    fun userMessageUsesRoundedTopRightSmall() {
        val shape = getMessageCornerShape(MessageRole.USER)
        assertEquals(MessageCornerShape.TOP_RIGHT_SMALL, shape)
    }

    @Test
    fun assistantMessageUsesRoundedTopLeftSmall() {
        val shape = getMessageCornerShape(MessageRole.ASSISTANT)
        assertEquals(MessageCornerShape.TOP_LEFT_SMALL, shape)
    }
}

enum class MessageAlignment { START, END }
enum class MessageCornerShape { TOP_RIGHT_SMALL, TOP_LEFT_SMALL }

fun getMessageAlignment(role: MessageRole): MessageAlignment = TODO()
fun getMessageCornerShape(role: MessageRole): MessageCornerShape = TODO()
```

**Step 2: Run test to verify it fails**

Run: `./gradlew allTests --no-daemon 2>&1 | grep -A5 "MessageBubbleTest"`
Expected: FAIL

**Step 3: Write the MessageBubble component**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import io.github.devmugi.cv.agent.agent.Message
import io.github.devmugi.cv.agent.agent.MessageRole
import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.models.CVReference

enum class MessageAlignment { START, END }
enum class MessageCornerShape { TOP_RIGHT_SMALL, TOP_LEFT_SMALL }

fun getMessageAlignment(role: MessageRole): MessageAlignment = when (role) {
    MessageRole.USER -> MessageAlignment.END
    MessageRole.ASSISTANT, MessageRole.SYSTEM -> MessageAlignment.START
}

fun getMessageCornerShape(role: MessageRole): MessageCornerShape = when (role) {
    MessageRole.USER -> MessageCornerShape.TOP_RIGHT_SMALL
    MessageRole.ASSISTANT, MessageRole.SYSTEM -> MessageCornerShape.TOP_LEFT_SMALL
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: Message,
    cvData: CVData? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = backgroundColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                } else {
                    Markdown(
                        content = message.content,
                        modifier = Modifier
                    )
                }

                if (message.references.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        message.references.forEach { reference ->
                            ReferenceChip(
                                reference = reference,
                                tooltipContent = getTooltipForReference(reference, cvData)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Markdown(
                content = content,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew allTests --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageBubble.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/MessageBubbleTest.kt
git commit -m "feat(ui): add MessageBubble with markdown and reference chips"
```

---

## Task 9: Create MessageInput Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageInput.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/MessageInputTest.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageInputTest {

    @Test
    fun sendButtonEnabledWhenTextNotEmpty() {
        val enabled = isSendButtonEnabled(text = "Hello", isLoading = false)
        assertTrue(enabled)
    }

    @Test
    fun sendButtonDisabledWhenTextEmpty() {
        val enabled = isSendButtonEnabled(text = "", isLoading = false)
        assertFalse(enabled)
    }

    @Test
    fun sendButtonDisabledWhenLoading() {
        val enabled = isSendButtonEnabled(text = "Hello", isLoading = true)
        assertFalse(enabled)
    }

    @Test
    fun sendButtonDisabledWhenTextIsBlank() {
        val enabled = isSendButtonEnabled(text = "   ", isLoading = false)
        assertFalse(enabled)
    }
}

fun isSendButtonEnabled(text: String, isLoading: Boolean): Boolean = TODO()
```

**Step 2: Run test to verify it fails**

Run: `./gradlew allTests --no-daemon 2>&1 | grep -A5 "MessageInputTest"`
Expected: FAIL

**Step 3: Write the MessageInput component**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

fun isSendButtonEnabled(text: String, isLoading: Boolean): Boolean {
    return text.isNotBlank() && !isLoading
}

@Composable
fun MessageInput(
    onSend: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val canSend = isSendButtonEnabled(text, isLoading)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .alpha(if (isLoading) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                placeholder = {
                    Text(
                        text = "Ask about my experience...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) {
                            onSend(text)
                            text = ""
                        }
                    }
                ),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (canSend) {
                        onSend(text)
                        text = ""
                    }
                },
                enabled = canSend,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew allTests --no-daemon 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/MessageInput.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/ui/components/MessageInputTest.kt
git commit -m "feat(ui): add MessageInput component with disabled state"
```

---

## Task 10: Create WelcomeSection Component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/WelcomeSection.kt`

**Step 1: Write the WelcomeSection component**

```kotlin
package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeSection(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "I'm Denys's AI assistant. Ask me anything about his professional experience, skills, or projects.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        SuggestionChipsGrid(
            suggestions = suggestions,
            onSuggestionClick = onSuggestionClick
        )
    }
}
```

**Step 2: Run build to verify**

Run: `./gradlew compileKotlinIosSimulatorArm64 --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/WelcomeSection.kt
git commit -m "feat(ui): add WelcomeSection with centered layout"
```

---

## Task 11: Update ChatScreen to Integrate All Components

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt`

**Step 1: Rewrite ChatScreen with full integration**

Replace the entire file:

```kotlin
package io.github.devmugi.cv.agent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.devmugi.cv.agent.agent.ChatState
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.ui.components.CVAgentTopBar
import io.github.devmugi.cv.agent.ui.components.ErrorMessage
import io.github.devmugi.cv.agent.ui.components.MessageBubble
import io.github.devmugi.cv.agent.ui.components.MessageInput
import io.github.devmugi.cv.agent.ui.components.StreamingMessageBubble
import io.github.devmugi.cv.agent.ui.components.WelcomeSection
import io.github.devmugi.cv.agent.ui.theme.CVAgentTheme

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    cvData: CVData? = null
) {
    val state by viewModel.state.collectAsState()

    CVAgentTheme {
        ChatScreenContent(
            state = state,
            cvData = cvData,
            onSendMessage = viewModel::sendMessage,
            onSuggestionClick = viewModel::onSuggestionClicked,
            onRetry = viewModel::retry
        )
    }
}

@Composable
private fun ChatScreenContent(
    state: ChatState,
    cvData: CVData?,
    onSendMessage: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    val listState = rememberLazyListState()
    val showWelcome = state.messages.isEmpty() && !state.isLoading && !state.isStreaming

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size, state.streamingContent) {
        if (state.messages.isNotEmpty() || state.isStreaming) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = { CVAgentTopBar() },
        bottomBar = {
            MessageInput(
                onSend = onSendMessage,
                isLoading = state.isLoading || state.isStreaming
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showWelcome) {
                WelcomeSection(
                    suggestions = state.suggestions,
                    onSuggestionClick = onSuggestionClick,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    reverseLayout = true,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Error message at top (bottom visually due to reverse)
                    state.error?.let { error ->
                        item(key = "error") {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                ErrorMessage(
                                    error = error,
                                    onRetry = onRetry,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Streaming message (appears at bottom, first in reversed list)
                    if (state.isStreaming && state.streamingContent.isNotEmpty()) {
                        item(key = "streaming") {
                            StreamingMessageBubble(content = state.streamingContent)
                        }
                    }

                    // Messages in reverse order (newest first in list = bottom visually)
                    items(
                        items = state.messages.reversed(),
                        key = { it.id }
                    ) { message ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            MessageBubble(
                                message = message,
                                cvData = cvData
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Step 2: Run build to verify**

Run: `./gradlew compileKotlinIosSimulatorArm64 --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/ChatScreen.kt
git commit -m "feat(ui): integrate ChatScreen with all components"
```

---

## Task 12: Update Android Entry Point

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

**Step 1: Read current MainActivity**

First check the current implementation to understand what needs updating.

**Step 2: Update MainActivity to use ChatViewModel**

The MainActivity should:
1. Initialize Koin (if not already)
2. Get ChatViewModel from Koin
3. Get CVData from repository
4. Pass both to ChatScreen

```kotlin
package io.github.devmugi.cv.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.data.repository.CVRepository
import io.github.devmugi.cv.agent.ui.ChatScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by inject()
    private val repository: CVRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cvData = remember { repository.getCVData() }
            ChatScreen(
                viewModel = viewModel,
                cvData = cvData
            )
        }
    }
}
```

**Step 3: Run Android build to verify**

Run: `./gradlew assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/androidMain/kotlin/io/github/devmugi/cv/agent/MainActivity.kt
git commit -m "feat(android): connect MainActivity to ChatViewModel"
```

---

## Task 13: Update iOS Entry Point

**Files:**
- Modify: `composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt`

**Step 1: Read current MainViewController**

Check current iOS entry point implementation.

**Step 2: Update MainViewController**

```kotlin
package io.github.devmugi.cv.agent

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.data.repository.CVRepository
import io.github.devmugi.cv.agent.ui.ChatScreen
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ViewControllerFactory : KoinComponent {
    private val viewModel: ChatViewModel by inject()
    private val repository: CVRepository by inject()

    fun create() = ComposeUIViewController {
        val cvData = remember { repository.getCVData() }
        ChatScreen(
            viewModel = viewModel,
            cvData = cvData
        )
    }
}

fun MainViewController() = ViewControllerFactory.create()
```

**Step 3: Run iOS build to verify**

Run: `./gradlew linkDebugFrameworkIosSimulatorArm64 --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/iosMain/kotlin/io/github/devmugi/cv/agent/MainViewController.kt
git commit -m "feat(ios): connect MainViewController to ChatViewModel"
```

---

## Task 14: Run Full Test Suite and Quality Gates

**Files:** None (verification only)

**Step 1: Run all tests**

Run: `./gradlew allTests --no-daemon`
Expected: BUILD SUCCESSFUL with all tests passing

**Step 2: Run ktlint**

Run: `./gradlew ktlintCheck --no-daemon`
Expected: BUILD SUCCESSFUL (no violations)

**Step 3: Run detekt**

Run: `./gradlew detekt --no-daemon`
Expected: BUILD SUCCESSFUL (no issues)

**Step 4: Build both platforms**

Run: `./gradlew assembleDebug linkDebugFrameworkIosSimulatorArm64 --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 5: Commit verification**

```bash
git commit --allow-empty -m "chore: verify Phase 4 quality gates pass"
```

---

## Task 15: Final Integration Verification

**Files:** None (manual verification)

**Step 1: Count new test files**

Run: `find composeApp/src/commonTest -name "*Test.kt" -path "*/ui/*" | wc -l`
Expected: 7 or more test files

**Step 2: Count new component files**

Run: `find composeApp/src/commonMain -name "*.kt" -path "*/ui/components/*" | wc -l`
Expected: 8 or more component files

**Step 3: Verify line counts are reasonable**

Run: `wc -l composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/*.kt`
Expected: ~400-500 lines total

**Step 4: Create completion commit**

```bash
git commit --allow-empty -m "docs: Phase 4 UI implementation complete

Components implemented:
- TopBar with branded title
- MessageBubble with markdown rendering
- MessageInput with disabled state
- SuggestionChip and SuggestionChipsGrid
- ReferenceChip with tooltip popup
- ErrorMessage with retry button
- WelcomeSection with centered layout
- ChatScreen integrating all components

Quality gates:
- All tests passing
- ktlint clean
- detekt clean
- Android and iOS builds successful"
```

---

## Summary

| Task | Component | LOC Est. | Tests |
|------|-----------|----------|-------|
| 1 | Theme colors | ~10 | - |
| 2 | TopBar | ~30 | 1 |
| 3 | SuggestionChip | ~30 | 2 |
| 4 | SuggestionChipsGrid | ~35 | 3 |
| 5 | ErrorMessage | ~50 | 3 |
| 6 | ReferenceChip | ~50 | 3 |
| 7 | TooltipProvider | ~30 | 3 |
| 8 | MessageBubble | ~80 | 4 |
| 9 | MessageInput | ~80 | 4 |
| 10 | WelcomeSection | ~35 | - |
| 11 | ChatScreen | ~90 | - |
| 12 | Android entry | ~20 | - |
| 13 | iOS entry | ~20 | - |
| 14-15 | Verification | - | - |

**Total: ~560 LOC, 23 new tests, 15 commits**
