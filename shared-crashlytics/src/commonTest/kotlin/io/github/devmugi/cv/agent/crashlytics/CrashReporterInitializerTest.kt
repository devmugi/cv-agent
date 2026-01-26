package io.github.devmugi.cv.agent.crashlytics

import io.github.devmugi.cv.agent.identity.TestInstallationIdentity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CrashReporterInitializerTest {

    @Test
    fun `initialize sets installation id as user id`() = runTest {
        val testCrashReporter = TestCrashReporter()
        val testIdentity = TestInstallationIdentity("test-install-123")
        val initializer = CrashReporterInitializer(testCrashReporter, testIdentity)

        initializer.initialize()

        assertEquals("test-install-123", testCrashReporter.userId)
    }

    @Test
    fun `initialize calls getInstallationId once`() = runTest {
        val testCrashReporter = TestCrashReporter()
        val testIdentity = TestInstallationIdentity("any-id")
        val initializer = CrashReporterInitializer(testCrashReporter, testIdentity)

        initializer.initialize()

        assertEquals(1, testIdentity.getIdCallCount)
    }
}
