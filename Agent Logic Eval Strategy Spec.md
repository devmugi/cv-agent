# Agent Logic Eval Strategy Spec

## 1. Overview

### Purpose
Systematically evaluate and optimize the CV Agent's prompt engineering, data strategy, and model selection to improve response quality, relevance, and cost efficiency.

### Goals
- Establish baseline metrics for current implementation
- Test hypotheses about prompt and data format improvements
- Compare LLM models for quality/cost trade-offs
- Find optimal balance between response quality and token cost
- Document findings for future iterations

---

## 2. Current State

### Architecture
```
ChatViewModel.sendMessage()
    ↓
SystemPromptBuilder.buildWithMetadata()
    → Returns: SystemPromptResult(prompt, version="1.0.0", variant)
    ↓
GroqApiClient.streamChatCompletion()
    → Model: llama-3.3-70b-versatile
    → Temperature: 0.7, MaxTokens: 1024
    ↓
OpenTelemetryAgentTracer (Phoenix)
    → Captures: TTFT, tokens, latency, version, variant
```

### System Prompt Components
| Component | Format | Current Implementation |
|-----------|--------|------------------------|
| Instructions | Plain text | Role + access description |
| Personal Info | Plain text | Name, title, location, email, links, summary |
| Skills | Plain text | 8 categories with items |
| Project Index | Plain text | All 13 projects: `id | name | role | period | tagline` |
| Featured Projects | Plain text | 5 projects (CURATED) or 13 (ALL_PROJECTS) with full details |
| Suggestion Format | JSON instruction | Requires `{"suggestions": ["id"]}` at response end |

### Existing Variants
| Variant | Featured Projects | Token Cost | Coverage |
|---------|-------------------|------------|----------|
| CURATED | 5 (geosatis, mcdonalds, adidas-gmr, food-network-kitchen, android-school) | Lower | Partial |
| ALL_PROJECTS | 13 (all projects) | Higher | Complete |

### Current Metrics (via Phoenix)
- `llm.token_count.prompt` / `completion` / `total`
- `llm.latency.time_to_first_token_ms`
- `llm.prompt.version` (1.0.0)
- `llm.prompt.variant` (CURATED / ALL_PROJECTS)
- `session.id`, `llm.turn_number` (conversations)

---

## 3. Evaluation Dimensions

### 3.1 Assistant Prompt

**Current:**
```
You are an AI assistant for Denys Honcharenko's portfolio. Answer questions
about Denys in third person. Be helpful, professional, and concise.

You have access to:
- Personal information and skills
- A project index with all projects (id, name, role, period, tagline)
- Full details for 5 featured projects

For non-featured projects, use the project index information.
```

**Hypotheses to Test:**

| Variant | Hypothesis | Change |
|---------|------------|--------|
| `PERSONA_RECRUITER` | Recruiter-focused language improves relevance | Add "...helping recruiters and hiring managers understand..." |
| `PERSONA_DETAILED` | More specific instructions = better responses | Add explicit guidance on response length, structure |
| `PERSONA_CONCISE` | Shorter instructions = same quality, fewer tokens | Minimize instruction text |
| `ROLE_FIRST_PERSON` | First person may feel more personal | Change "third person" to allow first person |

### 3.2 Personal Info

**Current Format (Plain Text):**
```
# PERSONAL INFO
Name: Denys Honcharenko
Title: Senior Android Engineer
Location: Genk, Belgium
...
```

**Hypotheses to Test:**

| Variant | Format | Hypothesis |
|---------|--------|------------|
| `INFO_TEXT` (current) | Plain text | Baseline |
| `INFO_JSON` | Structured JSON | Better for extraction tasks |
| `INFO_MARKDOWN` | Markdown with headers | Better readability for LLM |
| `INFO_MINIMAL` | Name + title + summary only | Lower tokens, sufficient for most queries |

**Fields to Evaluate:**
- Core: name, title, location (always include)
- Contact: email, linkedin, github (include vs exclude)
- Summary: full vs short version
- Skills: all 8 categories vs top 4 relevant

### 3.3 Projects Data Strategy

**Current:** Full details for featured projects, index-only for others

**Hypotheses to Test:**

| Variant | Strategy | Hypothesis |
|---------|----------|------------|
| `PROJ_CURATED` (current) | 5 featured + index | Good balance |
| `PROJ_ALL_FULL` | 13 projects full details | Better accuracy, higher cost |
| `PROJ_INDEX_ONLY` | Index for all, no full details | Low cost, LLM can infer |
| `PROJ_SELECTIVE_FIELDS` | All projects, but only: name, role, period, tagline, technologies, standout | Medium cost, key info preserved |
| `PROJ_TOOL_RETRIEVAL` | Index only + tool to fetch on-demand | RAG-like approach |

