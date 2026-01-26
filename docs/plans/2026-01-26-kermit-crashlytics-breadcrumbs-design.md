# Kermit Crashlytics Breadcrumbs Design

## Overview

Integrate Kermit logging with Firebase Crashlytics to provide breadcrumb logs when crashes occur. When a crash happens, recent Kermit logs will appear in the Crashlytics timeline, helping debug what led to the crash.

## Requirements

- Forward Kermit logs to Crashlytics as breadcrumbs
- Configurable severity filtering based on build type
- Pattern redaction for sensitive data in release builds

### Configuration by Build Type

| Build   | Severity Threshold | Redaction           |
|---------|-------------------|---------------------|
| Debug   | All (Verbose+)    | Disabled            |
| Release | Info+             | Enabled             |

### Patterns to Redact (Release Only)

- **Email addresses**: `user@example.com` → `[EMAIL]`
- **API keys/tokens**: Long alphanumeric strings, Bearer tokens → `[TOKEN]`

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Code                        │
│         Logger.d(TAG) { "Starting request..." }            │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    Kermit Logger                            │
│              (routes to all LogWriters)                     │
└──────────┬──────────────────────────────────────┬───────────┘
           │                                      │
           ▼                                      ▼
┌──────────────────────┐            ┌─────────────────────────┐
│  Default LogWriter   │            │  CrashlyticsLogWriter   │
│  (Logcat/Console)    │            │  (shared-crashlytics)   │
└──────────────────────┘            └──────────────┬──────────┘
                                                   │
                                                   ▼
                                    ┌─────────────────────────┐
                                    │  LogRedactor            │
                                    │  - Email pattern        │
                                    │  - Token pattern        │
                                    └──────────────┬──────────┘
                                                   │
                                                   ▼
                                    ┌─────────────────────────┐
                                    │  CrashReporter.log()    │
                                    │  (existing interface)   │
                                    └─────────────────────────┘
```

**Module location:** `shared-crashlytics` (keeps crash-related code together)

## Component Details

### LogRedactor

Pattern-based message sanitization:

```kotlin
// shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/LogRedactor.kt

object LogRedactor {
    private val EMAIL_PATTERN = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val TOKEN_PATTERN = Regex("\\b(Bearer\\s+)?[A-Za-z0-9_-]{20,}\\b")

    fun redact(message: String): String {
        return message
            .replace(EMAIL_PATTERN, "[EMAIL]")
            .replace(TOKEN_PATTERN, "[TOKEN]")
    }
}
```

### CrashlyticsLogWriter

Kermit LogWriter that forwards to CrashReporter:

```kotlin
// shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriter.kt

class CrashlyticsLogWriter(
    private val crashReporter: CrashReporter,
    private val minSeverity: Severity = Severity.Info,
    private val redactMessages: Boolean = true
) : LogWriter() {

    override fun isLoggable(tag: String, severity: Severity): Boolean {
        return severity >= minSeverity
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val finalMessage = if (redactMessages) LogRedactor.redact(message) else message
        val formatted = "[$severity][$tag] $finalMessage"

        crashReporter.log(formatted)

        // Also record non-fatal exceptions for Error/Assert
        if (throwable != null && severity >= Severity.Error) {
            crashReporter.recordException(throwable)
        }
    }
}
```

### Factory Function

```kotlin
// shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriter.kt

fun createCrashlyticsLogWriter(
    crashReporter: CrashReporter,
    isDebug: Boolean
): CrashlyticsLogWriter {
    return if (isDebug) {
        CrashlyticsLogWriter(
            crashReporter = crashReporter,
            minSeverity = Severity.Verbose,
            redactMessages = false
        )
    } else {
        CrashlyticsLogWriter(
            crashReporter = crashReporter,
            minSeverity = Severity.Info,
            redactMessages = true
        )
    }
}
```

## Integration

### CVAgentApplication Changes

```kotlin
// android-app/src/main/kotlin/io/github/devmugi/cv/agent/CVAgentApplication.kt

