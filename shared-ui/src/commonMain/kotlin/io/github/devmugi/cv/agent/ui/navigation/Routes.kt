package io.github.devmugi.cv.agent.ui.navigation

object Routes {
    const val CHAT = "chat"
    const val TIMELINE = "timeline"
    const val DETAILS = "details/{projectId}"

    fun details(projectId: String) = "details/$projectId"
}