**Fields to Evaluate for Selective Strategy:**
- Essential: id, name, role, period, tagline
- Important: technologies.primary, standout, metrics.quickStats
- Optional: description.full, achievements, team

### 3.4 Data Format

**Hypotheses:**

| Format | Token Efficiency | LLM Parsing | Use Case |
|--------|------------------|-------------|----------|
| Plain Text | Medium | Good | Current default |
| JSON | Lower (no labels) | Excellent | Structured extraction |
| Markdown | Higher (headers) | Good | Readability |
| YAML | Medium | Good | Hierarchical data |

### 3.5 Model Comparison

**Models to Evaluate (Groq):**

| Model | Size | Speed | Cost | Expected Quality |
|-------|------|-------|------|------------------|
| `llama-3.3-70b-versatile` (current) | 70B | Medium | Higher | Best |
| `llama-3.1-70b-versatile` | 70B | Medium | Higher | Good |
| `llama-3.1-8b-instant` | 8B | Fast | Lower | Acceptable? |
| `mixtral-8x7b-32768` | 46.7B (MoE) | Fast | Medium | Good |

**Hypotheses:**
- Smaller models may be sufficient for simple questions (Q1-Q5)
- 70B models needed for complex multi-project queries
- Cost savings potential: 8B for simple, 70B for complex (routing)

---

## 4. Test Matrix

### Priority 1: Quick Wins (Low Effort)
| Test ID | Dimension | Current | Variant | Expected Impact |
|---------|-----------|---------|---------|-----------------|
| A1 | Assistant Prompt | Current | PERSONA_CONCISE | -10% tokens, same quality |
| P1 | Personal Info | Full | INFO_MINIMAL | -5% tokens |

### Priority 2: Format Experiments
| Test ID | Dimension | Current | Variant | Expected Impact |
|---------|-----------|---------|---------|-----------------|
| F1 | Projects Data | Text | JSON | Better accuracy on tech questions |
| F2 | Personal Info | Text | Markdown | Improved skill extraction |

### Priority 3: Model Comparison
| Test ID | Dimension | Current | Variant | Expected Impact |
|---------|-----------|---------|---------|-----------------|
| M1 | Model | 70B | 8B-instant | -80% cost, -?% quality |
| M2 | Model | 70B | mixtral-8x7b | -50% cost, -?% quality |

### Priority 4: Architecture Changes
| Test ID | Dimension | Current | Variant | Expected Impact |
|---------|-----------|---------|---------|-----------------|
| D1 | Projects | CURATED | PROJ_SELECTIVE_FIELDS | Balance of coverage + cost |
| D2 | Projects | Static | PROJ_TOOL_RETRIEVAL | Dynamic, lower base cost |

---

## 5. Metrics & Success Criteria

### Quality Metrics
| Metric | Measurement | Success Threshold |
|--------|-------------|-------------------|
| Accuracy | Manual review of 10 questions | >90% factually correct |
| Relevance | Suggestion accuracy (suggested project exists and is relevant) | >80% relevant suggestions |
| Completeness | Does response answer the question fully? | >85% complete answers |

### Performance Metrics
| Metric | Source | Target |
|--------|--------|--------|
| TTFT | `llm.latency.time_to_first_token_ms` | <1000ms |
| Total Latency | Span duration | <3000ms |
| Prompt Tokens | `llm.token_count.prompt` | Minimize while maintaining quality |

### Cost Metrics
| Metric | Calculation | Target |
|--------|-------------|--------|
| Token Efficiency | completion_tokens / prompt_tokens | >0.1 (10% response vs input) |
| Cost per Query | prompt_tokens * rate + completion_tokens * rate | Track trend |

---

## 6. Implementation Plan

### Phase 1: Baseline
1. Create `:eval` JVM module
2. Run all questions with current config
3. Export Phoenix data for baseline metrics
4. Document: avg TTFT, avg tokens, accuracy rate

### Phase 2: Prompt Variations
1. Create `PromptVariant` enum with test variants
2. Create variant instruction files
3. Run tests for each variant
4. Compare metrics in Phoenix

### Phase 3: Model Comparison
1. Add model parameter to EvalConfig
2. Run same questions across different models
3. Compare quality vs cost trade-offs
4. Identify model routing opportunities

### Phase 4: Data Format Experiments
1. Create `DataFormatter` interface with TEXT, JSON, MARKDOWN implementations
2. Test each format with same questions
3. Measure token efficiency and accuracy

### Phase 5: Tool-Based Approach (Optional)
1. Implement `ProjectRetrievalTool` for on-demand project fetching
2. Modify system prompt to describe tool availability
3. Compare with static approaches

---

## 7. Tooling

### 7.1 New `:eval` JVM Module

**Purpose:** Standalone evaluation runner with flexible configuration

**Location:** `/eval` module at project root

