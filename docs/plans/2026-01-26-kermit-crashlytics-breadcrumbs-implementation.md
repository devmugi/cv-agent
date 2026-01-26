# Kermit Crashlytics Breadcrumbs Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Forward Kermit logs to Firebase Crashlytics as breadcrumbs with configurable severity filtering and pattern redaction.

**Architecture:** Create a Kermit `LogWriter` that forwards log messages to the existing `CrashReporter.log()` interface. Include a `LogRedactor` for sanitizing sensitive data (emails, tokens) in release builds. Configuration is based on `BuildConfig.DEBUG`.

**Tech Stack:** Kotlin Multiplatform, Kermit logging, Firebase Crashlytics, Koin DI

---

## Task 1: Add Kermit Dependency to shared-crashlytics

**Files:**
- Modify: `shared-crashlytics/build.gradle.kts:24`

**Step 1: Add Kermit to commonMain dependencies**

Add `implementation(libs.kermit)` to the commonMain dependencies block:

```kotlin
sourceSets {
    commonMain.dependencies {
        api(projects.sharedIdentity)
        implementation(libs.koin.core)
        implementation(libs.kermit)
    }
    // ... rest unchanged
}
```

**Step 2: Verify the project syncs**

Run: `./gradlew :shared-crashlytics:compileAndroidMain --dry-run`
Expected: BUILD SUCCESSFUL (dry run completes without errors)

**Step 3: Commit**

```bash
git add shared-crashlytics/build.gradle.kts
git commit -m "build(crashlytics): add Kermit dependency for log writer"
```

---

## Task 2: Create LogRedactor with Tests (TDD)

**Files:**
- Create: `shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/LogRedactorTest.kt`
- Create: `shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/LogRedactor.kt`

**Step 1: Write failing tests for LogRedactor**

```kotlin
package io.github.devmugi.cv.agent.crashlytics

import kotlin.test.Test
import kotlin.test.assertEquals

class LogRedactorTest {

    @Test
    fun redactsEmailAddresses() {
        val input = "User john@example.com logged in"
        val result = LogRedactor.redact(input)
        assertEquals("User [EMAIL] logged in", result)
    }

    @Test
    fun redactsMultipleEmails() {
        val input = "From alice@test.org to bob@example.com"
        val result = LogRedactor.redact(input)
        assertEquals("From [EMAIL] to [EMAIL]", result)
    }

    @Test
    fun redactsBearerTokens() {
        val input = "Auth: Bearer abc123def456ghi789jkl012mno"
        val result = LogRedactor.redact(input)
        assertEquals("Auth: [TOKEN]", result)
    }

    @Test
    fun redactsLongAlphanumericTokens() {
        val input = "API key: sk_live_abc123def456ghi789"
        val result = LogRedactor.redact(input)
        assertEquals("API key: [TOKEN]", result)
    }

    @Test
    fun redactsTokensWithUnderscoresAndDashes() {
        val input = "Token: gsk_abc123-def456_ghi789-jkl"
        val result = LogRedactor.redact(input)
        assertEquals("Token: [TOKEN]", result)
    }

    @Test
    fun preservesNormalMessages() {
        val input = "Starting chat session"
        val result = LogRedactor.redact(input)
        assertEquals("Starting chat session", result)
    }

    @Test
    fun preservesShortStrings() {
        val input = "User ID: abc123"
        val result = LogRedactor.redact(input)
        assertEquals("User ID: abc123", result)
    }

    @Test
    fun handlesEmptyString() {
        val result = LogRedactor.redact("")
        assertEquals("", result)
    }

    @Test
    fun redactsBothEmailAndToken() {
        val input = "User test@email.com with token abc123def456ghi789jkl012"
        val result = LogRedactor.redact(input)
        assertEquals("User [EMAIL] with token [TOKEN]", result)
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :shared-crashlytics:testDebugUnitTest --tests "io.github.devmugi.cv.agent.crashlytics.LogRedactorTest" 2>&1 | head -30`
Expected: FAIL - "Unresolved reference: LogRedactor"

**Step 3: Implement LogRedactor**

