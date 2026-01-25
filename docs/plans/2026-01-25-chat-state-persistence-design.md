# Chat State Persistence Design

## Problem

Chat messages were lost on orientation changes and process death because:
- `ChatViewModel` used Koin's `factory` scope (new instance on every injection)
- No state serialization or persistence mechanism

## Solution

Use `SavedStateHandle` with Koin's `viewModel` scope for automatic state persistence across:
- Configuration changes (rotation)
- Process death (app killed in background)

## Changes Made

### Dependencies
- Added `koin-androidx-compose` for `koinViewModel()` with SavedStateHandle support
- Added `robolectric` version to toml (not currently used but available)

### Model Serialization
Added `@Serializable` annotations to:
- `Message` and `MessageRole` in `shared-domain`
- `ChatState` and `ChatError` in `shared-domain`

### ViewModel Changes (`ChatViewModel.kt`)
- Added optional `SavedStateHandle` parameter (nullable for test compatibility)
- Messages serialized to JSON and stored in SavedStateHandle
- Session ID and turn number also persisted for conversation continuity
- Null-safe operations throughout for testability

### DI Changes
- Moved ViewModel registration to Android-specific `ViewModelModule.kt`
- Uses `viewModel { }` DSL which provides SavedStateHandle automatically
- CommonMain `AppModule.kt` no longer registers the ViewModel

### Activity Changes (`MainActivity.kt`)
- Changed from `koinInject` to `koinViewModel` for proper lifecycle scoping

### Application Changes (`CVAgentApplication.kt`)
- Added `viewModelModule` to Koin modules list

## What Gets Persisted

| Data | Persisted | Reason |
|------|-----------|--------|
| Messages list | Yes | Core user data |
| Session ID | Yes | Conversation context |
| Turn number | Yes | Conversation context |
| isLoading/isStreaming | No | Transient UI state |
| streamingMessageId | No | Transient UI state |
| error | No | Should reset on restore |

## Testing

Tests pass `null` for SavedStateHandle, which is acceptable because:
- State persistence is a platform concern, not business logic
- Core ViewModel functionality (message sending, streaming, error handling) is still tested
- Production code provides SavedStateHandle through Koin

## Trade-offs

1. **Slight serialization overhead** on each message save - acceptable since messages are infrequent
2. **SavedStateHandle nullable** - makes ViewModel testable without Android dependencies
3. **Android-specific module** - required because `viewModel` DSL needs Android lifecycle classes
