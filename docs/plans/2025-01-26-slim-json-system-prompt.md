# Slim JSON System Prompt

## Problem

The system prompt sends full `CareerProject` JSON objects to the LLM, including UI-only fields (hero, meta, media, seo) that waste tokens.

Current state:
- ALL_PROJECTS mode: ~49,000 tokens
- Pretty-printed JSON adds ~25% overhead from whitespace

## Solution

Filter project fields at serialization time and use compact JSON.

### Fields to Keep

| Field | Reason |
|-------|--------|
| id, name, slug, tagline | Identity |
| overview | Role, period, company, location |
| companies | Employer/client context |
| description.full | Project description (skip short, howItWorked) |
| achievements | Key accomplishments with impact |
| team | Team size, structure, methodology |
| metrics | Downloads, countries, stats |
| technologies | Tech stack used |

### Fields to Exclude

- hero (UI gradient colors, icons)
- meta (featured flag, visibility)
- description.short (redundant with full)
- description.howItWorked (step-by-step UI content)
- challenge (less useful than achievements)
- standout (redundant with achievements)
- links (URLs not useful for Q&A)
- lifecycle (project status timeline)
- media (screenshots, videos, logos)
- seo (web metadata)
- relatedProjects, lastUpdated

## Implementation

**File:** `shared-agent/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/SystemPromptBuilder.kt`

### Change 1: Compact JSON encoder

```kotlin
// Before:
private val prettyJson = Json { prettyPrint = true }

// After:
private val compactJson = Json { prettyPrint = false }
```

### Change 2: Add slim projection function

```kotlin
private fun CareerProject.toSlimJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("name", name)
    put("slug", slug)
    tagline?.let { put("tagline", it) }
    overview?.let { put("overview", Json.encodeToJsonElement(it)) }
    companies?.let { put("companies", Json.encodeToJsonElement(it)) }
    description?.full?.let { put("description", it) }
    achievements?.let { put("achievements", Json.encodeToJsonElement(it)) }
    team?.let { put("team", Json.encodeToJsonElement(it)) }
    metrics?.let { put("metrics", Json.encodeToJsonElement(it)) }
    technologies?.let { put("technologies", Json.encodeToJsonElement(it)) }
}
```

### Change 3: Update serialization

```kotlin
// Before:
appendLine(prettyJson.encodeToString(project))

// After:
appendLine(compactJson.encodeToString(project.toSlimJson()))
```

## Expected Results

| Metric | Before | After | Savings |
|--------|--------|-------|---------|
| Field filtering | 100% | ~60% | ~40% |
| JSON formatting | pretty | compact | ~25% |
| **Combined** | ~49,000 tokens | ~22,000-25,000 tokens | **~50-55%** |

## Verification

1. Run unit tests: `./gradlew :shared-agent:testAndroidUnitTest`
2. Run evaluation tests with tracing:
   ```bash
   GROQ_TEST_DELAY_MS=10000 ./gradlew :shared-agent-api:evaluationTests --tests "*Q1*"
   ```
3. Check Phoenix traces for reduced `llm.token_count.prompt` values