```kotlin
package io.github.devmugi.cv.agent.crashlytics

/**
 * Redacts sensitive patterns from log messages.
 * Used to sanitize breadcrumb logs before sending to Crashlytics.
 */
object LogRedactor {
    private val EMAIL_PATTERN = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val TOKEN_PATTERN = Regex("\\b(Bearer\\s+)?[A-Za-z0-9_-]{20,}\\b")

    /**
     * Redacts sensitive patterns from the message.
     * - Email addresses → [EMAIL]
     * - Long alphanumeric tokens (20+ chars) → [TOKEN]
     * - Bearer tokens → [TOKEN]
     */
    fun redact(message: String): String {
        return message
            .replace(EMAIL_PATTERN, "[EMAIL]")
            .replace(TOKEN_PATTERN, "[TOKEN]")
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :shared-crashlytics:testDebugUnitTest --tests "io.github.devmugi.cv.agent.crashlytics.LogRedactorTest"`
Expected: BUILD SUCCESSFUL, all 9 tests pass

**Step 5: Commit**

```bash
git add shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/LogRedactor.kt \
        shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/LogRedactorTest.kt
git commit -m "feat(crashlytics): add LogRedactor for sensitive data sanitization"
```

---

## Task 3: Create CrashlyticsLogWriter with Tests (TDD)

**Files:**
- Create: `shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriterTest.kt`
- Create: `shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriter.kt`

**Step 1: Write failing tests for CrashlyticsLogWriter**

```kotlin
package io.github.devmugi.cv.agent.crashlytics

import co.touchlab.kermit.Severity
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrashlyticsLogWriterTest {
    private lateinit var testReporter: TestCrashReporter

    @BeforeTest
    fun setup() {
        testReporter = TestCrashReporter()
    }

    @Test
    fun filtersLogsBelowMinSeverity() {
        val writer = CrashlyticsLogWriter(testReporter, minSeverity = Severity.Warn)

        assertFalse(writer.isLoggable("TAG", Severity.Verbose))
        assertFalse(writer.isLoggable("TAG", Severity.Debug))
        assertFalse(writer.isLoggable("TAG", Severity.Info))
        assertTrue(writer.isLoggable("TAG", Severity.Warn))
        assertTrue(writer.isLoggable("TAG", Severity.Error))
        assertTrue(writer.isLoggable("TAG", Severity.Assert))
    }

    @Test
    fun allowsAllLogsWhenMinSeverityIsVerbose() {
        val writer = CrashlyticsLogWriter(testReporter, minSeverity = Severity.Verbose)

        assertTrue(writer.isLoggable("TAG", Severity.Verbose))
        assertTrue(writer.isLoggable("TAG", Severity.Debug))
        assertTrue(writer.isLoggable("TAG", Severity.Info))
    }

    @Test
    fun logsMessageToCrashReporter() {
        val writer = CrashlyticsLogWriter(testReporter, minSeverity = Severity.Info, redactMessages = false)
        writer.log(Severity.Info, "Test message", "TestTag", null)

        assertEquals(1, testReporter.loggedMessages.size)
        assertEquals("[Info][TestTag] Test message", testReporter.loggedMessages.first())
    }

    @Test
    fun redactsMessagesWhenEnabled() {
        val writer = CrashlyticsLogWriter(testReporter, minSeverity = Severity.Info, redactMessages = true)
        writer.log(Severity.Info, "User test@email.com logged in", "TAG", null)

        assertEquals(1, testReporter.loggedMessages.size)
        assertTrue(testReporter.loggedMessages.first().contains("[EMAIL]"))
        assertFalse(testReporter.loggedMessages.first().contains("test@email.com"))
    }

    @Test
    fun doesNotRedactWhenDisabled() {
        val writer = CrashlyticsLogWriter(testReporter, minSeverity = Severity.Info, redactMessages = false)
        writer.log(Severity.Info, "User test@email.com logged in", "TAG", null)

        assertTrue(testReporter.loggedMessages.first().contains("test@email.com"))
    }

    @Test
    fun recordsExceptionsForErrorSeverity() {
        val writer = CrashlyticsLogWriter(testReporter, minSeverity = Severity.Info)
        val exception = RuntimeException("Test error")
        writer.log(Severity.Error, "Failed operation", "TAG", exception)

        assertEquals(1, testReporter.recordedExceptions.size)
        assertEquals(exception, testReporter.recordedExceptions.first())
    }

    @Test
    fun recordsExceptionsForAssertSeverity() {
        val writer = CrashlyticsLogWriter(testReporter, minSeverity = Severity.Info)
        val exception = AssertionError("Assertion failed")
        writer.log(Severity.Assert, "Assertion", "TAG", exception)

        assertEquals(1, testReporter.recordedExceptions.size)
        assertEquals(exception, testReporter.recordedExceptions.first())
    }

    @Test
    fun doesNotRecordExceptionsForWarnSeverity() {
        val writer = CrashlyticsLogWriter(testReporter, minSeverity = Severity.Info)
        val exception = RuntimeException("Warning")
        writer.log(Severity.Warn, "Warning message", "TAG", exception)

        assertEquals(0, testReporter.recordedExceptions.size)
        assertEquals(1, testReporter.loggedMessages.size)
    }

    @Test
    fun doesNotRecordExceptionWhenNull() {
        val writer = CrashlyticsLogWriter(testReporter, minSeverity = Severity.Info)
        writer.log(Severity.Error, "Error without exception", "TAG", null)

        assertEquals(0, testReporter.recordedExceptions.size)
        assertEquals(1, testReporter.loggedMessages.size)
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :shared-crashlytics:testDebugUnitTest --tests "io.github.devmugi.cv.agent.crashlytics.CrashlyticsLogWriterTest" 2>&1 | head -30`
Expected: FAIL - "Unresolved reference: CrashlyticsLogWriter"

