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
