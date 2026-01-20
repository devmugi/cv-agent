# CV Agent - Product Requirements Document

**Project:** CV Agent
**Version:** 1.0
**Date:** 2026-01-20
**Author:** Denys Honcharenko
**Package:** `io.github.devmugi.cv.agent`

---

## 1. Project Overview

### 1.1 Purpose

**CV Agent** is a cross-platform mobile application (Android & iOS) that provides an interactive AI-powered chatbot interface for exploring Denys Honcharenko's professional background. The app demonstrates modern Kotlin Multiplatform and AI integration capabilities as a portfolio piece.

### 1.2 Goals

- **Primary:** Showcase advanced KMP, Compose Multiplatform, and AI integration skills
- **Secondary:** Provide an engaging way for recruiters and hiring managers to explore professional experience
- **Technical:** Demonstrate production-quality code with comprehensive testing and quality gates

### 1.3 Target Audience

- Recruiters and hiring managers evaluating candidates
- Technical peers interested in portfolio work
- Potential employers exploring background and skills

---

## 2. Architecture & Tech Stack

### 2.1 Core Architecture

- **Compose Multiplatform** for shared UI across Android and iOS
- **Kotlin Multiplatform Mobile (KMM)** for shared business logic
- **Groq API** for AI agent responses with hybrid conversational + citation style
- **Embedded CV data** in the app (no backend required)
- **Ktor Client** for HTTP communication with Groq

### 2.2 Technology Stack

| Layer | Technology |
|-------|------------|
| UI Framework | Compose Multiplatform (latest stable) |
| Language | Kotlin 2.0+ |
| Networking | Ktor Client |
| Serialization | Kotlinx Serialization |
| Concurrency | Kotlinx Coroutines + Flow |
| AI Provider | Groq API (mixtral-8x7b-32768 or llama-3) |
| Markdown | multiplatform-markdown-renderer |
| Testing | Kotlin Test, MockK, Compose Testing |
| Code Quality | Detekt, Ktlint, Kover |

### 2.3 Project Structure

```
cv-agent/
├── shared/
│   ├── commonMain/
│   │   ├── kotlin/io/github/devmugi/cv/agent/
│   │   │   ├── data/            # CV data models
│   │   │   ├── api/             # Groq API client
│   │   │   ├── agent/           # Chat logic & state
│   │   │   ├── ui/              # Compose UI screens
│   │   │   │   ├── components/  # Reusable UI components
│   │   │   │   └── theme/       # Design system
│   │   │   └── util/            # Utilities
│   │   └── resources/
│   │       └── cv_data.json     # Structured CV data
│   ├── commonTest/              # Shared tests
│   ├── androidMain/             # Android-specific code
│   └── iosMain/                 # iOS-specific code
├── androidApp/                  # Android app entry point
├── iosApp/                      # iOS app entry point
├── gradle/
└── build.gradle.kts
```

---

## 3. Data Model & CV Data

### 3.1 Core Data Models

```kotlin
package io.github.devmugi.cv.agent.data

@Serializable
data class CVData(
    val personalInfo: PersonalInfo,
    val summary: String,
    val skills: List<SkillCategory>,
    val experience: List<WorkExperience>,
    val projects: List<Project>,
    val achievements: List<Achievement>,
    val education: Education
)

@Serializable
data class PersonalInfo(
    val name: String,
    val location: String,
    val email: String,
    val phone: String,
    val linkedin: String,
    val github: String,
    val portfolio: String
)

@Serializable
data class SkillCategory(
    val id: String,
    val category: String,
    val level: String?, // e.g., "Specialist", "Power User"
    val skills: List<String>
)

@Serializable
data class WorkExperience(
    val id: String,
    val title: String,
    val company: String,
    val period: String,
    val description: String,
    val highlights: List<String>,
    val technologies: List<String>,
    val featured: Boolean = false
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val type: String, // "Open Source", "Personal", etc.
    val description: String,
    val technologies: List<String>,
    val links: ProjectLinks?,
    val featured: Boolean = false
)

@Serializable
data class ProjectLinks(
    val demo: String?,
    val source: String?,
    val playStore: String?
)

@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val organization: String?,
    val year: String,
    val description: String
)

@Serializable
data class Education(
    val degree: String,
    val field: String,
    val institution: String
)

@Serializable
data class CVReference(
    val id: String,
    val type: String, // "experience", "project", "skill", "achievement"
    val label: String // Human-readable label
)
```

