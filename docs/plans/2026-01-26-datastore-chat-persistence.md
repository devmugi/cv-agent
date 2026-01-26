# DataStore Chat Persistence Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Persist chat messages and session metadata across app restarts using KMP DataStore.

**Architecture:** Repository pattern with interface in shared-domain, DataStore implementation in shared module. Platform-specific file path resolution via expect/actual.

**Tech Stack:** KMP DataStore 1.2.0, Preferences DataStore, Koin DI, kotlinx.serialization

---

## Task 1: Add DataStore Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`

**Step 1: Add version and libraries to version catalog**

In `gradle/libs.versions.toml`, add after line 58 (after turbine):

```toml
# DataStore
datastore = "1.2.0"
```

In the `[libraries]` section, add after line 133 (after robolectric):

```toml
# DataStore
datastore = { module = "androidx.datastore:datastore", version.ref = "datastore" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

**Step 2: Add dependencies to shared module**

In `shared/build.gradle.kts`, add to `commonMain.dependencies` block (after line 68, before closing brace):

```kotlin
            // DataStore
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)
```

**Step 3: Verify build compiles**

Run: `./gradlew :shared:compileAndroidMain`

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts
git commit -m "build: add DataStore 1.2.0 dependencies"
```

---

## Task 2: Create ChatRepository Interface

**Files:**
- Create: `shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/repository/ChatRepository.kt`

**Step 1: Create the interface**

```kotlin
package io.github.devmugi.cv.agent.domain.repository

import io.github.devmugi.cv.agent.domain.models.Message

interface ChatRepository {
    suspend fun getMessages(): List<Message>
    suspend fun saveMessages(messages: List<Message>)
    suspend fun clearMessages()

    suspend fun getSessionId(): String?
    suspend fun saveSessionId(sessionId: String)

    suspend fun getTurnNumber(): Int
    suspend fun saveTurnNumber(turnNumber: Int)

    suspend fun clearAll()
}
```

**Step 2: Verify build compiles**

Run: `./gradlew :shared-domain:compileAndroidMain`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-domain/src/commonMain/kotlin/io/github/devmugi/cv/agent/domain/repository/ChatRepository.kt
git commit -m "feat(domain): add ChatRepository interface"
```

---

## Task 3: Create FakeChatRepository for Testing

**Files:**
- Create: `shared-agent/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/FakeChatRepository.kt`

**Step 1: Create the fake implementation**

```kotlin
package io.github.devmugi.cv.agent.agent

import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.repository.ChatRepository

class FakeChatRepository : ChatRepository {
    private var messages = emptyList<Message>()
    private var sessionId: String? = null
    private var turnNumber = 0

    // For test assertions
    var saveMessagesCalled = false
    var clearAllCalled = false

    override suspend fun getMessages(): List<Message> = messages

    override suspend fun saveMessages(messages: List<Message>) {
        this.messages = messages
        saveMessagesCalled = true
    }

    override suspend fun clearMessages() {
        messages = emptyList()
    }

    override suspend fun getSessionId(): String? = sessionId

    override suspend fun saveSessionId(sessionId: String) {
        this.sessionId = sessionId
    }

    override suspend fun getTurnNumber(): Int = turnNumber

    override suspend fun saveTurnNumber(turnNumber: Int) {
        this.turnNumber = turnNumber
    }

    override suspend fun clearAll() {
        messages = emptyList()
        sessionId = null
        turnNumber = 0
        clearAllCalled = true
    }

    // Test helpers
    fun preloadMessages(messages: List<Message>) {
        this.messages = messages
    }

    fun preloadSession(sessionId: String, turnNumber: Int) {
        this.sessionId = sessionId
        this.turnNumber = turnNumber
    }

    fun reset() {
        messages = emptyList()
        sessionId = null
        turnNumber = 0
        saveMessagesCalled = false
        clearAllCalled = false
    }
}
```

**Step 2: Verify test compiles**

Run: `./gradlew :shared-agent:compileTestKotlinAndroid`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared-agent/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/FakeChatRepository.kt
git commit -m "test(agent): add FakeChatRepository for testing"
```

---

## Task 4: Add ChatRepository to ChatViewModel Constructor

