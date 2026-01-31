package io.github.devmugi.cv.agent.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.devmugi.cv.agent.ui.navigation.Route

@Immutable
data class NavDestination(
    val route: Route,
    val icon: ImageVector,
    val label: String
)

val defaultDestinations = listOf(
    NavDestination(Route.Chat, Icons.Default.Chat, "Chat"),
    NavDestination(Route.CareerTimeline, Icons.Default.Timeline, "Career")
)

@Composable
fun AdaptiveScaffold(
    windowSizeClass: WindowWidthSizeClass,
    currentRoute: Route,
    onNavigate: (Route) -> Unit,
    destinations: List<NavDestination> = defaultDestinations,
    content: @Composable () -> Unit
) {
    when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> {
            Scaffold(
                bottomBar = {
                    AdaptiveBottomBar(currentRoute, destinations, onNavigate)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    content()
                }
            }
        }
        WindowWidthSizeClass.Medium -> {
            Row(modifier = Modifier.fillMaxSize()) {
                AdaptiveNavRail(currentRoute, destinations, onNavigate)
                content()
            }
        }
        WindowWidthSizeClass.Expanded -> {
            PermanentNavigationDrawer(
                drawerContent = {
                    AdaptiveNavDrawer(currentRoute, destinations, onNavigate)
                }
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AdaptiveBottomBar(
    currentRoute: Route,
    destinations: List<NavDestination>,
    onNavigate: (Route) -> Unit
) {
    NavigationBar {
        destinations.forEach { dest ->
            NavigationBarItem(
                selected = isRouteSelected(currentRoute, dest.route),
                onClick = { onNavigate(dest.route) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}

@Composable
private fun AdaptiveNavRail(
    currentRoute: Route,
    destinations: List<NavDestination>,
    onNavigate: (Route) -> Unit
) {
    NavigationRail {
        destinations.forEach { dest ->
            NavigationRailItem(
                selected = isRouteSelected(currentRoute, dest.route),
                onClick = { onNavigate(dest.route) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}

@Composable
private fun AdaptiveNavDrawer(
    currentRoute: Route,
    destinations: List<NavDestination>,
    onNavigate: (Route) -> Unit
) {
    PermanentDrawerSheet {
        destinations.forEach { dest ->
            NavigationDrawerItem(
                selected = isRouteSelected(currentRoute, dest.route),
                onClick = { onNavigate(dest.route) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}

private fun isRouteSelected(currentRoute: Route, destinationRoute: Route): Boolean {
    return when {
        currentRoute == destinationRoute -> true
        currentRoute is Route.ProjectDetails && destinationRoute is Route.CareerTimeline -> true
        else -> false
    }
}