### 3.2 Reference System

Each CV section will have a unique ID for referencing:
- `experience.mcdonalds` → McDonald's work experience
- `experience.geosatis` → GEOSATIS work experience
- `project.adidas-gmr` → Adidas GMR project
- `project.mtg-deckbuilder` → MTG DeckBuilder project
- `skills.kmp` → Kotlin Multiplatform skills
- `skills.ai-dev` → AI-Powered Development skills
- `achievement.android-school` → Android School Creator

### 3.3 Data Loading

- CV data stored as `shared/src/commonMain/resources/cv_data.json`
- Loaded at app startup using Kotlinx Serialization
- Parsed into strongly-typed data models
- Cached in memory for agent context

---

## 4. Groq API Integration & Agent Logic

### 4.1 API Client

```kotlin
package io.github.devmugi.cv.agent.api

class GroqApiClient(
    private val apiKey: String,
    private val httpClient: HttpClient = createHttpClient()
) {
    suspend fun chat(
        messages: List<ChatMessage>,
        model: String = "mixtral-8x7b-32768"
    ): Result<String>

    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1"
        private const val TIMEOUT_MS = 30_000L
    }
}

@Serializable
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)
```

### 4.2 Agent Prompt Strategy

**System Prompt Template:**
```
You are a professional career assistant representing Denys Honcharenko,
a Senior Android Developer and KMM Specialist with 15+ years of experience.

Your role is to:
- Answer questions about Denys's professional background in third person
- Use a professional but friendly tone
- Always include references to specific CV sections when relevant
- Format references as [Experience: Company Name], [Project: Project Name],
  [Skills: Category], or [Achievement: Title]
- Keep responses concise but informative (2-4 sentences for simple questions)
- Use markdown formatting for better readability (lists, bold, etc.)

Context - Denys's Complete CV:
{CV_DATA_JSON}

Guidelines:
- For experience questions: mention company, role, key achievements
- For skills questions: mention proficiency level and relevant projects
- For project questions: describe the project, technologies, and impact
- Always be factual and based only on the provided CV data
```

### 4.3 Chat State Management

```kotlin
package io.github.devmugi.cv.agent.agent

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val references: List<CVReference> = emptyList()
)

class ChatViewModel(
    private val groqClient: GroqApiClient,
    private val cvData: CVData
) : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun sendMessage(text: String)
    private fun extractReferences(response: String): List<CVReference>
    private fun buildSystemPrompt(): String
}
```

### 4.4 API Key Management

- **MVP:** API key stored in `gradle.properties` (excluded from git)
- **Build:** Injected via BuildConfig at compile time
- **Future Enhancement:** Settings screen for user-provided API key

---

## 5. UI Design & Components

### 5.1 Design System

**Theme (matching portfolio style):**
- **Background:** Dark navy/blue (`#1a1d2e`, `#16213e`)
- **Surface:** Slightly lighter blue (`#1e2746`)
- **Primary:** Gold/Yellow (`#f5a623`, `#ffc947`)
- **Text Primary:** White (`#ffffff`)
- **Text Secondary:** Light gray (`#b0b3c1`)
- **Border:** Gold with subtle glow (`#f5a623` with alpha)

**Typography:**
- Display: Large, bold for branding
- Title: Medium weight for headers
- Body: Regular weight for messages
- Code: Monospace for code blocks in markdown

**Spacing:**
- Small: 8dp
- Medium: 16dp
- Large: 24dp

### 5.2 Core Components

#### TopBar
```kotlin
@Composable
fun TopBar(modifier: Modifier = Modifier) {
    // Title: "<DH/> CV Agent"
    // Dark background with gold accent
    // Minimal, clean design
}
```

#### MessageBubble
```kotlin
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    Card(
        colors = if (message.isUser) goldAccent else darkSurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        // User: Simple text (right-aligned, gold background)
        // Agent: Markdown rendered text (left-aligned, dark surface)

        if (message.references.isNotEmpty()) {
            ReferenceChips(message.references)
        }
    }
}
```

#### MessageInput
```kotlin
@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    // TextField with rounded corners
    // Send button (icon) on the right
    // Gold accent when focused
    // Disabled state when loading
}
```

