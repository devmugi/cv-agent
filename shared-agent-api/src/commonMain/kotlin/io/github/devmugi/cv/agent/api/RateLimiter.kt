package io.github.devmugi.cv.agent.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Rate limiter interface for controlling API request frequency.
 */
interface RateLimiter {
    /**
     * Acquires permission to make a request. Suspends if rate limit would be exceeded.
     * Call this before making an API request.
     */
    suspend fun acquire()

    /**
     * Reports a rate limit response from the API.
     * @param retryAfterSeconds Optional retry-after hint from the API (in seconds)
     */
    suspend fun reportRateLimited(retryAfterSeconds: Int?)

    companion object {
        /**
         * No-op rate limiter that doesn't throttle requests.
         */
        val NOOP: RateLimiter = object : RateLimiter {
            override suspend fun acquire() = Unit
            override suspend fun reportRateLimited(retryAfterSeconds: Int?) = Unit
        }
    }
}

/**
 * Platform-specific function to get current time in milliseconds.
 */
internal expect fun currentTimeMillis(): Long

/**
 * Token bucket rate limiter implementation.
 *
 * Ensures minimum delay between requests and respects rate limit responses.
 *
 * @param minDelayMs Minimum delay between requests in milliseconds (default: 2000ms for ~30 RPM)
 * @param timeProvider Function to get current time in milliseconds (for testing)
 */
class TokenBucketRateLimiter(
    private val minDelayMs: Long = DEFAULT_MIN_DELAY_MS,
    private val timeProvider: () -> Long = { currentTimeMillis() }
) : RateLimiter {

    companion object {
        /**
         * Default minimum delay: 2000ms = 30 requests per minute (Groq free tier limit)
         */
        const val DEFAULT_MIN_DELAY_MS = 2000L

        private const val DEFAULT_BACKOFF_SECONDS = 60
    }

    private val mutex = Mutex()
    private var lastRequestTimeMs: Long = 0L
    private var backoffUntilMs: Long = 0L

    override suspend fun acquire() {
        mutex.withLock {
            val now = timeProvider()

            // Respect backoff from rate limit response
            if (now < backoffUntilMs) {
                val waitMs = backoffUntilMs - now
                if (waitMs > 0) {
                    delay(waitMs)
                }
            }

            // Ensure minimum delay between requests
            val timeSinceLastRequest = timeProvider() - lastRequestTimeMs
            if (timeSinceLastRequest < minDelayMs) {
                val waitMs = minDelayMs - timeSinceLastRequest
                delay(waitMs)
            }

            lastRequestTimeMs = timeProvider()
        }
    }

    override suspend fun reportRateLimited(retryAfterSeconds: Int?) {
        mutex.withLock {
            val backoffSeconds = retryAfterSeconds ?: DEFAULT_BACKOFF_SECONDS
            backoffUntilMs = timeProvider() + (backoffSeconds * 1000L)
        }
    }
}
