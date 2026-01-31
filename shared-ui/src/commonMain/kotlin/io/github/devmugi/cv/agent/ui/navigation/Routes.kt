package io.github.devmugi.cv.agent.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the CV Agent app.
 * Uses @Serializable for Navigation 3 compatibility.
 */
@Serializable
sealed interface Route {
    /**
     * Main chat screen - the app's home/default destination.
     */
    @Serializable
    data object Chat : Route

    /**
     * Career timeline showing all projects chronologically.
     */
    @Serializable
    data object CareerTimeline : Route

    /**
     * Project details screen showing full information for a specific project.
     * @param projectId The unique identifier for the project to display.
     */
    @Serializable
    data class ProjectDetails(val projectId: String) : Route
}
