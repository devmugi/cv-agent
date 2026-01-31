package io.github.devmugi.cv.agent.ui.navigation

/**
 * Navigation routes for the app.
 * Uses string-based routes for cross-platform compatibility.
 */
object Routes {
    const val CHAT = "chat"
    const val TIMELINE = "timeline"
    const val DETAILS = "details/{projectId}"

    fun details(projectId: String) = "details/$projectId"
}