#### SuggestedQuestionChip
```kotlin
@Composable
fun SuggestedQuestionChip(
    question: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Outlined chip with gold border
    // Tappable, shows ripple effect
    // Disappears after first message
}
```

#### ReferenceChip
```kotlin
@Composable
fun ReferenceChip(
    reference: CVReference,
    modifier: Modifier = Modifier
) {
    // Small chip with subtle gold background
    // Shows CV reference type and label
    // MVP: Display only (no tap action)
    // Future: Tap to expand CV section
}
```

### 5.3 Main Screen Layout

```kotlin
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    Column(Modifier.fillMaxSize().background(darkBackground)) {
        TopBar()

        Box(Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                reverseLayout = true // Latest message at bottom
            ) {
                items(viewModel.state.messages.reversed()) { message ->
                    MessageBubble(
                        message = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .align(
                                if (message.isUser) Alignment.End
                                else Alignment.Start
                            )
                    )
                }

                // Show suggested questions when empty
                if (viewModel.state.messages.isEmpty()) {
                    item {
                        SuggestedQuestions(
                            questions = listOf(
                                "What's Denys's KMM experience?",
                                "Tell me about the McDonald's project",
                                "What are his AI development skills?",
                                "Show me his open source work"
                            ),
                            onQuestionClick = { viewModel.sendMessage(it) }
                        )
                    }
                }
            }

            // Loading indicator overlay
            if (viewModel.state.isLoading) {
                LoadingIndicator(Modifier.align(Alignment.Center))
            }
        }

        MessageInput(
            onSendMessage = { viewModel.sendMessage(it) },
            isEnabled = !viewModel.state.isLoading
        )
    }
}
```

### 5.4 Markdown Support

**Library:** `com.mikepenz:multiplatform-markdown-renderer` or `com.halilibo.compose-richtext`

**Supported Features:**
- **Bold**, *Italic*, `Code`
- Lists (ordered and unordered)
- Code blocks with syntax highlighting
- Links (though external navigation is secondary)
- Headers (H1-H6)

**Custom Styling:**
- Links: Gold color, no underline
- Code blocks: Darker background with gold border
- Lists: Gold bullet points

---

## 6. Quality Gates & Testing

### 6.1 Code Quality Tools

#### Detekt
- **Configuration:** `detekt.yml` with strict rules
- **Rules Enabled:**
  - Complexity: max cyclomatic complexity 15
  - Code smells: long methods, long parameter lists
  - Naming conventions
  - Potential bugs
- **Integration:** Gradle task + GitHub Actions
- **Failure Policy:** Build fails on violations

#### Ktlint
- **Style:** Standard Kotlin style guide
- **Features:**
  - Auto-formatting: `./gradlew ktlintFormat`
  - Verification: `./gradlew ktlintCheck`
- **Integration:** Pre-commit hook (optional) + CI
- **Scope:** All Kotlin code (shared, android, ios)

#### Kover
- **Coverage Target:** 80% for business logic
- **Excluded:** UI composables, generated code
- **Reports:** HTML + XML for CI integration
- **Task:** `./gradlew koverHtmlReport`

### 6.2 Testing Strategy

#### Unit Tests (80%+ coverage target)

**Location:** `shared/src/commonTest/`
**Framework:** Kotlin Test, MockK, Turbine (for Flow testing)

**Coverage Areas:**
1. **Data Models**
   - CV data parsing from JSON
   - Model validation
   - Reference ID generation

2. **API Client**
   - Groq API request/response handling
   - Error handling (network, timeout, API errors)
   - Retry logic
   - Mocked HTTP responses

3. **Agent Logic**
   - System prompt generation with CV context
   - Reference extraction from agent responses
   - Chat state management
   - Message ordering and timestamps

4. **Utilities**
   - Markdown parsing helpers
   - Date/time formatting
   - String utilities

**Example Test:**
```kotlin
class GroqApiClientTest {
    @Test
    fun `chat returns success with valid response`() = runTest {
        val mockClient = mockk<HttpClient> {
            // Mock response
        }
        val client = GroqApiClient("test-key", mockClient)

        val result = client.chat(listOf(
            ChatMessage("user", "Test question")
        ))

        assertTrue(result.isSuccess)
    }
}
```

