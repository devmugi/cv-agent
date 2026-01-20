# Phase 2: Data Layer & CV Models - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement CV data models, JSON data storage, and repository layer with 80%+ test coverage

**Architecture:** Kotlinx Serialization data classes with JSON resource file, CVDataLoader for parsing, CVRepository for data access and reference resolution

**Tech Stack:** Kotlin, Kotlinx Serialization, Kotlin Test

---

## Task 1: Create Data Models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/CVData.kt`

**Step 1: Create CVData.kt with all data classes**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/CVData.kt`:

```kotlin
package io.github.devmugi.cv.agent.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CVData(
    val personalInfo: PersonalInfo,
    val summary: String,
    val skills: List<SkillCategory>,
    val experience: List<WorkExperience>,
    val projects: List<Project>,
    val achievements: List<Achievement>,
    val education: Education
)

@Serializable
data class PersonalInfo(
    val name: String,
    val location: String,
    val email: String,
    val phone: String,
    val linkedin: String,
    val github: String,
    val portfolio: String
)

@Serializable
data class SkillCategory(
    val id: String,
    val category: String,
    val level: String? = null,
    val skills: List<String>
)

@Serializable
data class WorkExperience(
    val id: String,
    val title: String,
    val company: String,
    val period: String,
    val description: String,
    val highlights: List<String>,
    val technologies: List<String>,
    val featured: Boolean = false
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val type: String,
    val description: String,
    val technologies: List<String>,
    val links: ProjectLinks? = null,
    val featured: Boolean = false
)

@Serializable
data class ProjectLinks(
    val demo: String? = null,
    val source: String? = null,
    val playStore: String? = null
)

@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val organization: String? = null,
    val year: String,
    val description: String
)

@Serializable
data class Education(
    val degree: String,
    val field: String,
    val institution: String
)
```

**Step 2: Verify compilation**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/CVData.kt
git commit -m "Add CV data models

Define serializable data classes for CVData, PersonalInfo, SkillCategory, WorkExperience, Project, Achievement, and Education.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Create CVReference Model

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/CVReference.kt`

**Step 1: Create CVReference.kt**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/CVReference.kt`:

```kotlin
package io.github.devmugi.cv.agent.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CVReference(
    val id: String,
    val type: String,
    val label: String
)
```

**Step 2: Verify compilation**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/CVReference.kt
git commit -m "Add CVReference model

Define reference type for AI agent citations.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Create CVDataLoader with Test

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVDataLoader.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/data/CVDataLoaderTest.kt`

**Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/data/CVDataLoaderTest.kt`:

```kotlin
package io.github.devmugi.cv.agent.data

