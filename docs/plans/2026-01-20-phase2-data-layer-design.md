# Phase 2: Data Layer & CV Models - Design Document

**Project:** CV Agent
**Phase:** 2 - Data Layer & CV Models
**Date:** 2026-01-20
**Author:** Denys Honcharenko (with Claude Code)

---

## Overview

Phase 2 establishes the data foundation for the CV Agent application. This includes all data models for representing CV information, JSON data storage, data loading utilities, and a repository layer for data access.

## Goals

- Define all CV data models with Kotlinx Serialization
- Create complete CV data in structured JSON format
- Implement data loading and caching
- Provide lookup methods for reference resolution
- Achieve 80%+ test coverage for data layer

---

## 1. Data Models

All models in `composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/data/models/`

### 1.1 Core Models

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

### 1.2 Reference Model

```kotlin
@Serializable
data class CVReference(
    val id: String,
    val type: String,
    val label: String
)
```

### 1.3 ID Convention

All IDs follow dot-notation: `type.identifier`

Examples:
- `experience.mcdonalds`
- `experience.geosatis`
- `project.mtg-deckbuilder`
- `project.scryfall-api`
- `skills.kmp`
- `skills.ai-dev`
- `achievement.android-school`

---

## 2. Data Loading

### 2.1 CVDataLoader

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

### 2.2 CVRepository