override fun onCreate() {
    super.onCreate()

    startKoin {
        androidContext(this@CVAgentApplication)
        modules(
            appModule,
            viewModelModule,
            tracingModule,
            analyticsModule,
            crashlyticsModule
        )
    }

    // Initialize crash reporter and log writer
    CoroutineScope(Dispatchers.Default).launch {
        try {
            val crashReporter = get<CrashReporter>()
            get<CrashReporterInitializer>().initialize()

            // Add Crashlytics breadcrumb logging
            val logWriter = createCrashlyticsLogWriter(
                crashReporter = crashReporter,
                isDebug = BuildConfig.DEBUG
            )
            Logger.addLogWriter(logWriter)

        } catch (e: Exception) {
            Logger.e("CVAgentApplication") { "Failed to initialize crash reporter: ${e.message}" }
        }
    }
}
```

### Dependency Addition

```kotlin
// shared-crashlytics/build.gradle.kts

commonMain.dependencies {
    implementation(libs.kermit)  // Add Kermit dependency
}
```

## Testing

### LogRedactorTest

```kotlin
// shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/LogRedactorTest.kt

class LogRedactorTest {
    @Test
    fun `redacts email addresses`() {
        val input = "User john@example.com logged in"
        val result = LogRedactor.redact(input)
        assertEquals("User [EMAIL] logged in", result)
    }

    @Test
    fun `redacts Bearer tokens`() {
        val input = "Auth: Bearer abc123def456ghi789jkl012mno"
        val result = LogRedactor.redact(input)
        assertEquals("Auth: [TOKEN]", result)
    }

    @Test
    fun `redacts long alphanumeric tokens`() {
        val input = "API key: sk_live_abc123def456ghi789"
        val result = LogRedactor.redact(input)
        assertEquals("API key: [TOKEN]", result)
    }

    @Test
    fun `preserves normal messages`() {
        val input = "Starting chat session"
        val result = LogRedactor.redact(input)
        assertEquals("Starting chat session", result)
    }
}
```

### CrashlyticsLogWriterTest

```kotlin
// shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriterTest.kt

class CrashlyticsLogWriterTest {
    private val testReporter = TestCrashReporter()

    @Test
    fun `filters logs below min severity`() {
        val writer = CrashlyticsLogWriter(testReporter, minSeverity = Severity.Warn)

        assertFalse(writer.isLoggable("TAG", Severity.Debug))
        assertFalse(writer.isLoggable("TAG", Severity.Info))
        assertTrue(writer.isLoggable("TAG", Severity.Warn))
        assertTrue(writer.isLoggable("TAG", Severity.Error))
    }

    @Test
    fun `redacts messages when enabled`() {
        val writer = CrashlyticsLogWriter(testReporter, redactMessages = true)
        writer.log(Severity.Info, "User test@email.com", "TAG", null)

        assertTrue(testReporter.logs.last().contains("[EMAIL]"))
    }

    @Test
    fun `records exceptions for errors`() {
        val writer = CrashlyticsLogWriter(testReporter)
        val exception = RuntimeException("Test")
        writer.log(Severity.Error, "Failed", "TAG", exception)

        assertEquals(exception, testReporter.recordedExceptions.last())
    }
}
```

## Files to Create/Modify

| File | Action |
|------|--------|
| `shared-crashlytics/src/commonMain/.../crashlytics/LogRedactor.kt` | Create |
| `shared-crashlytics/src/commonMain/.../crashlytics/CrashlyticsLogWriter.kt` | Create |
| `shared-crashlytics/src/commonTest/.../crashlytics/LogRedactorTest.kt` | Create |
| `shared-crashlytics/src/commonTest/.../crashlytics/CrashlyticsLogWriterTest.kt` | Create |
| `shared-crashlytics/build.gradle.kts` | Modify (add Kermit dependency) |
| `android-app/src/main/kotlin/.../CVAgentApplication.kt` | Modify |
