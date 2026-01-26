# Enhanced System Prompt Evaluation: Model Comparison

**Goal:** Compare model performance using production `SystemPromptBuilder` (APP_DEFAULT format) with full `personal_info.json` data including `agentClarifications`.

---

## Configuration

| Setting | Value |
|---------|-------|
| **DataFormat** | `APP_DEFAULT` (exact match to production app) |
| **ProjectMode** | `ALL_PROJECTS` (all 13 projects with JSON details) |
| **Questions** | `WELCOME` (W1-W8) |
| **Delay** | 1000ms |
| **Temperature** | 0.7 |

### Key System Prompt Features
- **Instructions** with BOUNDARY_INSTRUCTIONS (response tiers, never-do)
- **agentClarifications** (critical corrections about employment status, roles)
- **Personal info** (name, title, location, email, linkedin, github, summary)
- **Skills** (8 categories)
- **Project index** (all 13 projects)
- **Project details** in **JSON format** (all 13 projects)
- **Suggestion instructions**

---

## Performance Summary

| Metric | llama-3.3-70b | gpt-oss-20b | gpt-oss-120b | Winner |
|--------|---------------|-------------|--------------|--------|
| **Avg Latency** | 3341ms | **1798ms** | 2959ms | **gpt-oss-20b** |
| **Avg TTFT** | 3338ms | **1526ms** | 2954ms | **gpt-oss-20b** |
| **P50 Latency** | 3357ms | **922ms** | 3049ms | **gpt-oss-20b** |
| **P95 Latency** | **3516ms** | 3214ms | 3763ms | gpt-oss-20b |
| **Success Rate** | 8/8 (100%) | 7/8 (87.5%)* | 8/8 (100%) | llama, gpt-120b |

*gpt-oss-20b returned empty response for W3 (Kotlin Multiplatform question)

---

## Run Details

| Model | Run ID | Timestamp |
|-------|--------|-----------|
| llama-3.3-70b-versatile | fb37193a | 2026-01-26T16:27:10Z |
| openai/gpt-oss-20b | 98a73ab4 | 2026-01-26T16:33:40Z |
| openai/gpt-oss-120b | e7d6564e | 2026-01-26T16:40:18Z |

---

## Question-by-Question Comparison

### W1: "What's Denys's current role?"

| Model | Latency | Response Quality | Correctly Uses agentClarifications |
|-------|---------|------------------|-----------------------------------|
| llama-3.3-70b | 3055ms | Good - mentions not employed | Yes |
| gpt-oss-20b | 3189ms | Good - seeking opportunities | Yes |
| gpt-oss-120b | 3049ms | Best - detailed with contact info | Yes |

**Key finding:** All models correctly use `agentClarifications` to state Denys is NOT currently employed.

---

### W2: "Has he worked with Jetpack Compose?"

| Model | Latency | Response Quality | Suggestions |
|-------|---------|------------------|-------------|
| llama-3.3-70b | 3357ms | Good - mentions McDonald's, GEOSATIS | mcdonalds, geosatis |
| gpt-oss-20b | **763ms** | Good - table format | mcdonalds, food-network-kitchen, geosatis |
| gpt-oss-120b | 3786ms | Best - detailed table with Arcane | mcdonalds, food-network-kitchen, geosatis |

---

### W3: "What's his Kotlin Multiplatform experience?"

| Model | Latency | Response Quality | Suggestions |
|-------|---------|------------------|-------------|
| llama-3.3-70b | 3418ms | Good - bullet points | mcdonalds, geosatis |
| gpt-oss-20b | 3684ms | **EMPTY RESPONSE** | - |
| gpt-oss-120b | 3763ms | Best - detailed table with CV-Agent | mcdonalds, geosatis |

**Issue:** gpt-oss-20b failed to generate a response for this question.

---

### W4: "Tell me about the McDonald's app"

| Model | Latency | Response Quality | Suggestions |
|-------|---------|------------------|-------------|
| llama-3.3-70b | 3896ms | Good - comprehensive | lesara, food-network-kitchen, geosatis |
| gpt-oss-20b | **922ms** | Excellent - structured with bullet points | geosatis, food-network-kitchen, adidas-gmr |
| gpt-oss-120b | 1841ms | Best - detailed with timeline | geosatis, food-network-kitchen |

