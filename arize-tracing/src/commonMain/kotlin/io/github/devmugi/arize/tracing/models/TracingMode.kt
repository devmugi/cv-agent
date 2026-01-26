package io.github.devmugi.arize.tracing.models

enum class TracingMode {
    /**
     * Production mode uses BatchSpanProcessor for async export.
     * Better performance but spans may be lost on crash.
     */
    PRODUCTION,

    /**
     * Testing mode uses SimpleSpanProcessor for sync export.
     * Deterministic behavior, spans exported immediately.
     */
    TESTING
}
