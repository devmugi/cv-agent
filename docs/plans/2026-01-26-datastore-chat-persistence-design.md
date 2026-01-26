# Chat Message Persistence with KMP DataStore

## Problem

Currently, chat messages are persisted using `SavedStateHandle`, which survives process death but NOT app restarts. Users lose their conversation when swiping away the app.

## Solution

Use KMP DataStore (Preferences) to persist chat messages and session metadata across app restarts until explicitly cleared.

## What Gets Persisted

- `chat_messages` - JSON string of `List<Message>`
- `session_id` - String (conversation identifier)
- `turn_number` - Int (message count in session)

## Architecture

```
shared-domain/
  └── repository/
      └── ChatRepository.kt              # Interface

shared/
  ├── commonMain/
  │   └── repository/
  │       ├── DataStoreFactory.kt        # Common factory
  │       └── DataStoreChatRepository.kt # Implementation
  ├── androidMain/
  │   └── repository/
  │       └── DataStoreFactory.android.kt
  └── iosMain/
      └── repository/
          └── DataStoreFactory.ios.kt

shared-agent/
  └── agent/
      └── ChatViewModel.kt               # Uses ChatRepository
```

**Data flow:**
```
ChatViewModel → ChatRepository (interface) → DataStore<Preferences>
                                                    ↓
                                            JSON-serialized storage
```

## Repository Interface

```kotlin
// shared-domain/src/commonMain/kotlin/.../repository/ChatRepository.kt

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

## Platform-Specific DataStore Creation

**Common factory (shared/commonMain):**

```kotlin
fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

internal const val DATA_STORE_FILE_NAME = "chat_preferences.preferences_pb"
```

**Android (shared/androidMain):**

```kotlin
fun createDataStore(context: Context): DataStore<Preferences> = createDataStore(
    producePath = { context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath }
)
```

**iOS (shared/iosMain):**

```kotlin
fun createDataStore(): DataStore<Preferences> = createDataStore(
    producePath = {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        requireNotNull(documentDirectory).path + "/$DATA_STORE_FILE_NAME"
    }
)
```

## ChatViewModel Changes

**Constructor change:**

```kotlin
class ChatViewModel(
    private val apiClient: GroqApiClient,
    private val promptBuilder: SystemPromptBuilder,
    private val suggestionExtractor: SuggestionExtractor,
    private val dataProvider: AgentDataProvider?,
    private val chatRepository: ChatRepository,  // Replaces SavedStateHandle for messages
    private val analytics: Analytics = Analytics.NOOP,
    private val tracer: ArizeTracer = ArizeTracer.NOOP,
) : ViewModel()
```

**Initialization:**

```kotlin
init {
    viewModelScope.launch {
        val messages = chatRepository.getMessages()
        val sessionId = chatRepository.getSessionId() ?: Uuid.random().toString()
        val turnNumber = chatRepository.getTurnNumber()

        _state.update { it.copy(messages = messages) }
        this@ChatViewModel.sessionId = sessionId
        this@ChatViewModel.turnNumber = turnNumber
    }
}
```

**Save after changes:**

```kotlin
viewModelScope.launch {
    chatRepository.saveMessages(newMessages)
}
```

## Dependencies

**libs.versions.toml:**

```toml
[versions]
datastore = "1.2.0"

[libraries]
datastore = { module = "androidx.datastore:datastore", version.ref = "datastore" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

**shared/build.gradle.kts:**

```kotlin
commonMain.dependencies {
    implementation(libs.datastore)
    implementation(libs.datastore.preferences)
}
```

## Testing

**FakeChatRepository for ViewModel tests:**

```kotlin
class FakeChatRepository : ChatRepository {
    private var messages = emptyList<Message>()
    private var sessionId: String? = null
    private var turnNumber = 0

    override suspend fun getMessages() = messages
    override suspend fun saveMessages(messages: List<Message>) { this.messages = messages }
    override suspend fun clearMessages() { messages = emptyList() }
    override suspend fun getSessionId() = sessionId
    override suspend fun saveSessionId(sessionId: String) { this.sessionId = sessionId }
    override suspend fun getTurnNumber() = turnNumber
    override suspend fun saveTurnNumber(turnNumber: Int) { this.turnNumber = turnNumber }
    override suspend fun clearAll() {
        messages = emptyList()
        sessionId = null
        turnNumber = 0
    }
}
```

**Unit tests for DataStoreChatRepository:** Test save/load roundtrips and clearAll behavior.

## References

- [KMP DataStore documentation](https://developer.android.com/kotlin/multiplatform/datastore)
