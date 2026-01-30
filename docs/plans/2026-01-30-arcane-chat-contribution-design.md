# Arcane Design System Chat Components Contribution

**Date:** 2026-01-30
**Status:** Draft
**Author:** Claude Code

## Overview

Contribute CV Agent's chat UI patterns to Arcane Design System as generalized, reusable components. This consolidates custom components into the shared library, enabling reuse across projects while following Compose Multiplatform and Material 3 best practices.

## Goals

1. **Consolidation** - Move CV Agent's custom chat components into Arcane
2. **Generalization** - Make components flexible via slot APIs and Defaults objects
3. **Consistency** - Follow Compose API guidelines and Arcane patterns
4. **Cleanup** - Replace CV Agent's custom components with library imports

## Non-Goals

- Redesigning existing Arcane chat components
- Adding iOS-specific implementations
- Breaking changes to existing Arcane APIs

## Research

Best practices research documented in: `docs/compose-multiplatform-best-practices.md`

Key principles applied:
- Slot APIs for flexible content
- Parameter ordering: required → modifier → optional → content lambda
- Defaults objects for discoverability
- Stateless components with hoisted state
- Theme-aware colors via CompositionLocal

---

## Phases

| Phase | Focus | Components | Complexity |
|-------|-------|------------|------------|
| 1 | Message variants | `ArcaneOutlinedUserMessageBlock` | Low |
| 2 | Message actions | `ArcaneMessageActions` | Medium |
| 3 | Input enhancements | `ArcaneFloatingInputContainer` + input animation | Medium |
| 4 | Welcome state | `ArcaneChatWelcomeSection` + `ArcaneSuggestionChipsGrid` | Medium |

---

## Phase 1: Message Variants

### Component: `ArcaneOutlinedUserMessageBlock`

User message block with outlined border styling (no fill). Complements the existing filled `ArcaneUserMessageBlock`.

### API

```kotlin
@Composable
fun ArcaneOutlinedUserMessageBlock(
    modifier: Modifier = Modifier,
    colors: OutlinedUserMessageBlockColors = OutlinedUserMessageBlockDefaults.colors(),
    shape: Shape = OutlinedUserMessageBlockDefaults.Shape,
    border: BorderStroke = OutlinedUserMessageBlockDefaults.border(),
    contentPadding: PaddingValues = OutlinedUserMessageBlockDefaults.ContentPadding,
    content: @Composable () -> Unit
)

@Immutable
data class OutlinedUserMessageBlockColors(
    val containerColor: Color,
    val contentColor: Color
)

object OutlinedUserMessageBlockDefaults {
    val Shape: Shape = RoundedCornerShape(16.dp)
    val ContentPadding = PaddingValues(12.dp)

    @Composable
    fun colors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = ArcaneTheme.colors.text
    ): OutlinedUserMessageBlockColors = OutlinedUserMessageBlockColors(
        containerColor = containerColor,
        contentColor = contentColor
    )

    @Composable
    fun border(
        color: Color = ArcaneTheme.colors.primary.copy(alpha = 0.4f),
        width: Dp = 1.dp
    ): BorderStroke = BorderStroke(width, color)
}
```

### Files

- `arcane-chat/src/commonMain/kotlin/io/github/devmugi/arcane/design/chat/components/messages/ArcaneOutlinedUserMessageBlock.kt`
- Add showcase to `catalog-chat/`

### CV Agent Migration

Replace `OutlinedUserMessageBlock.kt` with:

```kotlin
ArcaneOutlinedUserMessageBlock {
    Markdown(content = message.content, ...)
}
```

---

## Phase 2: Message Actions

### Component: `ArcaneMessageActions`

Action buttons for chat messages: copy, share, like, dislike, regenerate.

### API