**Files:**
- Modify: `shared-agent/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt`
- Modify: `shared-agent/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Add chatRepository parameter to ChatViewModel**

In `ChatViewModel.kt`, update the constructor (lines 27-35) to add `chatRepository` parameter:

```kotlin
class ChatViewModel(
    private val savedStateHandle: SavedStateHandle? = null,
    private val apiClient: GroqApiClient,
    private val promptBuilder: SystemPromptBuilder,
    private val suggestionExtractor: SuggestionExtractor,
    private val dataProvider: AgentDataProvider?,
    private val chatRepository: ChatRepository? = null,
    private val analytics: Analytics = Analytics.NOOP,
    private val tracer: ArizeTracer = ArizeTracer.NOOP,
) : ViewModel() {
```

Add the import at the top of the file:

```kotlin
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
```

**Step 2: Update test setup to pass FakeChatRepository**

In `ChatViewModelTest.kt`, add to the class fields (after line 64):

```kotlin
    private lateinit var fakeChatRepository: FakeChatRepository
```

Update the `setup()` function (around line 73-82):

```kotlin
    @BeforeTest
    fun setup() {
        // Disable logging in tests to avoid android.util.Log dependency
        Logger.setMinSeverity(Severity.Assert)

        Dispatchers.setMain(testDispatcher)
        fakeApiClient = FakeGroqApiClient()
        fakeAnalytics = FakeAnalytics()
        fakeChatRepository = FakeChatRepository()
        viewModel = ChatViewModel(
            apiClient = fakeApiClient,
            promptBuilder = SystemPromptBuilder(),
            suggestionExtractor = SuggestionExtractor(),
            dataProvider = null,
            chatRepository = fakeChatRepository,
            analytics = fakeAnalytics
        )
    }
```

Also add the import at the top:

```kotlin
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
```

Update the `systemPromptIncludedWhenDataProviderAvailable` test (around line 252) to include chatRepository:

```kotlin
    @Test
    fun systemPromptIncludedWhenDataProviderAvailable() = runTest {
        val viewModelWithData = ChatViewModel(
            apiClient = fakeApiClient,
            promptBuilder = SystemPromptBuilder(),
            suggestionExtractor = SuggestionExtractor(),
            dataProvider = testDataProvider,
            chatRepository = FakeChatRepository()
        )

        viewModelWithData.sendMessage("Hi")
        advanceUntilIdle()

        val systemMessage = fakeApiClient.capturedMessages.find { it.role == "system" }
        assertNotNull(systemMessage)
        assertTrue(systemMessage.content.contains("Test Name"))
    }
```

**Step 3: Run tests to verify nothing breaks**

Run: `./gradlew :shared-agent:testAndroidUnitTest`

Expected: All tests PASS

**Step 4: Commit**

```bash
git add shared-agent/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt
git add shared-agent/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt
git commit -m "feat(agent): add ChatRepository parameter to ChatViewModel"
```

---

## Task 5: Write Tests for Repository-Based Message Persistence

**Files:**
- Modify: `shared-agent/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt`

**Step 1: Write the failing tests**

Add these tests to `ChatViewModelTest.kt` after the existing tests (before the `FakeGroqApiClient` class):

```kotlin
    // ============ Repository Persistence Tests ============

    @Test
    fun sendMessageSavesMessagesToRepository() = runTest {
        fakeApiClient.responseChunks = listOf("Response")
        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        assertTrue(fakeChatRepository.saveMessagesCalled)
        val savedMessages = fakeChatRepository.getMessages()
        assertEquals(2, savedMessages.size) // user + assistant
    }

    @Test
    fun clearHistoryClearsRepository() = runTest {
        fakeApiClient.responseChunks = listOf("Response")
        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        viewModel.clearHistory()
        advanceUntilIdle()

        assertTrue(fakeChatRepository.clearAllCalled)
    }

    @Test
    fun viewModelRestoresMessagesFromRepository() = runTest {
        val preloadedMessages = listOf(
            Message(role = MessageRole.USER, content = "Restored message"),
            Message(role = MessageRole.ASSISTANT, content = "Restored response")
        )
        fakeChatRepository.preloadMessages(preloadedMessages)

        // Create new ViewModel that should load from repository
        val restoredViewModel = ChatViewModel(
            apiClient = fakeApiClient,
            promptBuilder = SystemPromptBuilder(),
            suggestionExtractor = SuggestionExtractor(),
            dataProvider = null,
            chatRepository = fakeChatRepository,
            analytics = fakeAnalytics
        )
        advanceUntilIdle()

        assertEquals(2, restoredViewModel.state.value.messages.size)
        assertEquals("Restored message", restoredViewModel.state.value.messages[0].content)
    }

    @Test
    fun viewModelRestoresSessionFromRepository() = runTest {
        fakeChatRepository.preloadSession(sessionId = "test-session-123", turnNumber = 5)

        val restoredViewModel = ChatViewModel(
            apiClient = fakeApiClient,
            promptBuilder = SystemPromptBuilder(),
            suggestionExtractor = SuggestionExtractor(),
            dataProvider = null,
            chatRepository = fakeChatRepository,
            analytics = fakeAnalytics
        )
        advanceUntilIdle()

        // Send a message and verify turnNumber continues from restored value
        fakeApiClient.responseChunks = listOf("Response")
        restoredViewModel.sendMessage("New message")
        advanceUntilIdle()

        val event = fakeAnalytics.findEvent<AnalyticsEvent.Chat.MessageSent>()
        assertNotNull(event)
        assertEquals(6, event.turnNumber) // Should be 5 + 1
    }
```

Add the import for Message:

```kotlin
import io.github.devmugi.cv.agent.domain.models.Message
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :shared-agent:testAndroidUnitTest --tests "*Repository*"`

Expected: FAIL - repository methods not called yet (tests fail on assertions)

**Step 3: Commit the failing tests**

```bash
git add shared-agent/src/commonTest/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModelTest.kt
git commit -m "test(agent): add failing tests for repository-based persistence"
```

---

## Task 6: Implement Repository Integration in ChatViewModel

**Files:**
- Modify: `shared-agent/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt`

**Step 1: Update restoreMessages() to use repository**

Replace the `restoreMessages()` function (lines 94-102) with this version that tries repository first:

```kotlin
    private fun restoreMessages(): List<Message> {
        // Repository-based restore happens in init block via coroutine
        // This synchronous version is fallback for SavedStateHandle only
        val messagesJson = savedStateHandle?.get<String>(KEY_MESSAGES) ?: return emptyList()
        return try {
            json.decodeFromString<List<Message>>(messagesJson)
        } catch (e: Exception) {
            Logger.w(TAG) { "Failed to restore messages from SavedStateHandle: ${e.message}" }
            emptyList()
        }
    }
```

**Step 2: Update init block to load from repository**

Replace the init block (lines 63-92) with:

```kotlin
    init {
        // Load from repository if available (async)
        viewModelScope.launch {
            chatRepository?.let { repo ->
                val repoMessages = repo.getMessages()
                val repoSessionId = repo.getSessionId()
                val repoTurnNumber = repo.getTurnNumber()

                if (repoMessages.isNotEmpty()) {
                    _state.update { it.copy(messages = repoMessages) }
                }
                repoSessionId?.let { sessionId = it }
                if (repoTurnNumber > 0) {
                    turnNumber = repoTurnNumber
                }
            }

            // Track session start/resume
            val currentMessages = _state.value.messages
            val isFirstEverOpen = savedStateHandle?.get<Boolean>(KEY_HAS_EVER_OPENED) != true

            // Mark as opened (persists even after clearHistory)
            savedStateHandle?.set(KEY_HAS_EVER_OPENED, true)

            if (currentMessages.isNotEmpty()) {
                analytics.logEvent(
                    AnalyticsEvent.Session.SessionResume(
                        sessionId = sessionId,
                        messageCount = currentMessages.size
                    )
                )
            } else {
                analytics.logEvent(
                    AnalyticsEvent.Session.SessionStart(
                        sessionId = sessionId,
                        isNewInstall = isFirstEverOpen
                    )
                )
            }

            // Save session info
            savedStateHandle?.let {
                it[KEY_SESSION_ID] = sessionId
                it[KEY_TURN_NUMBER] = turnNumber
            }
        }
    }
```

**Step 3: Update saveMessages() to also save to repository**

Replace the `saveMessages()` function (lines 104-111) with:

```kotlin
    private fun saveMessages(messages: List<Message>) {
        // Save to SavedStateHandle (process death)
        savedStateHandle?.let {
            try {
                it[KEY_MESSAGES] = json.encodeToString(messages)
            } catch (e: Exception) {
                Logger.w(TAG) { "Failed to save messages to SavedStateHandle: ${e.message}" }
            }
        }
        // Save to repository (app restart)
        viewModelScope.launch {
            chatRepository?.saveMessages(messages)
        }
    }
```

**Step 4: Update saveSessionState() to also save to repository**

Replace the `saveSessionState()` function (lines 113-118) with:

```kotlin
    private fun saveSessionState() {
        savedStateHandle?.let {
            it[KEY_SESSION_ID] = sessionId
            it[KEY_TURN_NUMBER] = turnNumber
        }
        viewModelScope.launch {
            chatRepository?.saveSessionId(sessionId)
            chatRepository?.saveTurnNumber(turnNumber)
        }
    }
```

**Step 5: Update clearHistory() to clear repository**

In the `clearHistory()` function, after line 182 (`savedStateHandle?.remove<String>(KEY_MESSAGES)`), add:

```kotlin
        viewModelScope.launch {
            chatRepository?.clearAll()
        }
```

**Step 6: Run tests to verify they pass**

Run: `./gradlew :shared-agent:testAndroidUnitTest`

Expected: All tests PASS

**Step 7: Commit**

```bash
git add shared-agent/src/commonMain/kotlin/io/github/devmugi/cv/agent/agent/ChatViewModel.kt
git commit -m "feat(agent): integrate ChatRepository for message persistence"
```

---

## Task 7: Create DataStore Factory (Common)

**Files:**
- Create: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/repository/DataStoreFactory.kt`

**Step 1: Create the common factory**

```kotlin
package io.github.devmugi.cv.agent.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val DATA_STORE_FILE_NAME = "chat_preferences.preferences_pb"

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )
```

**Step 2: Verify build compiles**

Run: `./gradlew :shared:compileAndroidMain`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/repository/DataStoreFactory.kt
git commit -m "feat(shared): add common DataStore factory"
```

---

## Task 8: Create Platform-Specific DataStore Factory (Android)

**Files:**
- Create: `shared/src/androidMain/kotlin/io/github/devmugi/cv/agent/repository/DataStoreFactory.android.kt`

**Step 1: Create the Android factory**

```kotlin
package io.github.devmugi.cv.agent.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun createDataStore(context: Context): DataStore<Preferences> = createDataStore(
    producePath = { context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath }
)
```

**Step 2: Verify build compiles**

Run: `./gradlew :shared:compileAndroidMain`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/androidMain/kotlin/io/github/devmugi/cv/agent/repository/DataStoreFactory.android.kt
git commit -m "feat(shared): add Android DataStore factory"
```

---

## Task 9: Create DataStoreChatRepository Implementation

**Files:**
- Create: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/repository/DataStoreChatRepository.kt`

**Step 1: Create the implementation**

```kotlin
package io.github.devmugi.cv.agent.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.devmugi.cv.agent.domain.models.Message
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreChatRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ChatRepository {

    companion object {
        private val KEY_MESSAGES = stringPreferencesKey("chat_messages")
        private val KEY_SESSION_ID = stringPreferencesKey("session_id")
        private val KEY_TURN_NUMBER = intPreferencesKey("turn_number")
    }

    override suspend fun getMessages(): List<Message> {
        val prefs = dataStore.data.first()
        val messagesJson = prefs[KEY_MESSAGES] ?: return emptyList()
        return try {
            json.decodeFromString<List<Message>>(messagesJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveMessages(messages: List<Message>) {
        dataStore.edit { prefs ->
            prefs[KEY_MESSAGES] = json.encodeToString(messages)
        }
    }

    override suspend fun clearMessages() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_MESSAGES)
        }
    }

    override suspend fun getSessionId(): String? {
        val prefs = dataStore.data.first()
        return prefs[KEY_SESSION_ID]
    }

    override suspend fun saveSessionId(sessionId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = sessionId
        }
    }

    override suspend fun getTurnNumber(): Int {
        val prefs = dataStore.data.first()
        return prefs[KEY_TURN_NUMBER] ?: 0
    }

    override suspend fun saveTurnNumber(turnNumber: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_TURN_NUMBER] = turnNumber
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_MESSAGES)
            prefs.remove(KEY_SESSION_ID)
            prefs.remove(KEY_TURN_NUMBER)
        }
    }
}
```

**Step 2: Verify build compiles**

Run: `./gradlew :shared:compileAndroidMain`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/repository/DataStoreChatRepository.kt
git commit -m "feat(shared): add DataStoreChatRepository implementation"
```

