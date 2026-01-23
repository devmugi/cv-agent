# Agent Improvements Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace old CV data system with rich project data from shared-career-projects, add LLM project suggestions.

**Architecture:** Personal info + skills in new JSON file. System prompt contains project index (all 13) + curated details for 5 featured projects. LLM outputs JSON suggestion block at end of responses, parsed into tappable chips.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization, Koin DI, Compose Resources

---

## Task 1: Create personal_info.json

**Files:**
- Create: `shared-career-projects/src/commonMain/composeResources/files/personal_info.json`

**Step 1: Create the JSON file**

```json
{
  "name": "Denys Honcharenko",
  "title": "Senior Android Engineer",
  "location": "Genk, Belgium",
  "email": "aidevmugi@gmail.com",
  "linkedin": "https://www.linkedin.com/in/denyshoncharenko/",
  "github": "https://github.com/devmugi",
  "portfolio": "https://devmugi.github.io/devmugi/",
  "summary": "Result-oriented Software Engineer with 15+ years of experience building apps that reach 100M+ users across 60+ countries. Specializing in Kotlin Multiplatform, Jetpack Compose, IoT solutions, and AI-powered development.",
  "skills": [
    { "category": "AI-Powered Development", "items": ["Claude Code", "MCP Server", "Superpowers", "spec-kit", "Koog", "AI Prompting"] },
    { "category": "Kotlin Multiplatform", "items": ["KMM", "Compose Multiplatform", "Coroutines", "Ktor"] },
    { "category": "Android Development", "items": ["Android SDK", "Kotlin", "Java", "Jetpack Compose", "Android TV"] },
    { "category": "Architecture", "items": ["MVVM", "MVP", "MVI", "Clean Architecture"] },
    { "category": "IoT & Hardware", "items": ["Bluetooth/BLE", "MQTT", "OpenHab", "Alexa Skills", "AWS IoT"] },
    { "category": "Enterprise & MDM", "items": ["Kiosk Mode", "Samsung Knox", "Android Enterprise"] },
    { "category": "Backend", "items": ["Spring Boot", "Kafka", "REST API", "Ktor", "Kubernetes"] },
    { "category": "Cloud & DevOps", "items": ["AWS", "Firebase", "CI/CD", "Docker"] }
  ]
}
```

**Step 2: Verify file created**

Run: `cat shared-career-projects/src/commonMain/composeResources/files/personal_info.json | head -20`

**Step 3: Commit**

```bash
git add shared-career-projects/src/commonMain/composeResources/files/personal_info.json
git commit -m "feat(agent): add personal_info.json for agent context"
```

---

## Task 2: Create PersonalInfo Model

**Files:**
- Create: `shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/models/PersonalInfo.kt`
- Test: `shared-career-projects/src/commonTest/kotlin/io/github/devmugi/cv/agent/career/models/PersonalInfoTest.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.career.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonalInfoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun deserializesPersonalInfo() {
        val jsonString = """
        {
          "name": "Test Name",
          "title": "Test Title",
          "location": "Test Location",
          "email": "test@example.com",
          "linkedin": "https://linkedin.com/test",
          "github": "https://github.com/test",
          "portfolio": "https://test.com",
          "summary": "Test summary",
          "skills": [
            { "category": "Category1", "items": ["Skill1", "Skill2"] }
          ]
        }
        """.trimIndent()

        val result = json.decodeFromString<PersonalInfo>(jsonString)

        assertEquals("Test Name", result.name)
        assertEquals("Test Title", result.title)
        assertEquals(1, result.skills.size)
        assertEquals("Category1", result.skills[0].category)
        assertEquals(listOf("Skill1", "Skill2"), result.skills[0].items)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared-career-projects:compileKotlinAndroid`
Expected: FAIL with "Unresolved reference: PersonalInfo"

**Step 3: Write the model**