**Step 3: Implement CrashlyticsLogWriter**

```kotlin
package io.github.devmugi.cv.agent.crashlytics

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

/**
 * Kermit [LogWriter] that forwards log messages to [CrashReporter] as breadcrumbs.
 * When a crash occurs, these logs appear in the Crashlytics timeline.
 *
 * @param crashReporter The crash reporter to forward logs to
 * @param minSeverity Minimum severity level to log (logs below this are filtered)
 * @param redactMessages Whether to redact sensitive patterns (emails, tokens)
 */
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

        // Record non-fatal exceptions for Error and Assert severity
        if (throwable != null && severity >= Severity.Error) {
            crashReporter.recordException(throwable)
        }
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :shared-crashlytics:testDebugUnitTest --tests "io.github.devmugi.cv.agent.crashlytics.CrashlyticsLogWriterTest"`
Expected: BUILD SUCCESSFUL, all 10 tests pass

**Step 5: Commit**

```bash
git add shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriter.kt \
        shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriterTest.kt
git commit -m "feat(crashlytics): add CrashlyticsLogWriter for breadcrumb logging"
```

---

## Task 4: Add Factory Function with Tests

**Files:**
- Modify: `shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriterTest.kt`
- Modify: `shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriter.kt`

**Step 1: Add failing tests for factory function**

Append to `CrashlyticsLogWriterTest.kt`:

```kotlin
class CreateCrashlyticsLogWriterTest {
    private lateinit var testReporter: TestCrashReporter

    @BeforeTest
    fun setup() {
        testReporter = TestCrashReporter()
    }

    @Test
    fun debugModeAllowsAllSeverities() {
        val writer = createCrashlyticsLogWriter(testReporter, isDebug = true)

        assertTrue(writer.isLoggable("TAG", Severity.Verbose))
        assertTrue(writer.isLoggable("TAG", Severity.Debug))
        assertTrue(writer.isLoggable("TAG", Severity.Info))
    }

    @Test
    fun debugModeDoesNotRedact() {
        val writer = createCrashlyticsLogWriter(testReporter, isDebug = true)
        writer.log(Severity.Info, "User test@email.com", "TAG", null)

        assertTrue(testReporter.loggedMessages.first().contains("test@email.com"))
    }

    @Test
    fun releaseModeFiltersDebugLogs() {
        val writer = createCrashlyticsLogWriter(testReporter, isDebug = false)

        assertFalse(writer.isLoggable("TAG", Severity.Verbose))
        assertFalse(writer.isLoggable("TAG", Severity.Debug))
        assertTrue(writer.isLoggable("TAG", Severity.Info))
        assertTrue(writer.isLoggable("TAG", Severity.Warn))
    }

    @Test
    fun releaseModeRedactsMessages() {
        val writer = createCrashlyticsLogWriter(testReporter, isDebug = false)
        writer.log(Severity.Info, "User test@email.com", "TAG", null)

        assertTrue(testReporter.loggedMessages.first().contains("[EMAIL]"))
        assertFalse(testReporter.loggedMessages.first().contains("test@email.com"))
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :shared-crashlytics:testDebugUnitTest --tests "io.github.devmugi.cv.agent.crashlytics.CreateCrashlyticsLogWriterTest" 2>&1 | head -30`
Expected: FAIL - "Unresolved reference: createCrashlyticsLogWriter"