#### UI Integration Tests

**Location:** `shared/src/commonTest/`
**Framework:** Compose Multiplatform Testing

**Coverage Areas:**
1. **Components**
   - MessageBubble renders correctly
   - SuggestedQuestionChip responds to clicks
   - MessageInput validation
   - ReferenceChip display

2. **Screen States**
   - Empty state shows suggestions
   - Loading state shows indicator
   - Error state shows message
   - Messages display in correct order

3. **Markdown Rendering**
   - Bold, italic, code formatting
   - Lists render correctly
   - Links are styled properly

**Example Test:**
```kotlin
@Test
fun chatScreen_showsSuggestedQuestions_whenEmpty() {
    composeTestRule.setContent {
        ChatScreen(viewModel = fakeChatViewModel())
    }

    composeTestRule
        .onNodeWithText("What's Denys's KMM experience?")
        .assertIsDisplayed()
}
```

#### E2E Tests

**Android:** `androidApp/src/androidTest/`
**Framework:** Compose UI Testing + Espresso

**iOS:** `iosApp/iosAppTests/`
**Framework:** XCTest + XCTestUI

**Test Scenarios:**
1. **Happy Path**
   - Launch app
   - Verify suggested questions appear
   - Tap a suggested question
   - Verify message appears in chat
   - Verify agent response (mocked API)
   - Verify references are shown

2. **User Input Flow**
   - Type custom question
   - Tap send button
   - Verify message sent
   - Verify response received

3. **Error Handling**
   - Trigger API error (mocked)
   - Verify error message displayed
   - Verify retry capability

4. **Multiple Messages**
   - Send multiple messages
   - Verify conversation history maintained
   - Verify scroll behavior

### 6.3 CI/CD Pipeline

**GitHub Actions Workflow:**

```yaml
name: Quality Gate

on: [push, pull_request]

jobs:
  quality-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Ktlint Check
        run: ./gradlew ktlintCheck

      - name: Detekt Analysis
        run: ./gradlew detekt

      - name: Unit Tests
        run: ./gradlew testDebugUnitTest

      - name: Code Coverage
        run: ./gradlew koverHtmlReport

      - name: Build
        run: ./gradlew build

      - name: Upload Coverage Report
        uses: codecov/codecov-action@v3
```

---

## 7. Implementation Phases

### Phase 1: Project Setup & Infrastructure

**Duration:** Foundation work
**Goal:** Buildable KMP project with quality gates

**Tasks:**
- Initialize KMP project with Compose Multiplatform template
- Configure Gradle build files (version catalogs, dependencies)
- Set up package structure (`io.github.devmugi.cv.agent.*`)
- Add dependencies:
  - Ktor Client
  - Kotlinx Serialization
  - Kotlinx Coroutines
  - Compose Multiplatform
  - Markdown renderer
- Configure Detekt (`detekt.yml`)
- Configure Ktlint
- Configure Kover
- Create GitHub Actions workflow
- Set up basic theme and design system (colors, typography, shapes)
- Create placeholder composables for Android and iOS

**Acceptance Criteria:**
- Project builds successfully on both Android and iOS
- All quality gates pass (Ktlint, Detekt, Kover)
- CI/CD pipeline runs successfully
- Basic app launches with empty screen

---

### Phase 2: Data Layer & CV Models

**Duration:** Data foundation
**Goal:** Complete CV data model with 80%+ test coverage

**Tasks:**
- Define all data models in `io.github.devmugi.cv.agent.data`:
  - `CVData`, `PersonalInfo`, `SkillCategory`
  - `WorkExperience`, `Project`, `Achievement`, `Education`
  - `CVReference`
- Create `cv_data.json` in `shared/src/commonMain/resources/`
- Structure complete CV data with unique IDs
- Implement `CVDataLoader` to parse JSON
- Add data validation logic
- Create `CVRepository` for data access
- Write unit tests:
  - JSON parsing tests
  - Model validation tests
  - Reference ID resolution tests
  - Edge cases (missing fields, invalid data)

**Acceptance Criteria:**
- All data models defined and serializable
- Complete CV data in structured JSON format
- CV data loads successfully from resources
- 80%+ test coverage for data layer
- All tests passing

---

### Phase 3: Groq API Integration & Agent Logic

**Duration:** Core business logic
**Goal:** Working Groq integration with full test coverage

