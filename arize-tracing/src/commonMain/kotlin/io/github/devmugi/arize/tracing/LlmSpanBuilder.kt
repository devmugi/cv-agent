package io.github.devmugi.arize.tracing

import io.github.devmugi.arize.tracing.models.ChatMessage
import io.github.devmugi.arize.tracing.models.PromptTemplate
import io.github.devmugi.arize.tracing.models.TokenPricing

/**
 * DSL builder for configuring an LLM tracing span.
 */
@Suppress("TooManyFunctions")
class LlmSpanBuilder {
    internal var model: String? = null
    internal var provider: String? = null
    internal var systemPrompt: String? = null
    internal var messages: List<ChatMessage> = emptyList()
    internal var sessionId: String? = null
    internal var turnNumber: Int? = null
    internal var userId: String? = null
    internal var installationId: String? = null
    internal var promptTemplate: PromptTemplate? = null
    internal var promptVersion: String? = null
    internal var promptVariant: String? = null
    internal var pricing: TokenPricing? = null
    internal var metadata: MutableMap<String, Any> = mutableMapOf()
    internal var tags: MutableList<String> = mutableListOf()
    internal var temperature: Double? = null
    internal var maxTokens: Int? = null

    /**
     * Sets the model name (required).
     * Example: "llama-3.3-70b-versatile", "gpt-4", "claude-3-opus"
     */
    fun model(name: String) {
        model = name
    }

    /**
     * Sets the provider name.
     * Example: "groq", "openai", "anthropic", "azure"
     */
    fun provider(name: String) {
        provider = name
    }

    /**
     * Sets the system prompt content.
     */
    fun systemPrompt(content: String) {
        systemPrompt = content
    }

    /**
     * Sets the conversation messages.
     */
    fun messages(messages: List<ChatMessage>) {
        this.messages = messages
    }

    /**
     * Sets the session ID for conversation tracking.
     */
    fun sessionId(id: String) {
        sessionId = id
    }

    /**
     * Sets the turn number within a conversation.
     */
    fun turnNumber(turn: Int) {
        turnNumber = turn
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
     * Sets the prompt template for Arize Playground experimentation.
     *
     * @param template The template string with placeholders (e.g., "Hello {name}")
     * @param version Optional version identifier (e.g., "v1.0")
     * @param variables Optional map of placeholder values
     */
    fun promptTemplate(
        template: String,
        version: String? = null,
        variables: Map<String, Any>? = null
    ) {
        promptTemplate = PromptTemplate(template, version, variables)
    }

    /**
     * Sets the prompt version for tracking prompt iterations.
     * Use this for simple version tracking without full template instrumentation.
     */
    fun promptVersion(version: String) {
        promptVersion = version
    }

    /**
     * Sets the prompt variant for A/B testing or configuration tracking.
     * Example: "CURATED", "ALL_PROJECTS", "v2_experimental"
     */
    fun promptVariant(variant: String) {
        promptVariant = variant
    }

    /**
     * Sets the token pricing for cost calculation.
     * Cost is calculated automatically when complete() is called with token usage.
     *
     * @param promptPerMillion Cost per million prompt tokens
     * @param completionPerMillion Cost per million completion tokens
     */
    fun pricing(promptPerMillion: Double, completionPerMillion: Double) {
        pricing = TokenPricing(promptPerMillion, completionPerMillion)
    }

    /**
     * Adds metadata key-value pairs.
     * Use vendor prefixes to avoid conflicts (e.g., "myapp.environment").
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

    /**
     * Sets the temperature parameter.
     */
    fun temperature(value: Double) {
        temperature = value
    }

    /**
     * Sets the max tokens parameter.
     */
    fun maxTokens(value: Int) {
        maxTokens = value
    }

    internal fun validate() {
        requireNotNull(model) { "model is required" }
    }
}
