# Phase 4: UI Implementation Design

## Overview

Phase 4 delivers the complete user-facing chat interface for the CV Agent app, connecting the UI layer to the existing ChatViewModel and Groq API integration from Phase 3.

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Markdown library | multiplatform-markdown-renderer | Purpose-built for KMP, actively maintained |
| Reference chip behavior | Tooltip/popup on tap | Informative without being intrusive |
| Streaming UX | Live text streaming | ChatGPT-like feel, polished experience |
| Empty state | Centered welcome + suggestions | Clean and welcoming |
| Error handling | Inline error message with retry | Contextual and non-disruptive |
| Input during loading | Disabled with visual feedback | Safe, prevents confusion |
| Animation level | Subtle and functional | Professional without distraction |
| TopBar | Title only | Clean and focused |
| Suggestion chips layout | 2x2 grid | Balanced, centered look |

## Architecture

### Component Structure

```
ChatScreen (main container)
├── TopBar
├── MessageList (LazyColumn)
│   ├── WelcomeSection (empty state)
│   │   ├── WelcomeMessage
│   │   └── SuggestionChipsGrid (2x2)
│   ├── MessageBubble (user)
│   ├── MessageBubble (assistant)
│   │   ├── MarkdownContent
│   │   └── ReferenceChipsRow
│   └── ErrorMessage (when applicable)
└── MessageInput
    ├── TextField
    └── SendButton
```

### Data Flow

The existing `ChatViewModel` exposes `ChatState` via StateFlow:

- `messages: List<Message>` → MessageList content
- `isLoading: Boolean` → Input disabled state
- `isStreaming: Boolean` → Live text updates
- `currentStreamingContent: String` → Partial message during stream
- `error: String?` → ErrorMessage display
- `suggestions: List<String>` → SuggestionChipsGrid (shown only when messages empty)

### File Organization

```
ui/
├── ChatScreen.kt (~80 LOC)
├── components/
│   ├── TopBar.kt (~30 LOC)
│   ├── MessageBubble.kt (~60 LOC)
│   ├── MessageInput.kt (~50 LOC)
│   ├── SuggestionChipsGrid.kt (~40 LOC)
│   ├── ReferenceChip.kt (~50 LOC)
│   ├── WelcomeSection.kt (~40 LOC)
│   └── ErrorMessage.kt (~30 LOC)
└── theme/ (existing, minor additions)
```

## Visual Design

### Color Application

| Element | Color | Token |
|---------|-------|-------|
| Screen background | #1a1d2e | `background` |
| Message bubbles (user) | #f5a623 (gold) | `primary` |
| Message bubbles (assistant) | #1e2746 (dark blue) | `surface` |
| User message text | #1a1d2e (dark) | `onPrimary` |
| Assistant message text | #ffffff (white) | `onSurface` |
| TopBar background | #1e2746 | `surface` |
| TopBar title "<DH/>" | #f5a623 (gold) | `primary` |
| TopBar title "CV Agent" | #ffffff | `onSurface` |
| Input field background | #1e2746 | `surface` |
| Input field border (focused) | #f5a623 | `primary` |
| Send button | #f5a623 | `primary` |
| Suggestion chip border | #f5a623 | `primary` |
| Suggestion chip text | #f5a623 | `primary` |
| Reference chip background | #2a3a5c | `surfaceVariant` |
| Error message background | #3d2936 | `errorContainer` |
| Error text | #ffb4ab | `onErrorContainer` |

### Markdown Styling