**Tasks:**
- Implement Ktor HTTP client configuration
- Create `GroqApiClient` in `io.github.devmugi.cv.agent.api`:
  - Chat completion endpoint
  - Request/response serialization
  - Error handling (network, timeout, API errors)
  - Retry logic
- Design system prompt template
- Implement CV context injection into system prompt
- Create `ChatViewModel` in `io.github.devmugi.cv.agent.agent`:
  - State management (StateFlow)
  - Send message logic
  - Response parsing
  - Reference extraction from responses
- Implement reference extraction logic (parse `[Type: Label]` format)
- Add API key configuration (BuildConfig)
- Write comprehensive unit tests:
  - API client with mocked HTTP responses
  - Chat state management
  - System prompt generation
  - Reference extraction
  - Error scenarios

**Acceptance Criteria:**
- Groq API client successfully calls chat completion endpoint
- System prompt includes full CV context
- Agent responses parsed correctly
- CV references extracted from responses
- 80%+ test coverage for API and agent logic
- All tests passing with mocked API

---

### Phase 4: UI Implementation

**Duration:** User interface development
**Goal:** Fully functional chat UI on both platforms

**Tasks:**
- Integrate markdown rendering library
- Implement design system in `io.github.devmugi.cv.agent.ui.theme`:
  - Color scheme (dark theme with gold accents)
  - Typography scale
  - Shapes and spacing
- Build UI components in `io.github.devmugi.cv.agent.ui.components`:
  - `TopBar` with branding
  - `MessageBubble` with markdown support
  - `MessageInput` with validation and send button
  - `SuggestedQuestionChip` (tappable)
  - `ReferenceChip` (display only in MVP)
  - `LoadingIndicator`
- Implement `ChatScreen` in `io.github.devmugi.cv.agent.ui`:
  - LazyColumn for messages
  - Suggested questions when empty
  - Loading state overlay
  - Error handling UI
- Connect UI to `ChatViewModel`
- Add animations and transitions:
  - Message appearance animations
  - Loading indicator
  - Chip tap effects
- Implement platform-specific entry points (Android/iOS)
- Write UI integration tests:
  - Component rendering tests
  - Interaction tests (clicks, input)
  - State change tests
  - Markdown rendering verification

**Acceptance Criteria:**
- All UI components render correctly on Android and iOS
- Chat interface fully functional
- Markdown formatting works properly
- Suggested questions appear and work
- Loading and error states display correctly
- Smooth animations and transitions
- UI tests passing for all components

---

### Phase 5: Testing & Quality Assurance

**Duration:** Comprehensive validation
**Goal:** All tests passing, quality gates green

**Tasks:**
- Complete unit test coverage:
  - Fill gaps to reach 80%+ coverage
  - Add edge case tests
  - Test error scenarios
- Write comprehensive UI integration tests:
  - All components tested in isolation
  - State changes verified
  - User interactions validated
- Implement E2E tests:
  - Android: Full user journey with mocked API
  - iOS: Basic interaction flows
- Run full quality gate suite:
  - Ktlint check and fix violations
  - Detekt analysis and fix issues
  - Kover coverage report verification
- Manual testing on real devices:
  - Android (multiple device sizes)
  - iOS (iPhone and iPad)
- Performance profiling:
  - API response times
  - UI rendering performance
  - Memory usage
  - Markdown rendering speed
- Bug fixing and refinement

**Acceptance Criteria:**
- 80%+ code coverage for business logic
- All unit tests passing
- All UI integration tests passing
- E2E tests passing on both platforms
- All quality gates green (Ktlint, Detekt, Kover)
- No critical bugs on real devices
- Smooth performance on both platforms

---

### Phase 6: Polish & Deployment Prep

**Duration:** Final touches
**Goal:** Production-ready portfolio app

**Tasks:**
- Refine UI animations and transitions
- Optimize markdown rendering performance
- Add app icons (Android and iOS)
- Create splash screens
- Configure proper API key management:
  - Document setup process
  - Add validation for missing key
- Polish error messages and empty states
- Improve loading states (skeleton screens)
- Add accessibility support:
  - Content descriptions
  - Screen reader support
- Write comprehensive README:
  - Project description
  - Setup instructions
  - Build and run instructions
  - Testing instructions
  - Architecture overview
