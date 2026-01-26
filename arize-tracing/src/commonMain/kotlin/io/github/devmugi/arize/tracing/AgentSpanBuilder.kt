package io.github.devmugi.arize.tracing

/**
 * DSL builder for configuring an Agent tracing span.
 *
 * Agent spans represent higher-level orchestration that may contain
 * one or more LLM calls, tool calls, or other operations.
 */
class AgentSpanBuilder {
    internal var name: String = "Agent"
    internal var sessionId: String? = null
    internal var userId: String? = null
    internal var installationId: String? = null
    internal var metadata: MutableMap<String, Any> = mutableMapOf()
    internal var tags: MutableList<String> = mutableListOf()

    /**
     * Sets the agent name displayed in Phoenix UI.
     * Example: "ChatAgent", "ResearchAgent", "TripPlanner"
     */
    fun name(name: String) {
        this.name = name
    }

    /**
     * Sets the session ID for conversation tracking.
     */
    fun sessionId(id: String) {
        sessionId = id
    }

    /**
     * Sets the user ID for user-level analytics.
     */
    fun userId(id: String) {
        userId = id
    }

    /**
     * Sets the device installation ID.
     */
    fun installationId(id: String) {
        installationId = id
    }

    /**
     * Adds metadata key-value pairs.
     */
    fun metadata(vararg pairs: Pair<String, Any>) {
        metadata.putAll(pairs)
    }

    /**
     * Adds categorizing tags.
     */
    fun tags(vararg tags: String) {
        this.tags.addAll(tags)
    }
}