---

## Task 10: Wire DataStore in Koin AppModule

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/AppModule.kt`

**Step 1: Add repository imports and singleton**

Add imports at the top:

```kotlin
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
import io.github.devmugi.cv.agent.repository.DataStoreChatRepository
```

Add to the `appModule` block, before the closing brace (after line 38):

```kotlin
    // Repository Layer
    single<ChatRepository> { DataStoreChatRepository(get()) }
```

**Step 2: Verify build compiles**

Run: `./gradlew :shared:compileAndroidMain`

Expected: BUILD SUCCESSFUL (will fail at runtime until DataStore is provided)

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/io/github/devmugi/cv/agent/di/AppModule.kt
git commit -m "feat(di): wire ChatRepository in AppModule"
```

---

## Task 11: Wire DataStore Instance in Android ViewModelModule

**Files:**
- Modify: `shared/src/androidMain/kotlin/io/github/devmugi/cv/agent/di/ViewModelModule.kt`

**Step 1: Add DataStore singleton and update ViewModel**

Add imports at the top:

```kotlin
import io.github.devmugi.cv.agent.domain.repository.ChatRepository
import io.github.devmugi.cv.agent.repository.createDataStore
```

Update the module to provide DataStore and inject ChatRepository:

```kotlin
val viewModelModule = module {
    // DataStore (singleton per app)
    single { createDataStore(androidContext()) }

    viewModel { params ->
        val savedStateHandle: SavedStateHandle = params.get()
        val dataProvider: AgentDataProvider? = params.getOrNull()
        ChatViewModel(
            savedStateHandle = savedStateHandle,
            apiClient = get(),
            promptBuilder = get(),
            suggestionExtractor = get(),
            dataProvider = dataProvider,
            chatRepository = get<ChatRepository>(),
            analytics = getOrNull<Analytics>() ?: Analytics.NOOP,
            tracer = getOrNull<ArizeTracer>() ?: ArizeTracer.NOOP
        )
    }
}
```