```kotlin
@Composable
fun ArcaneMessageActions(
    onCopy: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onLike: (() -> Unit)? = null,
    onDislike: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    likeState: LikeState = LikeState.None,
    colors: MessageActionsColors = MessageActionsDefaults.colors(),
    arrangement: Arrangement.Horizontal = Arrangement.spacedBy(4.dp)
)

enum class LikeState { None, Liked, Disliked }

@Immutable
data class MessageActionsColors(
    val iconColor: Color,
    val activeColor: Color
)

object MessageActionsDefaults {
    @Composable
    fun colors(
        iconColor: Color = ArcaneTheme.colors.textSecondary,
        activeColor: Color = ArcaneTheme.colors.primary
    ): MessageActionsColors = MessageActionsColors(
        iconColor = iconColor,
        activeColor = activeColor
    )
}
```

### Behavior

- Null callbacks hide that action (declarative visibility)
- Like/Dislike mutually exclusive with `LikeState` enum
- Unbounded ripple (16.dp radius) for touch feedback
- Horizontal row with configurable arrangement

### Files

- `arcane-chat/src/commonMain/kotlin/io/github/devmugi/arcane/design/chat/components/actions/ArcaneMessageActions.kt`
- `arcane-chat/src/commonMain/kotlin/io/github/devmugi/arcane/design/chat/components/actions/MessageActionsDefaults.kt`
- Add showcase to `catalog-chat/`

### CV Agent Migration

Replace `MessageActions.kt` with:

```kotlin
ArcaneMessageActions(
    onCopy = { clipboardManager.setText(...) },
    onShare = { shareSheet.show(...) },
    onLike = { viewModel.like(message) },
    onDislike = { viewModel.dislike(message) },
    likeState = message.likeState
)
```

---

## Phase 3: Input Enhancements

### Component A: `ArcaneFloatingInputContainer`

Blurred, semi-transparent container for floating input fields over scrollable content.

### API

```kotlin
@Composable
fun ArcaneFloatingInputContainer(
    modifier: Modifier = Modifier,
    blur: Dp = FloatingInputContainerDefaults.BlurRadius,
    colors: FloatingInputContainerColors = FloatingInputContainerDefaults.colors(),
    contentPadding: PaddingValues = FloatingInputContainerDefaults.ContentPadding,
    content: @Composable () -> Unit
)

@Immutable
data class FloatingInputContainerColors(
    val containerColor: Color,
    val gradientColor: Color
)

object FloatingInputContainerDefaults {
    val BlurRadius = 20.dp
    val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

    @Composable
    fun colors(
        containerColor: Color = ArcaneTheme.colors.surface.copy(alpha = 0.85f),
        gradientColor: Color = ArcaneTheme.colors.surface
    ): FloatingInputContainerColors = FloatingInputContainerColors(
        containerColor = containerColor,
        gradientColor = gradientColor
    )
}
```

### Component B: Input Animation

Add focus animation to existing `ArcaneAgentChatInput`.

### API Addition

```kotlin
@Composable
fun ArcaneAgentChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    // ... existing parameters ...
    animateFocus: Boolean = false,           // NEW
    focusWidthFraction: Float = 0.9f,        // NEW - unfocused width
    focusAnimationDuration: Int = 200,       // NEW - animation ms
    // ... rest ...
)
```

### Behavior

- When `animateFocus = true`, input width animates from `focusWidthFraction` (90%) to 100% on focus
- Animation duration configurable (default 200ms)
- Backward compatible - disabled by default

### Files

- `arcane-chat/src/commonMain/kotlin/io/github/devmugi/arcane/design/chat/components/input/ArcaneFloatingInputContainer.kt` (new)
- `arcane-chat/src/commonMain/kotlin/io/github/devmugi/arcane/design/chat/components/input/ArcaneAgentChatInput.kt` (modify)
- Add showcase to `catalog-chat/`

### CV Agent Migration

Replace `FloatingInputContainer.kt` and `AnimatedChatInput.kt` with:

```kotlin
ArcaneFloatingInputContainer {
    ArcaneAgentChatInput(
        value = input,
        onValueChange = { input = it },
        onSend = { viewModel.send(input) },
        animateFocus = true,
        focusWidthFraction = 0.9f
    )
}
```

---

## Phase 4: Welcome State

### Component A: `ArcaneChatWelcomeSection`

Welcome/empty state for chat screens with slots for title, subtitle, and suggestions.