```kotlin
package io.github.devmugi.cv.agent.career.models

import kotlinx.serialization.Serializable

@Serializable
data class PersonalInfo(
    val name: String,
    val title: String,
    val location: String,
    val email: String,
    val linkedin: String,
    val github: String,
    val portfolio: String,
    val summary: String,
    val skills: List<SkillCategory>
)

@Serializable
data class SkillCategory(
    val category: String,
    val items: List<String>
)
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared-career-projects:testDebugUnitTest --tests "*.PersonalInfoTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared-career-projects/src/commonMain/kotlin/io/github/devmugi/cv/agent/career/models/PersonalInfo.kt
git add shared-career-projects/src/commonTest/kotlin/io/github/devmugi/cv/agent/career/models/PersonalInfoTest.kt
git commit -m "feat(agent): add PersonalInfo model with tests"
```

---

## Task 3: Create SuggestionExtractor

**Files:**
- Create: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/SuggestionExtractor.kt`
- Test: `shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/SuggestionExtractorTest.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SuggestionExtractorTest {

    private val extractor = SuggestionExtractor()

    @Test
    fun extractsSuggestionsFromJsonBlock() {
        val input = """
            Denys has extensive experience with BLE and IoT.

            ```json
            {"suggestions": ["adidas-gmr", "geosatis"]}
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertEquals("Denys has extensive experience with BLE and IoT.", result.cleanedContent.trim())
        assertEquals(listOf("adidas-gmr", "geosatis"), result.suggestions)
    }

    @Test
    fun handlesEmptySuggestions() {
        val input = """
            Just a regular response.

            ```json
            {"suggestions": []}
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertEquals("Just a regular response.", result.cleanedContent.trim())
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun handlesNoJsonBlock() {
        val input = "Response without any JSON block."

        val result = extractor.extract(input)

        assertEquals(input, result.cleanedContent)
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun handlesMalformedJson() {
        val input = """
            Some text.

            ```json
            {not valid json}
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun handlesSingleSuggestion() {
        val input = """
            Response text.

            ```json
            {"suggestions": ["mcdonalds"]}
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertEquals(listOf("mcdonalds"), result.suggestions)
    }

    @Test
    fun handlesWhitespaceVariations() {
        val input = """
            Response text.

            ```json
            {
              "suggestions": [
                "adidas-gmr",
                "geosatis"
              ]
            }
            ```
        """.trimIndent()

        val result = extractor.extract(input)

        assertEquals(listOf("adidas-gmr", "geosatis"), result.suggestions)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: FAIL with "Unresolved reference: SuggestionExtractor"

**Step 3: Write the implementation**

```kotlin
package io.github.devmugi.cv.agent.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SuggestionsPayload(
    val suggestions: List<String> = emptyList()
)

data class SuggestionResult(
    val cleanedContent: String,
    val suggestions: List<String>
)

class SuggestionExtractor {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonBlockPattern = Regex(
        """```json\s*(\{[\s\S]*?\})\s*```""",
        RegexOption.MULTILINE
    )

    fun extract(content: String): SuggestionResult {
        val match = jsonBlockPattern.find(content)
            ?: return SuggestionResult(content, emptyList())

        val jsonStr = match.groupValues[1]
        val suggestions = try {
            json.decodeFromString<SuggestionsPayload>(jsonStr).suggestions
        } catch (_: Exception) {
            emptyList()
        }

        val cleanedContent = content.replace(match.value, "").trim()
        return SuggestionResult(cleanedContent, suggestions)
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testDebugUnitTest --tests "*.SuggestionExtractorTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/SuggestionExtractor.kt
git add shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/SuggestionExtractorTest.kt
git commit -m "feat(agent): add SuggestionExtractor for JSON suggestion parsing"
```

---

## Task 4: Create AgentDataProvider

**Files:**
- Create: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/AgentDataProvider.kt`
- Test: `shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/AgentDataProviderTest.kt`

**Step 1: Write the failing test**

```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.Description
import io.github.devmugi.cv.agent.career.models.Overview
import io.github.devmugi.cv.agent.career.models.Period
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.career.models.SkillCategory
import io.github.devmugi.cv.agent.career.models.Technologies
import io.github.devmugi.cv.agent.career.models.Technology
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentDataProviderTest {

    private val testPersonalInfo = PersonalInfo(
        name = "Test Name",
        title = "Test Title",
        location = "Test Location",
        email = "test@test.com",
        linkedin = "https://linkedin.com",
        github = "https://github.com",
        portfolio = "https://portfolio.com",
        summary = "Test summary",
        skills = listOf(SkillCategory("Category", listOf("Skill1", "Skill2")))
    )

    private val testProject = CareerProject(
        id = "test-project",
        name = "Test Project",
        slug = "test-project",
        tagline = "Test tagline",
        overview = Overview(
            company = "Test Company",
            client = "Test Client",
            product = "Test Product",
            role = "Test Role",
            period = Period(displayText = "Jan 2020 - Dec 2020")
        ),
        description = Description(
            short = "Short description",
            full = "Full description"
        ),
        technologies = Technologies(
            primary = listOf(Technology(name = "Kotlin"), Technology(name = "Android"))
        )
    )

    @Test
    fun buildsProjectIndex() {
        val provider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val index = provider.getProjectIndex()

        assertEquals(1, index.size)
        assertEquals("test-project", index[0].id)
        assertEquals("Test Project", index[0].name)
        assertEquals("Test Role", index[0].role)
        assertEquals("Jan 2020 - Dec 2020", index[0].period)
        assertEquals("Test tagline", index[0].tagline)
    }

    @Test
    fun extractsCuratedDetailsForFeatured() {
        val provider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = listOf("test-project")
        )

        val details = provider.getCuratedDetails("test-project")

        assertTrue(details != null)
        assertTrue(details!!.contains("Test Company"))
        assertTrue(details.contains("Test Product"))
        assertTrue(details.contains("Short description"))
        assertTrue(details.contains("Kotlin"))
    }

    @Test
    fun returnsNullForNonFeatured() {
        val provider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val details = provider.getCuratedDetails("test-project")

        assertEquals(null, details)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: FAIL with "Unresolved reference: AgentDataProvider"

**Step 3: Write the implementation**

```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.PersonalInfo

data class ProjectIndexEntry(
    val id: String,
    val name: String,
    val role: String,
    val period: String,
    val tagline: String
)

class AgentDataProvider(
    val personalInfo: PersonalInfo,
    private val allProjects: List<CareerProject>,
    private val featuredProjectIds: List<String>
) {
    companion object {
        val FEATURED_PROJECT_IDS = listOf(
            "geosatis",
            "mcdonalds",
            "adidas-gmr",
            "food-network-kitchen",
            "android-school"
        )
    }

    fun getProjectIndex(): List<ProjectIndexEntry> {
        return allProjects.map { project ->
            ProjectIndexEntry(
                id = project.id,
                name = project.name,
                role = project.overview?.role ?: "Engineer",
                period = project.overview?.period?.displayText ?: "",
                tagline = project.tagline ?: project.description?.short ?: ""
            )
        }
    }

    fun getCuratedDetails(projectId: String): String? {
        if (projectId !in featuredProjectIds) return null

        val project = allProjects.find { it.id == projectId } ?: return null

        return buildString {
            // Overview
            project.overview?.let { overview ->
                overview.company?.let { appendLine("Company: $it") }
                overview.client?.let { appendLine("Client: $it") }
                overview.product?.let { appendLine("Product: $it") }
                overview.role?.let { appendLine("Role: $it") }
                overview.period?.displayText?.let { appendLine("Period: $it") }
            }

            // Description
            appendLine()
            project.description?.short?.let { appendLine(it) }
            project.description?.full?.let { appendLine(it) }

            // Technologies
            project.technologies?.primary?.let { techs ->
                if (techs.isNotEmpty()) {
                    appendLine()
                    append("Technologies: ")
                    appendLine(techs.mapNotNull { it.name }.joinToString(", "))
                }
            }

            // Challenge
            project.challenge?.let { challenge ->
                appendLine()
                challenge.context?.let { appendLine("Challenge: $it") }
                challenge.response?.let { appendLine("Response: $it") }
            }
        }
    }

    fun getFeaturedProjects(): List<CareerProject> {
        return allProjects.filter { it.id in featuredProjectIds }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testDebugUnitTest --tests "*.AgentDataProviderTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/AgentDataProvider.kt
git add shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/AgentDataProviderTest.kt
git commit -m "feat(agent): add AgentDataProvider for system prompt data"
```

---

## Task 5: Rewrite SystemPromptBuilder

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilder.kt`
- Modify: `shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilderTest.kt`

**Step 1: Write the new test**

```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.career.models.CareerProject
import io.github.devmugi.cv.agent.career.models.Description
import io.github.devmugi.cv.agent.career.models.Overview
import io.github.devmugi.cv.agent.career.models.Period
import io.github.devmugi.cv.agent.career.models.PersonalInfo
import io.github.devmugi.cv.agent.career.models.SkillCategory
import io.github.devmugi.cv.agent.career.models.Technologies
import io.github.devmugi.cv.agent.career.models.Technology
import kotlin.test.Test
import kotlin.test.assertTrue

class SystemPromptBuilderTest {

    private val testPersonalInfo = PersonalInfo(
        name = "Test Name",
        title = "Test Title",
        location = "Test Location",
        email = "test@test.com",
        linkedin = "https://linkedin.com",
        github = "https://github.com",
        portfolio = "https://portfolio.com",
        summary = "Test summary",
        skills = listOf(SkillCategory("Languages", listOf("Kotlin", "Java")))
    )

    private val testProject = CareerProject(
        id = "test-project",
        name = "Test Project",
        slug = "test-project",
        tagline = "Test tagline",
        overview = Overview(
            company = "Test Company",
            role = "Test Role",
            period = Period(displayText = "2020")
        ),
        description = Description(short = "Short desc", full = "Full desc"),
        technologies = Technologies(primary = listOf(Technology(name = "Kotlin")))
    )

    private val builder = SystemPromptBuilder()

    @Test
    fun includesPersonalInfo() {
        val dataProvider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val prompt = builder.build(dataProvider)

        assertTrue(prompt.contains("Test Name"))
        assertTrue(prompt.contains("Test Title"))
        assertTrue(prompt.contains("Test Location"))
    }

    @Test
    fun includesSkills() {
        val dataProvider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val prompt = builder.build(dataProvider)

        assertTrue(prompt.contains("Languages"))
        assertTrue(prompt.contains("Kotlin"))
        assertTrue(prompt.contains("Java"))
    }

    @Test
    fun includesProjectIndex() {
        val dataProvider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val prompt = builder.build(dataProvider)

        assertTrue(prompt.contains("test-project"))
        assertTrue(prompt.contains("Test Project"))
    }

    @Test
    fun includesFeaturedProjectDetails() {
        val dataProvider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = listOf("test-project")
        )

        val prompt = builder.build(dataProvider)

        assertTrue(prompt.contains("Test Company"))
        assertTrue(prompt.contains("Full desc"))
    }

    @Test
    fun includesSuggestionInstructions() {
        val dataProvider = AgentDataProvider(
            personalInfo = testPersonalInfo,
            allProjects = listOf(testProject),
            featuredProjectIds = emptyList()
        )

        val prompt = builder.build(dataProvider)

        assertTrue(prompt.contains("suggestions"))
        assertTrue(prompt.contains("json"))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testDebugUnitTest --tests "*.SystemPromptBuilderTest"`
Expected: FAIL (method signature changed)

**Step 3: Rewrite the implementation**

```kotlin
package io.github.devmugi.cv.agent.agent

class SystemPromptBuilder {

    fun build(dataProvider: AgentDataProvider): String = buildString {
        appendLine(INSTRUCTIONS)
        appendLine()
        appendPersonalInfo(dataProvider)
        appendLine()
        appendSkills(dataProvider)
        appendLine()
        appendProjectIndex(dataProvider)
        appendLine()
        appendFeaturedProjects(dataProvider)
        appendLine()
        appendLine(SUGGESTION_INSTRUCTIONS)
    }

    private fun StringBuilder.appendPersonalInfo(dataProvider: AgentDataProvider) {
        val info = dataProvider.personalInfo
        appendLine("# PERSONAL INFO")
        appendLine("Name: ${info.name}")
        appendLine("Title: ${info.title}")
        appendLine("Location: ${info.location}")
        appendLine("Email: ${info.email}")
        appendLine("LinkedIn: ${info.linkedin}")
        appendLine("GitHub: ${info.github}")
        appendLine()
        appendLine("Summary: ${info.summary}")
    }

    private fun StringBuilder.appendSkills(dataProvider: AgentDataProvider) {
        appendLine("# SKILLS")
        dataProvider.personalInfo.skills.forEach { skill ->
            appendLine("- ${skill.category}: ${skill.items.joinToString(", ")}")
        }
    }

    private fun StringBuilder.appendProjectIndex(dataProvider: AgentDataProvider) {
        appendLine("# PROJECT INDEX")
        appendLine("All projects Denys has worked on:")
        dataProvider.getProjectIndex().forEach { entry ->
            appendLine("- ${entry.id}: \"${entry.name}\" | ${entry.role} | ${entry.period} | ${entry.tagline}")
        }
    }

    private fun StringBuilder.appendFeaturedProjects(dataProvider: AgentDataProvider) {
        appendLine("# FEATURED PROJECTS (FULL DETAILS)")
        dataProvider.getFeaturedProjects().forEach { project ->
            appendLine()
            appendLine("## ${project.name} (${project.id})")
            dataProvider.getCuratedDetails(project.id)?.let { details ->
                appendLine(details)
            }
        }
    }

    companion object {
        private val INSTRUCTIONS = """
            You are an AI assistant for Denys Honcharenko's portfolio. Answer questions about Denys in third person. Be helpful, professional, and concise.

            You have access to:
            - Personal information and skills
            - A project index with all projects (id, name, role, period, tagline)
            - Full details for 5 featured projects

            For non-featured projects, use the project index information.
        """.trimIndent()

        private val SUGGESTION_INSTRUCTIONS = """
            # RESPONSE FORMAT

            End EVERY response with a JSON block suggesting 0-3 related projects the user might want to explore:

            ```json
            {"suggestions": ["project-id-1", "project-id-2"]}
            ```

            Rules:
            - Only suggest projects relevant to the question
            - Use exact project IDs from the project index
            - If no projects are relevant, use: {"suggestions": []}
            - Always include this JSON block, even if empty
        """.trimIndent()
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testDebugUnitTest --tests "*.SystemPromptBuilderTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilder.kt
git add shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilderTest.kt
git commit -m "refactor(agent): rewrite SystemPromptBuilder for new data structure"
```

---

## Task 6: Update Message Model

**Files:**
- Modify: `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/Message.kt`
- Modify: `shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/MessageTest.kt`

**Step 1: Update the model**

Change `references: List<CVReference>` to `suggestions: List<String>`:

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
    val suggestions: List<String> = emptyList(),
    val timestamp: Long = currentTimeMillis()
)
```

**Step 2: Update MessageTest if needed**

Check if MessageTest references `references` and update to `suggestions`.

**Step 3: Run tests**

Run: `./gradlew :shared-domain:testDebugUnitTest`
Expected: PASS

**Step 4: Commit**

```bash
git add shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/Message.kt
git add shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/MessageTest.kt
git commit -m "refactor(domain): change Message.references to suggestions"
```

---

## Task 7: Update ChatViewModel

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt`

**Step 1: Update imports and dependencies**

Replace:
- `ReferenceExtractor` → `SuggestionExtractor`
- `CVData` → `AgentDataProvider`
- Remove `CVRepository` usage

**Step 2: Update the implementation**

```kotlin
package io.github.devmugi.cv.agent.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.github.devmugi.cv.agent.api.GroqApiException
import io.github.devmugi.cv.agent.api.models.ChatMessage
import io.github.devmugi.cv.agent.domain.models.ChatError
import io.github.devmugi.cv.agent.domain.models.ChatState
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.models.MessageRole
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val apiClient: GroqApiClient,
    private val promptBuilder: SystemPromptBuilder,
    private val suggestionExtractor: SuggestionExtractor,
    private val dataProvider: AgentDataProvider?,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var lastUserMessage: String? = null

    companion object {
        private const val MAX_HISTORY = 10
    }

    fun sendMessage(content: String) {
        lastUserMessage = content
        val userMessage = Message(role = MessageRole.USER, content = content)

        _state.update { current ->
            current.copy(
                messages = current.messages + userMessage,
                isLoading = true,
                thinkingStatus = "Crafting personalized response.",
                error = null,
                suggestions = emptyList()
            )
        }

        viewModelScope.launch {
            streamResponse()
        }
    }

    fun onSuggestionClicked(suggestion: String) {
        sendMessage(suggestion)
    }

    fun retry() {
        lastUserMessage?.let { sendMessage(it) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearHistory() {
        _state.update { ChatState() }
        lastUserMessage = null
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun streamResponse() {
        val systemPrompt = dataProvider?.let { promptBuilder.build(it) } ?: ""

        val apiMessages = buildApiMessages(systemPrompt)
        val assistantMessageId = Uuid.random().toString()

        val assistantMessage = Message(
            id = assistantMessageId,
            role = MessageRole.ASSISTANT,
            content = "",
            suggestions = emptyList()
        )

        _state.update { current ->
            current.copy(
                messages = current.messages + assistantMessage,
                isLoading = false,
                isStreaming = true,
                streamingMessageId = assistantMessageId,
                thinkingStatus = null
            )
        }

        var streamedContent = ""

        apiClient.streamChatCompletion(
            messages = apiMessages,
            onChunk = { chunk ->
                streamedContent += chunk
                _state.update { current ->
                    current.copy(
                        messages = current.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(content = streamedContent)
                            } else {
                                msg
                            }
                        }
                    )
                }
            },
            onComplete = {
                val extractionResult = suggestionExtractor.extract(streamedContent)
                _state.update { current ->
                    current.copy(
                        messages = current.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(
                                    content = extractionResult.cleanedContent,
                                    suggestions = extractionResult.suggestions
                                )
                            } else {
                                msg
                            }
                        },
                        isStreaming = false,
                        streamingMessageId = null
                    )
                }
            },
            onError = { exception ->
                val error = when (exception) {
                    is GroqApiException.NetworkError -> ChatError.Network(exception.reason)
                    is GroqApiException.RateLimitError -> ChatError.RateLimit
                    is GroqApiException.AuthError -> ChatError.Api("Authentication failed")
                    is GroqApiException.ApiError -> ChatError.Api(exception.message)
                }
                _state.update { current ->
                    current.copy(
                        messages = current.messages.filter { it.id != assistantMessageId },
                        isLoading = false,
                        isStreaming = false,
                        streamingMessageId = null,
                        error = error
                    )
                }
            }
        )
    }

    private fun buildApiMessages(systemPrompt: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        if (systemPrompt.isNotEmpty()) {
            messages.add(ChatMessage(role = "system", content = systemPrompt))
        }

        val recentMessages = _state.value.messages.takeLast(MAX_HISTORY)
        recentMessages.forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            messages.add(ChatMessage(role = role, content = msg.content))
        }

        return messages
    }
}
```

**Step 3: Run compilation**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: PASS (may have errors in tests, fix in next step)

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt
git commit -m "refactor(agent): update ChatViewModel for new data providers"
```

---

## Task 8: Update AppModule DI

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/AppModule.kt`

**Step 1: Update the module**

```kotlin
package io.github.devmugi.cv.agent.di

import io.github.devmugi.cv.agent.GroqConfig
import io.github.devmugi.cv.agent.agent.AgentDataProvider
import io.github.devmugi.cv.agent.agent.ChatViewModel
import io.github.devmugi.cv.agent.agent.SuggestionExtractor
import io.github.devmugi.cv.agent.agent.SystemPromptBuilder
import io.github.devmugi.cv.agent.api.GroqApiClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {
    // HTTP Client
    single {
        HttpClient(httpEngineFactory) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    // API Layer
    single { GroqApiClient(get(), GroqConfig.apiKey) }

    // Agent Layer
    single { SystemPromptBuilder() }
    single { SuggestionExtractor() }

    // ViewModel factory
    factory { (dataProvider: AgentDataProvider?) ->
        ChatViewModel(
            apiClient = get(),
            promptBuilder = get(),
            suggestionExtractor = get(),
            dataProvider = dataProvider
        )
    }
}
```

**Step 2: Run compilation**

Run: `./gradlew :shared:compileKotlinAndroid`
Expected: PASS

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/AppModule.kt
git commit -m "refactor(di): update AppModule for new agent architecture"
```

---

## Task 9: Remove Old Files

**Files to delete:**
- `shared/src/commonMain/composeResources/files/cv_data.json`
- `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractor.kt`
- `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVRepository.kt`
- `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVDataLoader.kt`
- `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/CVData.kt`
- `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/CVReference.kt`

**Step 1: Delete the files**

```bash
rm shared/src/commonMain/composeResources/files/cv_data.json
rm shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractor.kt
rm shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVRepository.kt
rm shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVDataLoader.kt
rm shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/CVData.kt
rm shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/models/CVReference.kt
```

**Step 2: Delete old tests**

```bash
rm shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ReferenceExtractorTest.kt
rm shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/data/CVDataLoaderTest.kt
```

**Step 3: Run compilation to find any remaining references**

Run: `./gradlew :shared:compileKotlinAndroid :shared-domain:compileKotlinAndroid`

Fix any remaining import errors.

**Step 4: Commit**

```bash
git add -A
git commit -m "refactor(agent): remove old CV data system

- Remove cv_data.json
- Remove ReferenceExtractor
- Remove CVRepository and CVDataLoader
- Remove CVData and CVReference models
- Remove associated tests"
```

---

## Task 10: Update ChatViewModelTest

**Files:**
- Modify: `shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Update test to use new dependencies**

Update the test to:
- Use `SuggestionExtractor` instead of `ReferenceExtractor`
- Use `AgentDataProvider` instead of `CVData`
- Update assertions for `suggestions` instead of `references`

**Step 2: Run tests**

Run: `./gradlew :shared:testDebugUnitTest --tests "*.ChatViewModelTest"`
Expected: PASS

**Step 3: Commit**

```bash
git add shared/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt
git commit -m "test(agent): update ChatViewModelTest for new architecture"
```

---

## Task 11: Integration - Update MainActivity

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt`

**Step 1: Update to load data and create AgentDataProvider**

The MainActivity needs to:
1. Load `personal_info.json` from compose resources
2. Load project JSON files
3. Create `AgentDataProvider`
4. Pass to ViewModel

**Step 2: Run the app**

Run: `./gradlew :android-app:assembleDebug && adb install -r android-app/build/outputs/apk/debug/android-app-debug.apk`

**Step 3: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/MainActivity.kt
git commit -m "feat(android): integrate new agent data loading"
```

---

## Task 12: Final Verification

**Step 1: Run all tests**

Run: `./gradlew :shared:testDebugUnitTest :shared-domain:testDebugUnitTest :shared-career-projects:testDebugUnitTest`

**Step 2: Run quality checks**

Run: `./gradlew qualityCheck`

**Step 3: Test the app manually**

1. Launch the app
2. Ask "What technologies does Denys know?"
3. Verify response includes skills and ends with JSON suggestions
4. Verify suggestion chips appear below the message

**Step 4: Final commit if needed**

```bash
git add -A
git commit -m "chore: final cleanup and fixes"
```
