# Welcome Screen Improvement Design

## Overview

Improve the chat app's welcome screen with:
1. Vertical list layout (instead of grid)
2. Scrollable when keyboard appears
3. Proper bottom padding to clear the floating chat input
4. 8 curated topic suggestions covering diverse categories

## Current State

- Uses `ArcaneEmptyState` component with bordered card
- `SuggestionChipsGrid` displays suggestions in 2-column grid
- Grid layout causes awkward text wrapping on some chips
- No accommodation for keyboard visibility
- Already vertically centered via `ArcaneChatScreenScaffold`

## Design

### Component Architecture

```
WelcomeSection
├── LazyColumn (scrollable)
│   ├── Welcome header (title + subtitle)
│   └── 8 TopicListItem rows (clickable list items)
└── Bottom spacer (200dp for chat input clearance)
```

### Visual Design

**Welcome Header:**
- "Welcome!" title with `displaySmall` typography
- Subtitle: "I'm Denys's AI assistant. Ask me anything about his professional experience, skills, or projects."
- Centered text alignment
- 24dp spacing after subtitle

**Topic List Items:**
- `bodyLarge` typography with `ArcaneTheme.colors.primary`
- Left-aligned text
- 16dp vertical padding per item
- No dividers between items
- Ripple effect on tap

**Container:**
- No card/border wrapper (remove `ArcaneEmptyState`)
- Content floats directly in screen
- Horizontal padding: 24dp
- Bottom padding: 200dp (clears floating input + disclaimer)

**Scrolling:**
- `LazyColumn` handles keyboard compression
- User can scroll to see all topics when keyboard visible

### Topic Suggestions

8 diverse topics based on evaluation test questions:

| # | Category | Question |
|---|----------|----------|
| 1 | Current Role | "What's Denys's current role?" |
| 2 | Jetpack Compose | "Has he worked with Jetpack Compose?" |
| 3 | KMP | "What's his Kotlin Multiplatform experience?" |
| 4 | Featured Project | "Tell me about the McDonald's app" |
| 5 | Featured Project | "What did he build at GEOSATIS?" |
| 6 | IoT/Hardware | "Tell me about the Adidas GMR project" |
| 7 | Teaching | "Has he trained other developers?" |
| 8 | Leadership | "Has Denys led teams before?" |

## Implementation

### Files to Modify

1. **`WelcomeSection.kt`** - Complete rewrite
   - Replace `ArcaneEmptyState` + `SuggestionChipsGrid` with `LazyColumn`
   - Add `TopicListItem` composable
   - Add bottom spacer for input clearance
   - Define `DEFAULT_WELCOME_TOPICS` constant

2. **`ChatScreen.kt`** - Minor update
   - Pass new default topics to `WelcomeSection`

3. **`SuggestionChipsGrid.kt`** - Keep unchanged
   - Still used for project chips after assistant messages

### Code Structure

```kotlin
// WelcomeSection.kt

val DEFAULT_WELCOME_TOPICS = listOf(
    "What's Denys's current role?",
    "Has he worked with Jetpack Compose?",
    "What's his Kotlin Multiplatform experience?",
    "Tell me about the McDonald's app",
    "What did he build at GEOSATIS?",
    "Tell me about the Adidas GMR project",
    "Has he trained other developers?",
    "Has Denys led teams before?"
)

@Composable
fun WelcomeSection(
    suggestions: List<String> = DEFAULT_WELCOME_TOPICS,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
)

@Composable
private fun TopicListItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

### Testing

- Keep existing `welcome_section` test tag
- Add `topic_item_$index` test tags for each row
- Verify scrolling behavior with keyboard

## Out of Scope

- Animation when transitioning from welcome to chat
- Voice input integration
- Dynamic topic suggestions based on context