- **Bold/Italic**: Standard weight variations in white
- **Code inline**: Gold text (#ffc947) with dark background (#252a3d)
- **Code blocks**: Dark background (#252a3d), monospace font, gold syntax
- **Links**: Gold (#f5a623), underlined
- **Lists**: Gold bullet points, white text
- **Headers**: Slightly larger, gold color

## Component Specifications

### TopBar
- Height: 56dp (standard)
- Centered title with "<DH/>" in gold, "CV Agent" in white
- Surface background with subtle bottom elevation/shadow
- No actions or navigation icons

### MessageBubble
- Max width: 85% of screen width
- User messages: Aligned right, gold background, rounded corners (16dp, top-right 4dp)
- Assistant messages: Aligned left, surface background, rounded corners (16dp, top-left 4dp)
- Padding: 12dp horizontal, 8dp vertical
- Markdown content rendered inside assistant bubbles
- Reference chips appear below message text with 8dp top margin

### MessageInput
- Fixed at bottom, surface background
- TextField with rounded corners (24dp), placeholder "Ask about my experience..."
- Send button: Circular, gold background, white arrow icon
- Disabled state: 50% opacity, non-interactive
- Horizontal padding: 16dp, vertical: 8dp

### SuggestionChipsGrid
- 2x2 grid with 8dp gap between chips
- Each chip: Outlined style, gold border, gold text, transparent background
- Tap triggers `onSuggestionClick(text)` → sends message
- Chips disappear once first message is sent

### ReferenceChip
- Small pill shape, surfaceVariant background
- Text format: "Experience" or "Skill: Kotlin" (type + optional label)
- Tap shows tooltip popup with 2-3 line summary from CV data
- Tooltip: Surface background, white text, appears above chip, auto-dismisses after 3s or on tap outside

## Interactions & Animations

### Message Appearance
- New messages fade in + slide up slightly (150ms, ease-out)
- User messages appear instantly after send
- Assistant messages: streaming content updates without animation (just text appending)

### Streaming Behavior
- When `isStreaming == true`, show assistant bubble with `currentStreamingContent`
- Text appends character-by-character as received from SSE
- When stream completes, final message replaces streaming content seamlessly
- Auto-scroll to bottom as new content arrives

### Input Interactions
- Send button: Ripple effect on tap
- Disabled state transition: 200ms fade to 50% opacity
- TextField focus: Border animates to gold (150ms)

### Suggestion Chips
- Tap: Ripple effect, then chip grid fades out (200ms)
- Selected text immediately appears as user message

### Reference Chip Tooltip
- Tap: Tooltip fades in above chip (150ms)
- Tooltip has small pointer/arrow toward chip
- Auto-dismiss after 3 seconds, or tap anywhere to dismiss
- Only one tooltip visible at a time

### Error Message
- Appears as system message with fade-in
- Retry button: Standard ripple, triggers `onRetry()`
- On retry: Error message fades out, loading state begins

### Scroll Behavior
- LazyColumn reversed (newest at bottom)
- Auto-scroll to bottom on new message
- User can scroll up to view history
- If scrolled up, new messages don't force scroll (preserve position)

## Testing Strategy

### Unit Tests (Component Logic)
- `ReferenceChip` tooltip content extraction from CVData
- `MessageBubble` role-based styling selection
- `SuggestionChipsGrid` visibility logic (shown only when messages empty)

### Compose UI Tests

| Component | Test Cases |
|-----------|------------|
| `TopBar` | Renders title correctly, gold accent applied |
| `MessageBubble` | User vs assistant styling, markdown renders, reference chips display |
| `MessageInput` | Text input works, send button clickable, disabled state respected |
| `SuggestionChipsGrid` | All 4 chips render, tap triggers callback, 2x2 layout |
| `ReferenceChip` | Renders label, tap shows tooltip, tooltip dismisses |
| `WelcomeSection` | Welcome text displays, suggestions visible |
| `ErrorMessage` | Error text shows, retry button clickable |
| `ChatScreen` | Integration: empty state → send message → response appears |

### Test File Structure

```
commonTest/
└── io/github/devmugi/cv/agent/ui/
    ├── ChatScreenTest.kt (~80 LOC)
    └── components/
        ├── TopBarTest.kt (~25 LOC)
        ├── MessageBubbleTest.kt (~50 LOC)
        ├── MessageInputTest.kt (~40 LOC)
        ├── SuggestionChipsGridTest.kt (~35 LOC)
        ├── ReferenceChipTest.kt (~40 LOC)
        └── ErrorMessageTest.kt (~25 LOC)
```

### Coverage Target
80%+ for UI layer, matching Phase 3 standards.

## Implementation Order

### New Dependency

```kotlin
// build.gradle.kts (commonMain)
implementation("com.mikepenz:multiplatform-markdown-renderer:0.13.0")
```

### Order (dependencies flow top-to-bottom)

1. **Add markdown library** - Update gradle, verify builds on both platforms
2. **Theme additions** - Add any missing color tokens (surfaceVariant, errorContainer)
3. **Leaf components** (no dependencies, can parallelize):
   - `TopBar`
   - `ErrorMessage`
   - `SuggestionChipsGrid`
4. **ReferenceChip** - Needs CVRepository access for tooltip content
5. **MessageBubble** - Depends on markdown library + ReferenceChip
6. **MessageInput** - Standalone, but tested with loading state
7. **WelcomeSection** - Composes SuggestionChipsGrid
8. **ChatScreen** - Integrates all components with ChatViewModel
9. **Platform entry points** - Connect ChatScreen to Android/iOS apps
10. **UI tests** - Written alongside or immediately after each component

## Deliverables

- ~8 new component files (~380 LOC implementation)
- ~7 test files (~295 LOC tests)
- 1 gradle dependency addition
- Minor theme updates

## Quality Gates

- All existing tests still pass
- New UI tests pass
- ktlint/detekt clean
- Builds successfully on Android + iOS

## Success Criteria

- [ ] All UI components render correctly on Android and iOS
- [ ] Chat interface fully functional with ChatViewModel
- [ ] Markdown formatting renders properly
- [ ] Suggested questions appear and are clickable
- [ ] Loading and error states display correctly
- [ ] Message bubbles style properly (user vs agent)
- [ ] Reference chips show CV section information with tooltips
- [ ] Smooth animations and transitions
- [ ] All UI tests passing
- [ ] Quality gates (ktlint, detekt) passing