**Structure:**
```
eval/
├── build.gradle.kts
├── src/main/kotlin/
│   ├── Main.kt                 # Entry point
│   ├── config/
│   │   ├── EvalConfig.kt       # Configuration model
│   │   └── ConfigLoader.kt     # Load from YAML/env
│   ├── runner/
│   │   ├── EvalRunner.kt       # Orchestrates evaluation runs
│   │   └── QuestionSet.kt      # Test questions
│   ├── variants/
│   │   ├── PromptVariant.kt    # Prompt variations
│   │   ├── DataFormat.kt       # TEXT, JSON, MARKDOWN
│   │   └── ModelConfig.kt      # Model selection
│   ├── metrics/
│   │   ├── MetricsCollector.kt # Collect from responses
│   │   └── PhoenixExporter.kt  # Export to Phoenix
│   └── report/
│       └── ReportGenerator.kt  # Generate comparison reports
└── src/main/resources/
    ├── questions/
    │   ├── simple_questions.yaml
    │   └── conversations.yaml
    └── prompts/
        ├── instructions_baseline.txt
        ├── instructions_concise.txt
        └── instructions_recruiter.txt
```

**Dependencies:**
```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":shared-agent"))
    implementation(project(":shared-agent-api"))
    implementation(project(":shared-career-projects"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kaml)  // YAML parsing
    implementation(libs.clikt) // CLI parsing
}

application {
    mainClass.set("io.github.devmugi.cv.agent.eval.MainKt")
}
```

**CLI Usage:**
```bash
# Run baseline evaluation
./gradlew :eval:run --args="--variant=BASELINE --model=llama-3.3-70b"

# Run specific variant
./gradlew :eval:run --args="--variant=PERSONA_CONCISE --format=JSON"

# Run model comparison
./gradlew :eval:run --args="--compare-models --questions=simple"

# Run all variants
./gradlew :eval:run --args="--all-variants"
```

**EvalConfig Model:**
```kotlin
data class EvalConfig(
    val promptVariant: PromptVariant = PromptVariant.BASELINE,
    val dataFormat: DataFormat = DataFormat.TEXT,
    val projectMode: ProjectContextMode = ProjectContextMode.CURATED,
    val model: String = "llama-3.3-70b-versatile",
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024,
    val questionSet: QuestionSet = QuestionSet.SIMPLE,
    val delayMs: Long = 10_000
)

enum class PromptVariant {
    BASELINE,
    PERSONA_CONCISE,
    PERSONA_RECRUITER,
    PERSONA_DETAILED,
    ROLE_FIRST_PERSON
}

enum class DataFormat {
    TEXT,
    JSON,
    MARKDOWN
}

enum class QuestionSet {
    SIMPLE,        // Q1-Q10
    CONVERSATIONS, // Conv1-Conv10
    ALL
}
```

### 7.2 Variant Files

**Location:** `eval/src/main/resources/prompts/`

```
instructions_baseline.txt:
You are an AI assistant for Denys Honcharenko's portfolio...

instructions_concise.txt:
Portfolio assistant for Denys Honcharenko. Answer in third person, be concise.

instructions_recruiter.txt:
You help recruiters and hiring managers understand Denys Honcharenko's experience...
```

### 7.3 Phoenix Integration

**Span Attributes for Eval:**
- `eval.variant` - Prompt variant name
- `eval.data_format` - TEXT/JSON/MARKDOWN
- `eval.question_id` - Q1, Q2, Conv1, etc.
- `eval.run_id` - Unique ID for this eval run

**Phoenix Analysis Queries:**
```graphql
query CompareVariants($runId: String!) {
  node(id: "PROJECT_ID") {
    ... on Project {
      spans(filter: { attributes: { eval.run_id: $runId } }) {
        edges {
          node {
            name
            latencyMs
            attributes
          }
        }
      }
    }
  }
}
```

---

## 8. Verification

### Running Evaluations
```bash
# Start Phoenix
phoenix serve

# Run baseline
./gradlew :eval:run --args="--variant=BASELINE"

# Run all variants comparison
./gradlew :eval:run --args="--all-variants --report"

# View results
open http://localhost:6006
```

### Phoenix Analysis Checklist
- [ ] Export spans for each variant
- [ ] Calculate avg TTFT per variant
- [ ] Calculate avg prompt tokens per variant
- [ ] Manual review of 10 responses per variant
- [ ] Compare suggestion accuracy
- [ ] Generate comparison report

---

## 9. Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| TBD | Baseline established | Starting point for comparison |
| TBD | Variant X selected | Best balance of quality/cost |
| TBD | Model Y selected | Optimal for use case |

---

## 10. Open Questions

1. What's the acceptable quality threshold for smaller models?
2. Should suggestion format be optional (some queries don't need it)?
3. What's the acceptable token budget per query?
4. Should we implement caching for repeated questions?
5. Should we add automatic quality scoring (LLM-as-judge)?
