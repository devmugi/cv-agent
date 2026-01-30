# Compose Multiplatform & Material 3 Design System Best Practices

Research compiled for Arcane Design System contribution planning.

## Sources

- [Android Developers: Custom Design Systems](https://developer.android.com/develop/ui/compose/designsystems/custom)
- [Android Developers: Anatomy of a Theme](https://developer.android.com/develop/ui/compose/designsystems/anatomy)
- [Jetpack Compose Component API Guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md)
- [Chris Banes: Slotting in with Compose UI](https://chrisbanes.me/posts/slotting-in-with-compose-ui/)
- [GetStream: Composition Patterns in Compose](https://getstream.io/blog/composition-pattern-compose/)
- [Bumble Tech: Refining Compose API for Design Systems](https://medium.com/bumble-tech/refining-compose-api-for-design-systems-d652e2c2eac3)
- [CometChat: Chat App Design Best Practices](https://www.cometchat.com/blog/chat-app-design-best-practices)
- [BricxLabs: Chat UI Design Patterns](https://bricxlabs.com/blogs/message-screen-ui-deisgn)
- [JetBrains KMP Roadmap 2025](https://blog.jetbrains.com/kotlin/2025/08/kmp-roadmap-aug-2025/)
- [Material Design 3](https://m3.material.io/)

---

## 1. Theme Architecture

### CompositionLocal Pattern

Use `staticCompositionLocalOf` for design tokens that don't change during composition:

```kotlin
@Immutable
data class CustomColors(
    val primary: Color,
    val onPrimary: Color,
    val surface: Color
)

val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        primary = Color.Unspecified,
        onPrimary = Color.Unspecified,
        surface = Color.Unspecified
    )
}

object CustomTheme {
    val colors: CustomColors
        @Composable
        get() = LocalCustomColors.current
}
```

**Key principles:**
- Use `staticCompositionLocalOf` for tokens (colors, typography, spacing) - more efficient, doesn't trigger recomposition
- Use `compositionLocalOf` only for values that change frequently at runtime
- Mirror MaterialTheme API with companion objects for consistency
- Provide sensible defaults (e.g., `Color.Unspecified`, `TextStyle.Default`)

### Theme Subsystems

A complete theme typically includes:
- **Colors** - Semantic color tokens with light/dark variants
- **Typography** - Text styles for different purposes
- **Shapes** - Corner radius and shape definitions
- **Spacing** - Consistent spacing tokens
- **Elevation** - Shadow and surface hierarchy

### Material 3 Surface System

M3 uses a 5-level tonal surface hierarchy:
1. `surfaceContainerLowest`
2. `surfaceContainerLow`
3. `surfaceContainer` (default)
4. `surfaceContainerHigh`
5. `surfaceContainerHighest`

**Best practice:** Use neutral shadows, not colored glows. State layer alphas: Hover (8%), Pressed (12%), Focus (12%), Dragged (16%).

---

## 2. Component API Design

### Parameter Ordering

Follow this strict sequence:

```kotlin
@Composable
fun CustomButton(
    onClick: () -> Unit,                    // 1. Required parameters
    modifier: Modifier = Modifier,          // 2. Modifier (always first optional)
    enabled: Boolean = true,                // 3. Optional parameters
    style: ButtonStyle = ButtonStyle.Primary,
    content: @Composable RowScope.() -> Unit // 4. Trailing content lambda
)
```

### Slot API Pattern

**Prefer composable lambdas over specific types:**

```kotlin
// BAD - restricts flexibility
@Composable
fun MessageBubble(
    text: String,  // Can't use AnnotatedString or custom rendering
    timestamp: String
)

// GOOD - slot API pattern
@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    footer: @Composable () -> Unit = {}
)
```

**Benefits:**
- Single responsibility - component handles layout, caller handles content
- Avoids over-parameterized APIs
- Simplifies testing (test pieces in isolation)
- Prevents code duplication (no CardWithIcon, CardWithLongBody variants)

### Naming Conventions

- Use specification prefixes for variants: `OutlinedButton`, `FilledButton`, `TextButton`
- Use `Basic*` prefix for barebones components without design opinions
- Most common variant gets the unprefixed name
- Avoid company/module prefixes in component names

### State Hoisting

```kotlin
// PREFERRED - stateless, caller manages state
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
)

// AVOID - MutableState creates split ownership
@Composable
fun TextField(
    value: MutableState<String>,  // Don't do this
    modifier: Modifier = Modifier
)
```

### Defaults Pattern

```kotlin
object MessageBubbleDefaults {
    val Shape: Shape = RoundedCornerShape(16.dp)
    val ContentPadding = PaddingValues(12.dp)

    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = MaterialTheme.colorScheme.onSurface
    ) = MessageBubbleColors(containerColor, contentColor)
}
```

**Key principles:**
- Default expressions must be public (not private/internal calls)
- Namespace defaults in `ComponentDefaults` object
- Avoid null as signal for "use internal default"

---

## 3. Chat UI Component Patterns

### Message Bubbles

**Layout principles:**
- Sent messages: right-aligned
- Received messages: left-aligned
- Rounded corners preferred over sharp edges
- Group consecutive messages from same sender (reduce visual clutter)
- Visual differentiation via color, not just position

**Content blocks:**
- Text (with markdown support)
- Images (with loading states)
- Code blocks (with syntax highlighting)
- Suggestions/chips
- Custom content slots

### Input Field

**Best practices:**
- Position at bottom (40% faster response times in studies)
- Support multiline editing (users need to review before sending)
- Clear border and placeholder text
- Attachment button for media
- Voice input option
- Send button state tied to input validity

### Feedback States

Essential states to implement:
- Typing indicators
- Read receipts
- Loading/streaming states
- Error states (inline or toast)
- Message status (sending, sent, delivered, read)

### Message Actions

Common actions pattern:
- Copy to clipboard
- Share
- React (like/dislike)
- Regenerate (for AI responses)
- Edit (for user messages)
- Delete

---

## 4. Compose Multiplatform Considerations

### Platform Targets (2025-2026)

| Platform | Status | Notes |
|----------|--------|-------|
| Android | Stable | Primary target |
| iOS | Stable (1.8.0+) | Full parity with Android |
| Desktop | Stable | JVM-based |
| Web (Wasm) | Beta | Ready for early adopters |

### Build Optimization

- iOS builds are slow - make them optional (`buildIos=false` by default)
- Desktop catalog apps enable fast iteration (~10s builds)
- Use convention plugins for shared build logic
- Keep platform-specific code minimal

### Performance Best Practices

1. **Scope state properly** - Keep state close to composables that use it
2. **Avoid overloading recomposition** - Don't pass large objects directly
3. **Use profiling tools** - Android Studio Profiler, Skia debugger
4. **Minimize platform checks** - Use expect/actual for platform differences

---

## 5. Library Publishing

### Module Organization

```
design-foundation/    → Tokens, theme, primitives
design-components/    → Reusable UI components
design-chat/          → Domain-specific (chat) components
catalog/              → Demo/showcase app
```

### Maven Central Requirements

- PGP signing for all artifacts
- Root module must contain JAR without classifier
- Namespace verification (e.g., `io.github.username`)
- Use convention plugins for consistent publishing

### Versioning

Follow semantic versioning:
- MAJOR: Breaking API changes
- MINOR: New features, backward compatible
- PATCH: Bug fixes

---

## 6. Design System Governance

### Team Structure

- Design council to approve new components
- Component library in catalog app for review
- Enforce consistent use via linting
- Training for engineers on design principles

### Common Pitfalls to Avoid

1. **Mixing M2 and M3** - Causes inconsistent padding/typography
2. **Overriding tokens without understanding semantics** - Causes accessibility issues
3. **Overusing intense colors/shadows** - Creates visual chaos
4. **Adding rounded corners without holistic approach** - Doesn't guarantee cohesion

### Component Contribution Checklist

- [ ] Follows parameter ordering convention
- [ ] Uses slot APIs where appropriate
- [ ] Has sensible defaults in `ComponentDefaults` object
- [ ] Supports theming via CompositionLocal
- [ ] Is stateless (state hoisted to caller)
- [ ] Has preview annotations
- [ ] Added to catalog app
- [ ] Has tests

---

## 7. Recommendations for Arcane Contributions

Based on this research, contributions from CV Agent to Arcane should:

1. **Use slot APIs** for flexible content - Don't hardcode text/icon types
2. **Follow M3 surface hierarchy** - Use tonal elevation, not colored shadows
3. **Provide Defaults objects** - Make customization discoverable
4. **Keep components stateless** - Hoist state to callers
5. **Add to catalog** - Every component needs a showcase
6. **Consider multiplatform** - Test on Android + Desktop minimum
7. **Document with KDoc** - Explain parameters and usage

### Components Worth Contributing

From CV Agent's custom components:
- `OutlinedUserMessageBlock` - User message variant with border styling
- `AnimatedChatInput` - Input with focus animation
- `FloatingInputContainer` - Blurred container pattern
- `MessageActions` - Copy/share/like/regenerate actions
- `WelcomeSection` - Initial state with suggestions
- `SuggestionChipsGrid` - Grid layout for chips

### Generalization Needed

Before contributing, generalize:
- Remove CV-specific text/content
- Add slot APIs for custom content
- Create Defaults objects for colors/shapes
- Add theme-aware color resolution
- Support both light and dark themes