**Step 3: Add factory function to CrashlyticsLogWriter.kt**

Append to `CrashlyticsLogWriter.kt`:

```kotlin
/**
 * Creates a [CrashlyticsLogWriter] configured for the build type.
 *
 * Debug builds: All logs, no redaction
 * Release builds: Info+ only, redaction enabled
 *
 * @param crashReporter The crash reporter to forward logs to
 * @param isDebug Whether this is a debug build
 */
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

**Step 4: Run all crashlytics tests to verify they pass**

Run: `./gradlew :shared-crashlytics:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass

**Step 5: Commit**

```bash
git add shared-crashlytics/src/commonMain/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriter.kt \
        shared-crashlytics/src/commonTest/kotlin/io/github/devmugi/cv/agent/crashlytics/CrashlyticsLogWriterTest.kt
git commit -m "feat(crashlytics): add factory function for build-type configuration"
```

---

## Task 5: Integrate into CVAgentApplication

**Files:**
- Modify: `android-app/src/main/kotlin/io/github/devmugi/cv/agent/CVAgentApplication.kt`

**Step 1: Add import and integrate log writer**

Update `CVAgentApplication.kt`:

```kotlin
package io.github.devmugi.cv.agent

import android.app.Application
import co.touchlab.kermit.Logger
import io.github.devmugi.cv.agent.crashlytics.CrashReporter
import io.github.devmugi.cv.agent.crashlytics.CrashReporterInitializer
import io.github.devmugi.cv.agent.crashlytics.crashlyticsModule
import io.github.devmugi.cv.agent.crashlytics.createCrashlyticsLogWriter
import io.github.devmugi.cv.agent.di.analyticsModule
import io.github.devmugi.cv.agent.di.appModule
import io.github.devmugi.cv.agent.di.tracingModule
import io.github.devmugi.cv.agent.di.viewModelModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class CVAgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the API key for the shared module
        GroqConfigProvider.initialize(BuildConfig.GROQ_API_KEY)

        startKoin {
            androidLogger()
            androidContext(this@CVAgentApplication)
            allowOverride(true) // Allow tracingModule to override appModule definitions
            modules(appModule, viewModelModule, tracingModule, analyticsModule, crashlyticsModule)
        }

        // Initialize crash reporter and breadcrumb logging
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
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
}
```

**Step 2: Verify the app builds**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add android-app/src/main/kotlin/io/github/devmugi/cv/agent/CVAgentApplication.kt
git commit -m "feat(app): integrate Crashlytics breadcrumb logging"
```

---

## Task 6: Run Full Quality Check

**Step 1: Run quality checks**

Run: `./gradlew qualityCheck`
Expected: BUILD SUCCESSFUL (ktlint and detekt pass)

**Step 2: Run all shared-crashlytics tests**

Run: `./gradlew :shared-crashlytics:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass

**Step 3: Build the app**

Run: `./gradlew :android-app:assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Summary

After completing all tasks, you will have:

1. **LogRedactor** - Sanitizes emails and tokens from log messages
2. **CrashlyticsLogWriter** - Kermit LogWriter forwarding to CrashReporter
3. **createCrashlyticsLogWriter()** - Factory for build-type configuration
4. **CVAgentApplication integration** - Wiring at app startup

**Test coverage:**
- 9 tests for LogRedactor (pattern matching edge cases)
- 10 tests for CrashlyticsLogWriter (severity filtering, redaction, exception recording)
- 4 tests for factory function (debug vs release configuration)
