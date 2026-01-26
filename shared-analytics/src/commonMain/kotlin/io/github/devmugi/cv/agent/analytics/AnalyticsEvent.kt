package io.github.devmugi.cv.agent.analytics

/**
 * Type-safe analytics event hierarchy.
 * Each event has a name and typed parameters.
 */
sealed class AnalyticsEvent(val name: String) {
    abstract val params: Map<String, Any?>

    // ============ CHAT EVENTS ============
    sealed class Chat(name: String) : AnalyticsEvent(name) {

        data class MessageSent(
            val messageLength: Int,
            val sessionId: String,
            val turnNumber: Int
        ) : Chat("message_sent") {
            override val params = mapOf(
                "message_length" to messageLength,
                "session_id" to sessionId,
                "turn_number" to turnNumber
            )
        }

        data class SuggestionClicked(
            val projectId: String,
            val position: Int
        ) : Chat("suggestion_clicked") {
            override val params = mapOf(
                "project_id" to projectId,
                "position" to position
            )
        }

        data class MessageCopied(
            val messageId: String,
            val messageLength: Int
        ) : Chat("message_copied") {
            override val params = mapOf(
                "message_id" to messageId,
                "message_length" to messageLength
            )
        }

        data class MessageLiked(val messageId: String) : Chat("message_liked") {
            override val params = mapOf("message_id" to messageId)
        }

        data class MessageDisliked(val messageId: String) : Chat("message_disliked") {
            override val params = mapOf("message_id" to messageId)
        }

        data class RegenerateClicked(
            val messageId: String,
            val turnNumber: Int
        ) : Chat("regenerate_clicked") {
            override val params = mapOf(
                "message_id" to messageId,
                "turn_number" to turnNumber
            )
        }

        data class HistoryCleared(
            val messageCount: Int,
            val sessionId: String
        ) : Chat("clear_history") {
            override val params = mapOf(
                "message_count" to messageCount,
                "session_id" to sessionId
            )
        }

        data class ResponseCompleted(
            val responseTimeMs: Long,
            val tokenCount: Int?,
            val sessionId: String
        ) : Chat("response_completed") {
            override val params = mapOf(
                "response_time_ms" to responseTimeMs,
                "token_count" to tokenCount,
                "session_id" to sessionId
            )
        }
    }

    // ============ NAVIGATION EVENTS ============
    sealed class Navigation(name: String) : AnalyticsEvent(name) {

        enum class Screen { CHAT, CAREER_TIMELINE, PROJECT_DETAILS }
        enum class NavigationMethod { BUTTON, GESTURE }
        enum class SelectionSource { TIMELINE, CHAT_SUGGESTION, CHAT_LINK }

        data class ScreenView(
            val screenName: Screen,
            val previousScreen: Screen? = null
        ) : Navigation("screen_view") {
            override val params = mapOf(
                "screen_name" to screenName.name.lowercase(),
                "previous_screen" to previousScreen?.name?.lowercase()
            )
        }

        data class BackNavigation(
            val fromScreen: String,
            val toScreen: String,
            val method: NavigationMethod
        ) : Navigation("back_navigation") {
            override val params = mapOf(
                "from_screen" to fromScreen,
                "to_screen" to toScreen,
                "method" to method.name.lowercase()
            )
        }

        data class ProjectSelected(
            val projectId: String,
            val source: SelectionSource
        ) : Navigation("project_selected") {
            override val params = mapOf(
                "project_id" to projectId,
                "source" to source.name.lowercase()
            )
        }
    }

    // ============ CONTACT/LINK EVENTS ============
    sealed class Link(name: String) : AnalyticsEvent(name) {

        enum class LinkType { LINKEDIN, GITHUB, EMAIL, PHONE, CV_WEBSITE, CV_PDF }

        data class ExternalLinkClicked(
            val linkType: LinkType,
            val url: String? = null
        ) : Link("external_link_clicked") {
            override val params = mapOf(
                "link_type" to linkType.name.lowercase(),
                "url" to url
            )
        }

        data class ProjectLinkClicked(
            val projectId: String,
            val linkType: String,
            val url: String
        ) : Link("project_link_clicked") {
            override val params = mapOf(
                "project_id" to projectId,
                "link_type" to linkType,
                "url" to url
            )
        }
    }

    // ============ ERROR EVENTS ============
    sealed class Error(name: String) : AnalyticsEvent(name) {

        enum class ErrorType { NETWORK, RATE_LIMIT, API, AUTH }

        data class ErrorDisplayed(
            val errorType: ErrorType,
            val errorMessage: String?,
            val sessionId: String?
        ) : Error("error_displayed") {
            override val params = mapOf(
                "error_type" to errorType.name.lowercase(),
                "error_message" to errorMessage?.take(100), // Truncate for Firebase limit
                "session_id" to sessionId
            )
        }

        data class ErrorRetried(
            val errorType: String,
            val retryCount: Int
        ) : Error("error_retried") {
            override val params = mapOf(
                "error_type" to errorType,
                "retry_count" to retryCount
            )
        }
    }

    // ============ SESSION EVENTS ============
    sealed class Session(name: String) : AnalyticsEvent(name) {

        data class SessionStart(
            val sessionId: String,
            val isNewInstall: Boolean = false
        ) : Session("session_start") {
            override val params = mapOf(
                "session_id" to sessionId,
                "is_new_install" to isNewInstall
            )
        }

        data class SessionResume(
            val sessionId: String,
            val messageCount: Int
        ) : Session("session_resume") {
            override val params = mapOf(
                "session_id" to sessionId,
                "message_count" to messageCount
            )
        }
    }
}