import io.github.devmugi.cv.agent.data.repository.CVDataLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CVDataLoaderTest {

    private val loader = CVDataLoader()

    private val validMinimalJson = """
        {
            "personalInfo": {
                "name": "Test Name",
                "location": "Test Location",
                "email": "test@test.com",
                "phone": "+1234567890",
                "linkedin": "https://linkedin.com/test",
                "github": "https://github.com/test",
                "portfolio": "https://test.com"
            },
            "summary": "Test summary",
            "skills": [],
            "experience": [],
            "projects": [],
            "achievements": [],
            "education": {
                "degree": "Test Degree",
                "field": "Test Field",
                "institution": "Test University"
            }
        }
    """.trimIndent()

    @Test
    fun `loads valid JSON successfully`() {
        val result = loader.load(validMinimalJson)

        assertEquals("Test Name", result.personalInfo.name)
        assertEquals("Test summary", result.summary)
        assertEquals("Test Degree", result.education.degree)
    }

    @Test
    fun `handles missing optional fields`() {
        val jsonWithOptionals = """
            {
                "personalInfo": {
                    "name": "Test",
                    "location": "Location",
                    "email": "test@test.com",
                    "phone": "123",
                    "linkedin": "https://linkedin.com",
                    "github": "https://github.com",
                    "portfolio": "https://portfolio.com"
                },
                "summary": "Summary",
                "skills": [{
                    "id": "skills.test",
                    "category": "Test Category",
                    "skills": ["Skill1"]
                }],
                "experience": [],
                "projects": [{
                    "id": "project.test",
                    "name": "Test Project",
                    "type": "Open Source",
                    "description": "Description",
                    "technologies": ["Kotlin"]
                }],
                "achievements": [{
                    "id": "achievement.test",
                    "title": "Test Achievement",
                    "year": "2024",
                    "description": "Description"
                }],
                "education": {
                    "degree": "Degree",
                    "field": "Field",
                    "institution": "Institution"
                }
            }
        """.trimIndent()

        val result = loader.load(jsonWithOptionals)

        assertEquals(null, result.skills[0].level)
        assertEquals(null, result.projects[0].links)
        assertEquals(null, result.achievements[0].organization)
    }

    @Test
    fun `throws on invalid JSON`() {
        val invalidJson = "{ invalid json }"

        assertFailsWith<Exception> {
            loader.load(invalidJson)
        }
    }

    @Test
    fun `throws on missing required fields`() {
        val missingFields = """
            {
                "personalInfo": {
                    "name": "Test"
                }
            }
        """.trimIndent()

        assertFailsWith<Exception> {
            loader.load(missingFields)
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.data.CVDataLoaderTest"`
Expected: FAIL (CVDataLoader class not found)

**Step 3: Implement CVDataLoader**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVDataLoader.kt`:

```kotlin
package io.github.devmugi.cv.agent.data.repository

import io.github.devmugi.cv.agent.data.models.CVData
import kotlinx.serialization.json.Json

class CVDataLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun load(jsonString: String): CVData {
        return json.decodeFromString<CVData>(jsonString)
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.data.CVDataLoaderTest"`
Expected: BUILD SUCCESSFUL (4 tests passed)

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVDataLoader.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/data/CVDataLoaderTest.kt
git commit -m "Add CVDataLoader with tests

Implement JSON parsing for CV data with error handling. Tests cover valid JSON, optional fields, and error cases.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Create CVRepository with Tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVRepository.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/data/CVRepositoryTest.kt`

**Step 1: Write the failing tests**

Create `composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/data/CVRepositoryTest.kt`:

```kotlin
package io.github.devmugi.cv.agent.data

import io.github.devmugi.cv.agent.data.repository.CVRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class CVRepositoryTest {

    private val repository = CVRepository()

    private val testJson = """
        {
            "personalInfo": {
                "name": "Denys Honcharenko",
                "location": "Genk, Belgium",
                "email": "test@test.com",
                "phone": "+32 123 456 789",
                "linkedin": "https://linkedin.com/in/test",
                "github": "https://github.com/test",
                "portfolio": "https://test.com"
            },
            "summary": "Test summary",
            "skills": [
                {
                    "id": "skills.kmp",
                    "category": "Kotlin Multiplatform",
                    "level": "Specialist",
                    "skills": ["KMM", "Compose"]
                }
            ],
            "experience": [
                {
                    "id": "experience.mcdonalds",
                    "title": "Android Engineer",
                    "company": "McDonald's",
                    "period": "Aug 2021 - Oct 2022",
                    "description": "Test description",
                    "highlights": ["100M+ Downloads"],
                    "technologies": ["KMM", "Kotlin"],
                    "featured": true
                }
            ],
            "projects": [
                {
                    "id": "project.mtg-deckbuilder",
                    "name": "MTG DeckBuilder",
                    "type": "Open Source",
                    "description": "Test description",
                    "technologies": ["Kotlin/JS"],
                    "featured": true
                }
            ],
            "achievements": [
                {
                    "id": "achievement.android-school-creator",
                    "title": "Android School Creator",
                    "organization": "EPAM Systems",
                    "year": "2020-2021",
                    "description": "Test description"
                }
            ],
            "education": {
                "degree": "Master's Degree",
                "field": "Computer Science",
                "institution": "Test University"
            }
        }
    """.trimIndent()

    @Test
    fun `getCVData returns parsed data`() {
        val result = repository.getCVData(testJson)

        assertEquals("Denys Honcharenko", result.personalInfo.name)
        assertEquals(1, result.skills.size)
        assertEquals(1, result.experience.size)
    }

    @Test
    fun `getCVData caches result`() {
        val first = repository.getCVData(testJson)
        val second = repository.getCVData(testJson)

        assertSame(first, second)
    }

    @Test
    fun `findExperienceById returns correct item`() {
        repository.getCVData(testJson)

        val result = repository.findExperienceById("experience.mcdonalds")

        assertNotNull(result)
        assertEquals("McDonald's", result.company)
    }

    @Test
    fun `findExperienceById returns null for unknown id`() {
        repository.getCVData(testJson)

        val result = repository.findExperienceById("experience.unknown")

        assertNull(result)
    }

    @Test
    fun `findProjectById returns correct item`() {
        repository.getCVData(testJson)

        val result = repository.findProjectById("project.mtg-deckbuilder")

        assertNotNull(result)
        assertEquals("MTG DeckBuilder", result.name)
    }

    @Test
    fun `findSkillCategoryById returns correct item`() {
        repository.getCVData(testJson)

        val result = repository.findSkillCategoryById("skills.kmp")

        assertNotNull(result)
        assertEquals("Kotlin Multiplatform", result.category)
    }

    @Test
    fun `findAchievementById returns correct item`() {
        repository.getCVData(testJson)

        val result = repository.findAchievementById("achievement.android-school-creator")

        assertNotNull(result)
        assertEquals("Android School Creator", result.title)
    }

    @Test
    fun `resolveReference creates correct CVReference for experience`() {
        repository.getCVData(testJson)

        val result = repository.resolveReference("experience.mcdonalds")

        assertNotNull(result)
        assertEquals("experience.mcdonalds", result.id)
        assertEquals("experience", result.type)
        assertEquals("McDonald's", result.label)
    }

    @Test
    fun `resolveReference creates correct CVReference for project`() {
        repository.getCVData(testJson)

        val result = repository.resolveReference("project.mtg-deckbuilder")

        assertNotNull(result)
        assertEquals("project", result.type)
        assertEquals("MTG DeckBuilder", result.label)
    }

    @Test
    fun `resolveReference creates correct CVReference for skill`() {
        repository.getCVData(testJson)

        val result = repository.resolveReference("skills.kmp")

        assertNotNull(result)
        assertEquals("skill", result.type)
        assertEquals("Kotlin Multiplatform", result.label)
    }

    @Test
    fun `resolveReference returns null for unknown id`() {
        repository.getCVData(testJson)

        val result = repository.resolveReference("unknown.id")

        assertNull(result)
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.data.CVRepositoryTest"`
Expected: FAIL (CVRepository class not found)

**Step 3: Implement CVRepository**

Create `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVRepository.kt`:

```kotlin
package io.github.devmugi.cv.agent.data.repository

import io.github.devmugi.cv.agent.data.models.Achievement
import io.github.devmugi.cv.agent.data.models.CVData
import io.github.devmugi.cv.agent.data.models.CVReference
import io.github.devmugi.cv.agent.data.models.Project
import io.github.devmugi.cv.agent.data.models.SkillCategory
import io.github.devmugi.cv.agent.data.models.WorkExperience

class CVRepository(
    private val loader: CVDataLoader = CVDataLoader()
) {
    private var cachedData: CVData? = null

    fun getCVData(jsonString: String): CVData {
        return cachedData ?: loader.load(jsonString).also {
            cachedData = it
        }
    }

    fun findExperienceById(id: String): WorkExperience? {
        return cachedData?.experience?.find { it.id == id }
    }

    fun findProjectById(id: String): Project? {
        return cachedData?.projects?.find { it.id == id }
    }

    fun findSkillCategoryById(id: String): SkillCategory? {
        return cachedData?.skills?.find { it.id == id }
    }

    fun findAchievementById(id: String): Achievement? {
        return cachedData?.achievements?.find { it.id == id }
    }

    fun resolveReference(id: String): CVReference? {
        val type = id.substringBefore(".")
        return when (type) {
            "experience" -> findExperienceById(id)?.let {
                CVReference(id, "experience", it.company)
            }
            "project" -> findProjectById(id)?.let {
                CVReference(id, "project", it.name)
            }
            "skills" -> findSkillCategoryById(id)?.let {
                CVReference(id, "skill", it.category)
            }
            "achievement" -> findAchievementById(id)?.let {
                CVReference(id, "achievement", it.title)
            }
            else -> null
        }
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew composeApp:testDebugUnitTest --tests "io.github.devmugi.cv.agent.data.CVRepositoryTest"`
Expected: BUILD SUCCESSFUL (12 tests passed)

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/CVRepository.kt
git add composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/data/CVRepositoryTest.kt
git commit -m "Add CVRepository with tests

Implement data access layer with caching, lookup by ID, and reference resolution. Tests cover all public methods.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Create Complete CV Data JSON

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/files/cv_data.json`

**Step 1: Replace placeholder with complete CV data**

Replace the content of `composeApp/src/commonMain/composeResources/files/cv_data.json` with the complete JSON from the design document (Section 3).

**Step 2: Verify JSON syntax**

Run: `./gradlew composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/composeResources/files/cv_data.json
git commit -m "Add complete CV data JSON

Populate cv_data.json with all personal info, skills, experiences, projects, and achievements.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Clean Up Package Structure

**Files:**
- Delete: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/.gitkeep`
- Delete: `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/.gitkeep`

**Step 1: Remove .gitkeep files**

```bash
rm composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/.gitkeep
rm composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/repository/.gitkeep
```

**Step 2: Commit**

```bash
git add -A
git commit -m "Remove .gitkeep files from data package

Directories now have actual source files.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 7: Run Quality Checks

**Step 1: Run ktlint format**

Run: `./gradlew ktlintFormat`
Expected: BUILD SUCCESSFUL (may modify files)

**Step 2: Run detekt**

Run: `./gradlew detekt`
Expected: BUILD SUCCESSFUL

**Step 3: Run all tests**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL

**Step 4: If any files were modified, commit**

```bash
git add -A
git commit -m "Fix code style issues

Apply ktlint formatting to data layer files.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 8: Final Verification

**Step 1: Run quality check**

Run: `./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL

**Step 2: Verify all tests pass**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL with 16+ tests passing

**Step 3: Create completion document**

Create `docs/verification/phase2-completion.md`:

```markdown
# Phase 2 Completion Verification

**Date:** 2026-01-20
**Status:** COMPLETE

## Data Models

- CVData.kt with all data classes
- CVReference.kt for AI citations

## Repository Layer

- CVDataLoader parses JSON successfully
- CVRepository provides lookup by ID
- resolveReference creates correct CVReference objects

## Test Coverage

- CVDataLoaderTest: 4 tests
- CVRepositoryTest: 12 tests
- All tests passing

## Quality Gates

- ktlintCheck: No violations
- detekt: No issues

## Next Steps

Ready for Phase 3: Groq API Integration & Agent Logic
```

**Step 4: Commit**

```bash
git add docs/verification/phase2-completion.md
git commit -m "Add Phase 2 completion verification

Document successful completion of data layer implementation.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Acceptance Criteria Checklist

After completing all tasks, verify:

- [ ] CVData.kt contains all data classes with @Serializable
- [ ] CVReference.kt exists with id, type, label fields
- [ ] CVDataLoader parses JSON correctly
- [ ] CVRepository provides all lookup methods
- [ ] cv_data.json contains complete CV data
- [ ] All 16+ tests passing
- [ ] ktlintCheck passes
- [ ] detekt passes
- [ ] qualityCheck passes

---

**Plan Complete - Ready for Execution**