**Step 2: Verify build compiles**

Run: `./gradlew :shared:compileAndroidMain`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/androidMain/kotlin/io/github/devmugi/cv/agent/di/ViewModelModule.kt
git commit -m "feat(di): wire DataStore in Android ViewModelModule"
```

---

## Task 12: Run Full Test Suite and Build

**Files:** None (verification only)

**Step 1: Run all agent tests**

Run: `./gradlew :shared-agent:testAndroidUnitTest`

Expected: All tests PASS

**Step 2: Run full quality check**

Run: `./gradlew qualityCheck`

Expected: BUILD SUCCESSFUL (no lint/detekt errors)

**Step 3: Build Android app**

Run: `./gradlew :android-app:assembleDebug`

Expected: BUILD SUCCESSFUL

**Step 4: Final commit (if any fixes needed)**

If quality check required fixes:
```bash
git add -A
git commit -m "fix: address lint/quality issues"
```

---

## Task 13: Manual Testing Checklist

**Test on device/emulator:**

1. [ ] Open app, send a message, get response
2. [ ] Force stop app (swipe away from recents)
3. [ ] Reopen app - messages should be restored
4. [ ] Clear history - messages should disappear
5. [ ] Force stop and reopen - should show empty chat
6. [ ] Send multiple messages, force stop, reopen - all messages restored

---

## Summary

| Task | Description | Files |
|------|-------------|-------|
| 1 | Add DataStore dependencies | `libs.versions.toml`, `shared/build.gradle.kts` |
| 2 | Create ChatRepository interface | `shared-domain/.../ChatRepository.kt` |
| 3 | Create FakeChatRepository | `shared-agent/test/.../FakeChatRepository.kt` |
| 4 | Add ChatRepository to ViewModel | `ChatViewModel.kt`, `ChatViewModelTest.kt` |
| 5 | Write failing persistence tests | `ChatViewModelTest.kt` |
| 6 | Implement repository integration | `ChatViewModel.kt` |
| 7 | Create common DataStore factory | `shared/.../DataStoreFactory.kt` |
| 8 | Create Android DataStore factory | `shared/androidMain/.../DataStoreFactory.android.kt` |
| 9 | Create DataStoreChatRepository | `shared/.../DataStoreChatRepository.kt` |
| 10 | Wire repository in AppModule | `AppModule.kt` |
| 11 | Wire DataStore in ViewModelModule | `ViewModelModule.kt` |
| 12 | Run full test suite | (verification) |
| 13 | Manual testing | (verification) |
