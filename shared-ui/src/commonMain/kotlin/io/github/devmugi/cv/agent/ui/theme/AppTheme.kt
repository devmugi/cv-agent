package io.github.devmugi.cv.agent.ui.theme

import io.github.devmugi.arcane.design.foundation.theme.ArcaneColors

enum class ThemeVariant(val displayName: String) {
    ARCANE("Arcane"),
    PERPLEXITY("Perplexity"),
    P2D("P2D"),
    P2L("P2L"),
    CLAUDE_D("Claude Dark"),
    CLAUDE_L("Claude Light"),
    AGENT2D("Agent2 Dark"),
    AGENT2L("Agent2 Light")
}

fun ThemeVariant.toColors(): ArcaneColors = when (this) {
    ThemeVariant.ARCANE -> ArcaneColors.default()
    ThemeVariant.PERPLEXITY -> ArcaneColors.perplexity()
    ThemeVariant.P2D -> ArcaneColors.p2d()
    ThemeVariant.P2L -> ArcaneColors.p2l()
    ThemeVariant.CLAUDE_D -> ArcaneColors.claudeD()
    ThemeVariant.CLAUDE_L -> ArcaneColors.claudeL()
    ThemeVariant.AGENT2D -> ArcaneColors.agent2Dark()
    ThemeVariant.AGENT2L -> ArcaneColors.agent2Light()
}

fun ThemeVariant.isLight(): Boolean = when (this) {
    ThemeVariant.P2L, ThemeVariant.CLAUDE_L, ThemeVariant.AGENT2L -> true
    else -> false
}

val DEFAULT_THEME = ThemeVariant.AGENT2L