### API

```kotlin
@Composable
fun ArcaneChatWelcomeSection(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    subtitle: @Composable () -> Unit = {},
    suggestions: @Composable () -> Unit = {},
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
)
```

### Component B: `ArcaneSuggestionChipsGrid`

Grid layout for suggestion chips.

### API

```kotlin
@Composable
fun ArcaneSuggestionChipsGrid(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    chipContent: @Composable (String) -> Unit = { suggestion ->
        ArcaneSuggestionChip(
            text = suggestion,
            onClick = { onSuggestionClick(suggestion) }
        )
    }
)
```

### Files

- `arcane-chat/src/commonMain/kotlin/io/github/devmugi/arcane/design/chat/components/welcome/ArcaneChatWelcomeSection.kt`
- `arcane-chat/src/commonMain/kotlin/io/github/devmugi/arcane/design/chat/components/welcome/ArcaneSuggestionChipsGrid.kt`
- Add showcase to `catalog-chat/`

### CV Agent Migration

Replace `WelcomeSection.kt` and `SuggestionChipsGrid.kt` with:

```kotlin
ArcaneChatWelcomeSection(
    title = {
        Text(
            "Ask me anything",
            style = ArcaneTheme.typography.headlineLarge
        )
    },
    subtitle = {
        Text(
            "I can help you explore my CV and career projects",
            style = ArcaneTheme.typography.bodyLarge
        )
    },
    suggestions = {
        ArcaneSuggestionChipsGrid(
            suggestions = viewModel.initialSuggestions,
            onSuggestionClick = { viewModel.sendMessage(it) }
        )
    }
)
```

---

## Implementation Plan

### Git Strategy

```bash
# In ArcaneDesignSystem repo
git worktree add ../arcane-worktrees/cv-agent-contrib feature/cv-agent-chat-components

# Work in worktree
cd ../arcane-worktrees/cv-agent-contrib
```

### Development Workflow (per phase)

1. Create component in `arcane-chat/`
2. Add Defaults object
3. Add to `catalog-chat/` showcase
4. Run desktop catalog: `./gradlew :catalog-chat:composeApp:run`
5. Run tests: `./gradlew :arcane-chat:allTests`
6. Commit and PR to Arcane main
7. After merge: bump version in CV Agent
8. Replace custom component with library import
9. Delete custom component from CV Agent
10. Commit CV Agent changes

### Version Bumps

| Phase | Arcane Version |
|-------|---------------|
| 1 | 0.4.0-alpha01 |
| 2 | 0.4.0-alpha02 |
| 3 | 0.4.0-alpha03 |
| 4 | 0.4.0-alpha04 |
| Final | 0.4.0 |

### CV Agent Files to Delete After Migration

```
shared-ui/src/commonMain/kotlin/io/github/devmugi/cv/agent/ui/components/
├── OutlinedUserMessageBlock.kt  → Phase 1
├── MessageActions.kt            → Phase 2
├── FloatingInputContainer.kt    → Phase 3
├── AnimatedChatInput.kt         → Phase 3
├── WelcomeSection.kt            → Phase 4
└── SuggestionChipsGrid.kt       → Phase 4
```

---

## Testing Strategy

### Arcane Tests

- Unit tests for Defaults objects
- Compose UI tests for component rendering
- Preview annotations for visual inspection
- Catalog showcase for manual testing

### CV Agent Tests

- Existing UI tests should pass after migration
- No behavior changes expected

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking existing Arcane consumers | New components only, no changes to existing APIs except additive params |
| Build time increase | Keep iOS builds disabled, test on Android/Desktop only |
| Theme incompatibility | Test all 11 Arcane themes in catalog |
| Animation performance | Use `animateFloatAsState` with standard easing |

---

## Success Criteria

- [ ] All 4 phases completed and merged to Arcane
- [ ] CV Agent updated to Arcane 0.4.0
- [ ] 6 custom components deleted from CV Agent
- [ ] All components showcased in catalog-chat
- [ ] No regressions in CV Agent UI tests
- [ ] Documentation in Arcane README updated