- Create demo materials:
  - Screenshots for portfolio
  - Screen recording of key features
  - Architecture diagram
- Prepare App Store/Play Store metadata (optional):
  - App description
  - Screenshots
  - Privacy policy

**Deliverable:** Production-ready portfolio app ready for distribution

**Acceptance Criteria:**
- All polish items completed
- App icons and splash screens implemented
- README documentation complete
- Demo video and screenshots ready
- App ready for Play Store/App Store submission (if desired)
- Portfolio-ready showcase piece

---

## 8. Non-Functional Requirements

### 8.1 Performance

- **API Response Time:** < 5 seconds for typical queries
- **UI Responsiveness:** 60fps animations, no jank
- **App Launch:** < 2 seconds to usable chat screen
- **Memory Usage:** < 100MB for typical session

### 8.2 Compatibility

- **Android:** API 24+ (Android 7.0+)
- **iOS:** iOS 14+
- **Device Support:** Phones and tablets

### 8.3 Security

- **API Key:** Not hardcoded, loaded from build config
- **HTTPS:** All API calls over secure connection
- **No Data Storage:** No persistent storage of conversations (MVP)

### 8.4 Accessibility

- Content descriptions for all interactive elements
- Screen reader support
- Sufficient color contrast (WCAG AA)
- Scalable text

---

## 9. Future Enhancements (Post-MVP)

### 9.1 Features
- Conversation history persistence
- Share conversation feature
- Deep links to CV sections from reference chips
- Settings screen (API key input, theme selection)
- Suggested follow-up questions based on context
- Voice input support
- Streaming responses (word-by-word)

### 9.2 Technical
- Backend service for API key management
- Analytics integration
- Crash reporting
- A/B testing framework
- Localization support

### 9.3 Portfolio Enhancements
- Interactive CV viewer tab
- Timeline visualization of career
- Skills graph/chart
- Project showcase with images

---

## 10. Success Metrics

### 10.1 Technical Metrics
- Code coverage: 80%+
- All quality gates passing
- Zero critical bugs
- 60fps UI performance

### 10.2 Portfolio Metrics
- Demonstrates advanced KMP skills
- Shows AI integration capability
- Production-quality code
- Comprehensive testing
- Clean architecture

### 10.3 User Experience Metrics
- Clear and professional responses
- Accurate CV information
- Smooth and intuitive UI
- Fast response times

---

## 11. Dependencies & Prerequisites

### 11.1 Required Tools
- **JDK:** 17+
- **Android Studio:** Latest stable with KMP plugin
- **Xcode:** Latest stable (for iOS)
- **Groq API Key:** Free tier available at groq.com

### 11.2 Knowledge Prerequisites
- Kotlin Multiplatform
- Compose Multiplatform
- Ktor Client
- Coroutines and Flow
- REST API integration
- Mobile app architecture (MVVM)

---

## 12. Risks & Mitigations

### 12.1 Technical Risks

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Groq API rate limits | High | Implement request throttling, error handling |
| Compose Multiplatform iOS bugs | Medium | Thorough testing, fallback to platform-specific UI if needed |
| Markdown rendering issues | Low | Test extensively, choose stable library |
| Large app size from markdown lib | Low | Use ProGuard/R8, optimize resources |

### 12.2 Project Risks

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Scope creep | High | Stick to MVP features, defer enhancements |
| Over-engineering | Medium | Follow YAGNI principle, simple solutions |
| Testing overhead | Low | Focus on critical paths, automate tests |

---

## Appendix A: Suggested Questions

Preset questions to guide users:

1. "What's Denys's experience with Kotlin Multiplatform?"
2. "Tell me about the McDonald's project"
3. "What are his AI development skills and tools?"
4. "Show me his open source contributions"
5. "What IoT projects has he worked on?"
6. "Describe his experience with Adidas GMR"
7. "What's his educational background?"
8. "What achievements and certifications does he have?"

---

## Appendix B: Reference Format

Agent responses will include references in this format:

```
[Experience: McDonald's]
[Project: MTG DeckBuilder]
[Skills: Kotlin Multiplatform]
[Achievement: Android School Creator]
```

These will be parsed and displayed as chips below agent messages.

---

## Document Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-01-20 | Initial PRD | Denys Honcharenko |

---

**End of Document**
