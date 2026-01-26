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