```kotlin
package io.github.devmugi.cv.agent.data.repository

import io.github.devmugi.cv.agent.data.models.*

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

---

## 3. JSON Data Structure

File: `composeApp/src/commonMain/composeResources/files/cv_data.json`

```json
{
  "personalInfo": {
    "name": "Denys Honcharenko",
    "location": "Genk, Belgium",
    "email": "aidevmugi@gmail.com",
    "phone": "+32 470 383 388",
    "linkedin": "https://www.linkedin.com/in/denyshoncharenko/",
    "github": "https://github.com/devmugi",
    "portfolio": "https://devmugi.github.io/devmugi/"
  },
  "summary": "Result-oriented Software Engineer with 15+ years of experience building apps that reach 100M+ users across 60+ countries. Specializing in Kotlin Multiplatform, Jetpack Compose, IoT solutions, and AI-powered development. Creator of open-source KMM libraries.",
  "skills": [
    {
      "id": "skills.ai-dev",
      "category": "AI-Powered Development",
      "level": "Power User",
      "skills": ["Claude Code", "MCP Server", "Superpowers", "spec-kit", "Claude Cowork", "Koog", "AI Prompting"]
    },
    {
      "id": "skills.kmp",
      "category": "Kotlin Multiplatform",
      "level": "Specialist",
      "skills": ["KMM", "Compose Multiplatform", "Coroutines", "Ktor"]
    },
    {
      "id": "skills.android",
      "category": "Android Development",
      "level": null,
      "skills": ["Android SDK", "Kotlin", "Java", "Jetpack Compose", "Android TV"]
    },
    {
      "id": "skills.architecture",
      "category": "Architecture Patterns",
      "level": null,
      "skills": ["MVVM", "MVP", "MVI", "Clean Architecture"]
    },
    {
      "id": "skills.iot",
      "category": "IoT & Smart Home",
      "level": null,
      "skills": ["Bluetooth/BLE", "MQTT", "OpenHab", "Alexa Skills", "AWS IoT"]
    },
    {
      "id": "skills.enterprise",
      "category": "Enterprise & MDM",
      "level": null,
      "skills": ["Kiosk Mode", "Samsung Knox", "Android Enterprise", "Custom ROM"]
    },
    {
      "id": "skills.backend",
      "category": "Backend Development",
      "level": null,
      "skills": ["Spring Boot", "Kafka", "REST API", "Node.js", "Ktor", "Kubernetes"]
    },
    {
      "id": "skills.cloud",
      "category": "Cloud & DevOps",
      "level": null,
      "skills": ["AWS", "Firebase", "CI/CD", "Docker", "Kubernetes"]
    }
  ],
  "experience": [
    {
      "id": "experience.geosatis",
      "title": "Android Developer / Backend Developer",
      "company": "GEOSATIS",
      "period": "Oct 2022 - Jan 2025",
      "description": "Secured IoT solution based on Samsung Knox and Android application to protect victims. Full-stack development including new Backend with Spring Boot, REST, Kafka, and Kubernetes.",
      "highlights": ["Victim Protection IoT system", "Samsung Knox integration", "Full-stack development"],
      "technologies": ["IoT", "Samsung Knox", "Spring Boot", "Kafka", "K8s", "Android"],
      "featured": false
    },
    {
      "id": "experience.mcdonalds",
      "title": "Android Engineer",
      "company": "McDonald's",
      "period": "Aug 2021 - Oct 2022",
      "description": "Developed features for the global McDonald's app. Led Kotlin Multiplatform Mobile development, created PoCs and MVPs, presented demos for new features. Organized Android Talks to align team and solve technical issues.",
      "highlights": ["100M+ Downloads", "60+ Countries", "KMM architecture lead"],
      "technologies": ["KMM", "Kotlin", "Coroutines", "RxJava", "Koin"],
      "featured": true
    },
    {
      "id": "experience.epam-discovery",
      "title": "Lead Android Engineer",
      "company": "EPAM Systems - Food Network Kitchen (Discovery)",
      "period": "Oct 2020 - Sep 2021",
      "description": "Led Android development for 30+ person team. Migrated CI/CD from Bitbucket to GitHub, integrated with Jira, SonarCube, Slack. Mentored 2 junior developers to middle level.",
      "highlights": ["30+ person team lead", "CI/CD migration", "Mentorship"],
      "technologies": ["Android TV", "ExoPlayer", "GraphQL", "Dagger2"],
      "featured": false
    },
    {
      "id": "experience.adidas-gmr",
      "title": "Lead Software Engineer",
      "company": "EPAM Systems - Adidas GMR",
      "period": "Feb 2020 - Jun 2020",
      "description": "Led mobile team for Adidas GMR - innovative product with Jacquard tag by Google. Built custom Bluetooth Tag emulator for QA. Participated in high-level meetings with Google, Adidas, and Electronic Arts stakeholders.",
      "highlights": ["Google Jacquard integration", "FIFA Integration", "Collaboration with Google, Adidas, EA"],
      "technologies": ["Bluetooth", "IoT", "Team Lead", "CI/CD"],
      "featured": true
    },
    {
      "id": "experience.adomi",
      "title": "Lead Software Engineer",
      "company": "Team Technologies (Temy) - Adomi Smart Home",
      "period": "May 2018 - Feb 2020",
      "description": "Built smart home solution from scratch for luxury Silicon Valley houses. Developed Android mobile app, Kiosk Mode tablet app, AWS IoT infrastructure, and Alexa/Google Assistant integrations.",
      "highlights": ["Smart home from scratch", "AWS IoT infrastructure", "Voice assistant integration"],
      "technologies": ["Smart Home", "AWS IoT", "Alexa Skills", "Kiosk Mode", "MQTT"],
      "featured": false
    },
    {
      "id": "experience.lesara",
      "title": "Senior Android Engineer",
      "company": "Team Technologies (Temy) - Lesara",
      "period": "Sep 2017 - May 2018",
      "description": "Fashion e-commerce app for Europe's fastest-growing tech company (2016). Introduced MVP architecture, initiated Clean Architecture discussions.",
      "highlights": ["1M+ Downloads", "4.4 Rating", "Architecture improvements"],
      "technologies": ["MVP", "RxJava", "Dagger"],
      "featured": false
    }
  ],
  "projects": [
    {
      "id": "project.mtg-deckbuilder",
      "name": "MTG DeckBuilder",
      "type": "Open Source",
      "description": "Commander deck builder for Magic: The Gathering. Features card search, mana curve analysis, price tracking, and deck export.",
      "technologies": ["Kotlin/JS", "Compose Web", "Scryfall API", "Coroutines", "Claude Cowork"],
      "links": {
        "demo": "https://devmugi.github.io/mtg-deckbuilder-web/",
        "source": "https://github.com/devmugi/mtg-deckbuilder-web"
      },
      "featured": true
    },
    {
      "id": "project.scryfall-api",
      "name": "scryfall-api",
      "type": "Open Source",
      "description": "Kotlin Multiplatform library for the Scryfall API (Magic: The Gathering). Supports JVM, Android, iOS, and JavaScript platforms.",
      "technologies": ["Kotlin Multiplatform", "Ktor", "spec-kit", "Claude"],
      "links": {
        "source": "https://github.com/devmugi/scryfall-api"
      },
      "featured": false
    },
    {
      "id": "project.wizards-api",
      "name": "wizards-api",
      "type": "Open Source",
      "description": "Kotlin Multiplatform library for Wizards of the Coast APIs. Cross-platform support for MTG-related applications.",
      "technologies": ["Kotlin Multiplatform", "Ktor", "Coroutines"],
      "links": {
        "source": "https://github.com/devmugi/wizards-api"
      },
      "featured": false
    },
    {
      "id": "project.pokedex",
      "name": "AndroidLabPokedex",
      "type": "Personal",
      "description": "Showcasing modern Android development practices with Kotlin, MVVM architecture, and Jetpack components.",
      "technologies": ["Kotlin", "MVVM", "Jetpack", "Coroutines"],
      "links": {
        "source": "https://github.com/devmugi/AndroidLabPokedex"
      },
      "featured": false
    }
  ],
  "achievements": [
    {
      "id": "achievement.claude-power-user",
      "title": "Claude Code Power User",
      "organization": null,
      "year": "2025",
      "description": "Building production apps with spec-kit, Superpowers, and Cowork"
    },
    {
      "id": "achievement.mcp-developer",
      "title": "MCP Server Developer",
      "organization": null,
      "year": "2025",
      "description": "Building AI automation tools for internal usage"
    },
    {
      "id": "achievement.google-prompting",
      "title": "Google Prompting Essentials",
      "organization": "Google",
      "year": "2024",
      "description": "Certified in AI prompting techniques"
    },
    {
      "id": "achievement.android-school-creator",
      "title": "Android School Creator",
      "organization": "EPAM Systems",
      "year": "2020-2021",
      "description": "Created and launched Android School education course. 14 of 16 attendees were hired by the company."
    },
    {
      "id": "achievement.android-school-instructor",
      "title": "Android School Instructor",
      "organization": "EPAM Systems",
      "year": "2021",
      "description": "2nd course of Android School. 6 of 10 students hired by company."
    },
    {
      "id": "achievement.engineering-manager",
      "title": "Engineering Manager",
      "organization": "EPAM Systems",
      "year": "2020-2021",
      "description": "Managed team of 4-6 engineers. Completed Software Engineering Manager School."
    }
  ],
  "education": {
    "degree": "Master's Degree",
    "field": "Computer Science",
    "institution": "Kherson National Technical University"
  }
}
```

---

## 4. Testing Strategy

### 4.1 Test Files

```
composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/data/
├── CVDataLoaderTest.kt
└── CVRepositoryTest.kt
```

### 4.2 CVDataLoaderTest

```kotlin
class CVDataLoaderTest {
    @Test fun `loads valid JSON successfully`()
    @Test fun `handles missing optional fields`()
    @Test fun `throws on invalid JSON`()
    @Test fun `throws on missing required fields`()
}
```

### 4.3 CVRepositoryTest

```kotlin
class CVRepositoryTest {
    @Test fun `getCVData returns parsed data`()
    @Test fun `getCVData caches result`()
    @Test fun `findExperienceById returns correct item`()
    @Test fun `findExperienceById returns null for unknown id`()
    @Test fun `findProjectById returns correct item`()
    @Test fun `findSkillCategoryById returns correct item`()
    @Test fun `findAchievementById returns correct item`()
    @Test fun `resolveReference creates correct CVReference for experience`()
    @Test fun `resolveReference creates correct CVReference for project`()
    @Test fun `resolveReference creates correct CVReference for skill`()
    @Test fun `resolveReference returns null for unknown id`()
}
```

### 4.4 Test Data

- Small JSON fixtures in test resources
- Edge cases: empty lists, null optionals
- Invalid data: malformed JSON, wrong types

---

## 5. File Structure

```
composeApp/src/commonMain/kotlin/io/github/devmugi/cv/agent/
├── data/
│   ├── models/
│   │   ├── CVData.kt
│   │   └── CVReference.kt
│   └── repository/
│       ├── CVDataLoader.kt
│       └── CVRepository.kt

composeApp/src/commonMain/composeResources/files/
└── cv_data.json

composeApp/src/commonTest/kotlin/io/github/devmugi/cv/agent/data/
├── CVDataLoaderTest.kt
└── CVRepositoryTest.kt
```

---

## 6. Acceptance Criteria

- [ ] All data models defined with `@Serializable` annotation
- [ ] Complete CV data in `cv_data.json` with all experiences, projects, skills, achievements
- [ ] CVDataLoader parses JSON successfully
- [ ] CVRepository provides lookup by ID for all types
- [ ] CVRepository.resolveReference creates correct CVReference objects
- [ ] 80%+ test coverage for data layer
- [ ] All tests passing
- [ ] Quality checks passing (ktlint, detekt)

---

## 7. Implementation Order

1. Create data model files (CVData.kt, CVReference.kt)
2. Implement CVDataLoader
3. Implement CVRepository
4. Create complete cv_data.json
5. Write unit tests for CVDataLoader
6. Write unit tests for CVRepository
7. Run quality checks and fix issues
8. Verify test coverage

---

## Document Metadata

| Version | Date | Status | Author |
|---------|------|--------|--------|
| 1.0 | 2026-01-20 | Draft | Denys Honcharenko |

---

**End of Design Document**