---

### W5: "What did he build at GEOSATIS?"

| Model | Latency | Response Quality | Suggestions |
|-------|---------|------------------|-------------|
| llama-3.3-70b | 3360ms | Good - concise | mcdonalds, geosatis, adidas-gmr |
| gpt-oss-20b | **1097ms** | Excellent - table format | mcdonalds, food-network-kitchen |
| gpt-oss-120b | 3544ms | Best - comprehensive with real impact | adidas-gmr, veev |

---

### W6: "Tell me about the Adidas GMR project"

| Model | Latency | Response Quality | Suggestions |
|-------|---------|------------------|-------------|
| llama-3.3-70b | 3516ms | Good - mentions BLE emulator | mcdonalds, food-network-kitchen, geosatis |
| gpt-oss-20b | 3214ms | Excellent - table format | mcdonalds, geosatis |
| gpt-oss-120b | 3476ms | Best - detailed with metrics | mcdonalds, geosatis |

**All models correctly state:** "Team Lead in EPAM Android team" (using agentClarifications)

---

### W7: "Has he trained other developers?"

| Model | Latency | Response Quality | Suggestions |
|-------|---------|------------------|-------------|
| llama-3.3-70b | 3166ms | Good - mentions Android School | android-school, mcdonalds, geosatis |
| gpt-oss-20b | **796ms** | Excellent - structured | android-school, lesara |
| gpt-oss-120b | 2938ms | Best - hiring rate details | android-school, rifl-media, lesara |

---

### W8: "Has Denys led teams before?"

| Model | Latency | Response Quality | Suggestions |
|-------|---------|------------------|-------------|
| llama-3.3-70b | **2961ms** | Good - concise | adidas-gmr, mcdonalds |
| gpt-oss-20b | **718ms** | Excellent - includes Veev | mcdonalds, adidas-gmr, veev |
| gpt-oss-120b | 1272ms | Best - includes Android School | mcdonalds, adidas-gmr, android-school |

---

## Quality Analysis

### Response Formatting

| Model | Style | Tables | Bullet Points | JSON Suggestions |
|-------|-------|--------|---------------|------------------|
| llama-3.3-70b | Paragraphs | Rare | Frequent | Inline |
| gpt-oss-20b | Structured | Frequent | Frequent | Code block |
| gpt-oss-120b | Detailed | Frequent | Frequent | Code block |

### agentClarifications Usage

All models correctly used the critical clarifications:
- "Not currently employed" in W1
- "Team Lead in EPAM Android team" for Adidas GMR in W6
- "Led 2 Android teams" for McDonald's in W4

---

## Recommendations

### For Production Use: **gpt-oss-20b**
- **46% faster** than llama-3.3-70b (1798ms vs 3341ms)
- Excellent formatting with tables
- Good suggestion quality
- **Caution:** May return empty responses for complex questions (W3)

### For Quality-First Use: **gpt-oss-120b**
- Most detailed and comprehensive responses
- Best formatting and structure
- Includes relevant context and metrics
- 11% faster than llama-3.3-70b

### Current Default (llama-3.3-70b): Reliable but Slow
- 100% success rate
- Consistent response quality
- Slowest latency (3341ms avg)
- Less sophisticated formatting

---

## Key Findings

1. **APP_DEFAULT format works correctly** - All models use `agentClarifications` data
2. **gpt-oss-20b has reliability issue** - Empty response for W3
3. **gpt-oss-120b best quality** - Most detailed, well-formatted responses
4. **gpt-oss-20b fastest** - 46% faster than llama, 39% faster than gpt-120b
5. **Suggestion quality varies** - gpt-oss models suggest more diverse projects

---

## Files

| Report | Model |
|--------|-------|
| `2026-01-26_172710_fb37193a_BASELINE_APP_DEFAULT.json` | llama-3.3-70b-versatile |
| `2026-01-26_173340_98a73ab4_BASELINE_APP_DEFAULT.json` | openai/gpt-oss-20b |
| `2026-01-26_174018_e7d6564e_BASELINE_APP_DEFAULT.json` | openai/gpt-oss-120b |
