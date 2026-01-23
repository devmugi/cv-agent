# Agent Improvements Design

**Date:** 2026-01-23
**Branch:** agent-chat

## Overview

Redesign the CV Agent to use rich project data from `shared-career-projects` module with a hybrid context strategy: featured projects get full details in system prompt, all projects listed in an index for awareness.

## Goals

1. **Interview/recruiter questions** - Answer "Tell me about your BLE experience" with project context
2. **Portfolio exploration** - Users can explore specific projects with follow-up questions
3. **Context efficiency** - Stay within 8-10K token budget

## Data Architecture

### Files to Create

```
shared-career-projects/src/commonMain/composeResources/files/
└── personal_info.json          # NEW: identity + skills
```

### Files to Keep (unchanged)

```
shared-career-projects/src/commonMain/composeResources/files/projects/
├── *_details_data.json         # Full project details
└── *_timeline_data.json        # Timeline/card view data
```

### Files to Remove

- `cv_data.json` (location TBD - find and remove)

### personal_info.json Structure

```json
{
  "name": "Denys Honcharenko",
  "title": "Senior Android Engineer",
  "location": "...",
  "email": "...",
  "linkedin": "...",
  "github": "...",
  "summary": "12+ years mobile development...",
  "skills": [
    { "category": "Languages", "items": ["Kotlin", "Java", "Swift"] },
    { "category": "Frameworks", "items": ["Compose", "KMP", "Jetpack"] }
  ]
}
```

## System Prompt Strategy

### Token Budget (~7-8K)

| Section | Tokens | Content |
|---------|--------|---------|
| Instructions | ~300 | Role, formatting rules, suggestion format |
| Personal Info | ~150 | Identity + summary |
| Skills | ~200 | All skill categories |
| Project Index | ~500 | 13 projects × {id, name, role, period, tagline} |
| Featured Projects | ~5000 | 5 projects × curated details |
| **Total** | ~6150 | Leaves ~2-4K for conversation |

### Featured Projects (Full Curated Details)

1. Geosatis
2. McDonald's
3. Adidas GMR
4. Food Network Kitchen
5. Android School

### Curated Details Format (for featured projects)

Extract from `*_details_data.json`:
- `overview`: company, client, product, role, period
- `description.short` + `description.full`
- `technologies.primary`: just list of names
- `challenge`: context (if notable)

**Skip:** hero, meta, seo, media, links, lifecycle, team structure, achievements

### Project Index Format (all 13 projects)

```
- adidas-gmr: "Adidas GMR" | Lead Android | Feb-Jun 2020 | Google Jacquard smart insole for FIFA Mobile
- mcdonalds: "McDonald's" | Senior Android | 2019-2020 | Global restaurant ordering app
- stoamigo: "Stoamigo" | Senior Android | 2023 | IoT smart home platform
...
```

### Non-Featured Projects (8 total)

stoamigo, veev, lesara, valentina, smildroid, rifl-media, aitweb, kntu-it

LLM has index tagline only. No on-demand retrieval (tool calling skipped).

## LLM Response Format

### Response Structure

```
[Natural language answer about Denys...]

```json
{"suggestions": ["adidas-gmr", "mcdonalds"]}
```
```

### System Prompt Instruction

```
End your response with a JSON block suggesting 0-3 related projects the user might want to explore:
```json
{"suggestions": ["project-id-1", "project-id-2"]}
```
Only suggest projects relevant to the question. Use exact project IDs from the project index.
If no projects are relevant, use an empty array: {"suggestions": []}
```

### Suggestion Extraction Logic

1. Find JSON code block at end of response (regex: ` ```json\s*(\{.*?\})\s*``` `)
2. Parse `suggestions` array
3. Strip JSON block from displayed message content
4. Render project chips below message
5. Tap chip → navigate to project detail screen

## Code Changes

### Remove

| File | Reason |
|------|--------|
| `cv_data.json` | Replaced by personal_info.json + project files |
| `ReferenceExtractor.kt` | Replace with SuggestionExtractor |
| `CVRepository.kt` | Remove or simplify (no reference resolution) |
| `CVData.kt` model | Remove (replaced by new models) |
| `CVDataLoader.kt` | Remove |

### Create

| File | Purpose |
|------|---------|
| `personal_info.json` | Identity + skills data |
| `PersonalInfo.kt` | Model for personal_info.json |
| `SkillCategory.kt` | Model for skill categories |
| `ProjectSummary.kt` | Minimal project index model |
| `SuggestionExtractor.kt` | Parse JSON suggestions from LLM response |
| `AgentDataProvider.kt` | Load and provide data for system prompt |

### Modify

| File | Changes |
|------|---------|
| `SystemPromptBuilder.kt` | New prompt structure with featured projects |
| `ChatViewModel.kt` | Use new data sources, suggestion extraction |
| `AppModule.kt` | Updated DI for new components |
| `Message.kt` | Change `references` to `suggestions: List<String>` |

## Domain Models

### PersonalInfo.kt

```kotlin
@Serializable
data class PersonalInfo(
    val name: String,
    val title: String,
    val location: String,
    val email: String,
    val linkedin: String,
    val github: String,
    val summary: String,
    val skills: List<SkillCategory>
)

@Serializable
data class SkillCategory(
    val category: String,
    val items: List<String>
)
```

### ProjectSummary.kt

```kotlin
@Serializable
data class ProjectSummary(
    val id: String,
    val name: String,
    val role: String,
    val period: String,
    val tagline: String,
    val featured: Boolean = false
)
```

### SuggestionResult.kt

```kotlin
data class SuggestionResult(
    val cleanedContent: String,
    val suggestions: List<String>  // project IDs
)
```

## Implementation Notes

### Loading Featured Project Details

For the 5 featured projects, load from `*_details_data.json` and extract:

```kotlin
fun extractCuratedDetails(detailsJson: ProjectDetails): String {
    return buildString {
        // Overview
        appendLine("Company: ${detailsJson.overview.company}")
        detailsJson.overview.client?.let { appendLine("Client: $it") }
        appendLine("Product: ${detailsJson.overview.product}")
        appendLine("Role: ${detailsJson.overview.role}")
        appendLine("Period: ${detailsJson.overview.period.displayText}")

        // Description
        appendLine()
        appendLine(detailsJson.description.short)
        appendLine(detailsJson.description.full)

        // Technologies
        appendLine()
        append("Technologies: ")
        appendLine(detailsJson.technologies.primary.map { it.name }.joinToString(", "))

        // Challenge (if present)
        detailsJson.challenge?.let {
            appendLine()
            appendLine("Challenge: ${it.context}")
            appendLine("Response: ${it.response}")
        }
    }
}
```

### Suggestion Extraction Regex

```kotlin
private val suggestionPattern = Regex(
    """```json\s*(\{"suggestions":\s*\[.*?\]\})\s*```""",
    RegexOption.DOT_MATCHES_ALL
)

fun extract(content: String): SuggestionResult {
    val match = suggestionPattern.find(content)
    if (match == null) {
        return SuggestionResult(content, emptyList())
    }

    val jsonStr = match.groupValues[1]
    val suggestions = json.decodeFromString<SuggestionsPayload>(jsonStr).suggestions
    val cleanedContent = content.replace(match.value, "").trim()

    return SuggestionResult(cleanedContent, suggestions)
}
```

## Testing Strategy

1. **Unit tests for SuggestionExtractor** - various JSON formats, edge cases
2. **Unit tests for SystemPromptBuilder** - verify token budget, format
3. **Integration test** - full flow from user message to suggestion chips

## Open Questions

None - design approved.
