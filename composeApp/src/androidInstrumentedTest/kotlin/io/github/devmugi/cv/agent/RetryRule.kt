package io.github.devmugi.cv.agent

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryRule(private val maxAttempts: Int = 3) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                var lastError: Throwable? = null
                repeat(maxAttempts) { attempt ->
                    try {
                        base.evaluate()
                        return // Success
                    } catch (e: Throwable) {
                        lastError = e
                        println("${description.methodName}: Attempt ${attempt + 1}/$maxAttempts failed: ${e.message}")
                        if (attempt < maxAttempts - 1) {
                            Thread.sleep(1000) // Wait 1s before retry
                        }
                    }
                }
                throw lastError!!
            }
        }
    }
}
