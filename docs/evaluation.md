# Evaluation Framework

The `eval/` module provides a framework for testing prompt variants and comparing agent performance.

## Running Evaluations

```bash
# Start Phoenix for trace collection
phoenix serve

# Run baseline evaluation (SIMPLE questions)
./gradlew :eval:eval -PevalVariant=BASELINE -PevalQuestions=SIMPLE

# Run with a different prompt variant
./gradlew :eval:eval -PevalVariant=PERSONA_CONCISE

# Run all questions including conversations
./gradlew :eval:eval -PevalQuestions=ALL

# Custom delay between API calls (default: 5000ms)
./gradlew :eval:eval -PevalDelayMs=10000
```

## Configuration Options

| Property | Values | Default |
|----------|--------|---------|
| `evalVariant` | BASELINE, PERSONA_CONCISE, PERSONA_RECRUITER, PERSONA_DETAILED, ROLE_FIRST_PERSON | BASELINE |
| `evalModel` | Any Groq model ID | llama-3.3-70b-versatile |
| `evalProjectMode` | CURATED, ALL_PROJECTS | CURATED |
| `evalFormat` | TEXT, JSON, MARKDOWN | TEXT |
| `evalQuestions` | SIMPLE, CONVERSATIONS, ALL | SIMPLE |
| `evalDelayMs` | Milliseconds between API calls | 5000 |

## Comparing Runs

After running evaluations with different variants, compare them:

```bash
# Compare baseline vs a variant
./gradlew :eval:compare -PbaselineRun=<run-id-1> -PvariantRun=<run-id-2>
```

Run IDs are printed at the end of each evaluation and appear in report filenames.

## Reports

Reports are saved to `eval/reports/`:

```
eval/reports/
├── 2026-01-24_222440_95f36218_BASELINE_TEXT.json   # Full structured data
├── 2026-01-24_222440_95f36218_BASELINE_TEXT.md     # Human-readable summary
└── comparisons/
    └── baseline_vs_concise.json                     # Comparison results
```

## Report Contents

Each report includes:
- **Summary**: Success rate, avg latency, TTFT, P50/P95 latency
- **Per-question results**: Latency, TTFT, suggested projects, response text
- **Configuration**: Variant, model, format, project mode

Example markdown report:

| ID | Category | Latency | TTFT | Suggestions | Status |
|----|----------|---------|------|-------------|--------|
| Q1 | personal info | 846ms | 819ms | - | OK |
| Q2 | skills | 613ms | 605ms | mcdonalds, geosatis | OK |
| Q3 | featured project | 923ms | 916ms | mcdonalds, android-school | OK |
