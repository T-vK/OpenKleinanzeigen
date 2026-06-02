package de.openkleinanzeigen.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.data.AppRepositories
import de.openkleinanzeigen.ui.components.AppShell
import de.openkleinanzeigen.ui.screens.AgentEditScreen
import de.openkleinanzeigen.ui.screens.AgentsScreen
import de.openkleinanzeigen.ui.screens.ChatScreen
import de.openkleinanzeigen.ui.screens.ListingDetailScreen
import de.openkleinanzeigen.ui.screens.LogViewerScreen
import de.openkleinanzeigen.ui.screens.LoginScreen
import de.openkleinanzeigen.ui.screens.MessagesScreen
import de.openkleinanzeigen.ui.screens.SearchScreen
import de.openkleinanzeigen.ui.screens.SettingsScreen

enum class TopRoute(val route: String, @StringRes val label: Int) {
    Search("search", R.string.nav_search),
    Agents("agents", R.string.nav_agents),
    Messages("messages", R.string.nav_messages),
    Settings("settings", R.string.nav_settings),
}

private val topLevelRoutes = TopRoute.entries.map { it.route }.toSet()

@Composable
fun OpenKleinanzeigenAppUi(repos: AppRepositories) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")
    val showDrawer = currentRoute in topLevelRoutes

    val titleRes = when (currentRoute) {
        TopRoute.Search.route -> TopRoute.Search.label
        TopRoute.Agents.route -> TopRoute.Agents.label
        TopRoute.Messages.route -> TopRoute.Messages.label
        TopRoute.Settings.route -> TopRoute.Settings.label
        else -> R.string.app_name
    }

    AppShell(
        currentRoute = currentRoute,
        titleRes = titleRes,
        showDrawer = showDrawer,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(TopRoute.Search.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onLogs = { navController.navigate("logs") },
    ) { modifier ->
        NavHost(
            navController = navController,
            startDestination = TopRoute.Search.route,
            modifier = modifier,
        ) {
            composable(TopRoute.Search.route) {
                SearchScreen(
                    repos = repos,
                    onListingClick = { id -> navController.navigate("listing/$id") },
                )
            }
            composable(TopRoute.Agents.route) {
                AgentsScreen(
                    repos = repos,
                    onAdd = { navController.navigate("agent/edit") },
                    onEdit = { id -> navController.navigate("agent/edit/$id") },
                )
            }
            composable(TopRoute.Messages.route) {
                MessagesScreen(
                    repos = repos,
                    onLogin = { navController.navigate("login") },
                    onConversation = { id -> navController.navigate("chat/$id") },
                )
            }
            composable(TopRoute.Settings.route) {
                SettingsScreen(
                    repos = repos,
                    onLogs = { navController.navigate("logs") },
                    onLogin = { navController.navigate("login") },
                )
            }
            composable("listing/{id}") { entry ->
                ListingDetailScreen(
                    listingId = entry.arguments?.getString("id").orEmpty(),
                    repos = repos,
                )
            }
            composable("agent/edit") {
                AgentEditScreen(repos = repos, agentId = null, onDone = { navController.popBackStack() })
            }
            composable("agent/edit/{id}") { entry ->
                AgentEditScreen(
                    repos = repos,
                    agentId = entry.arguments?.getString("id")?.toLongOrNull(),
                    onDone = { navController.popBackStack() },
                )
            }
            composable("chat/{id}") { entry ->
                ChatScreen(
                    conversationId = entry.arguments?.getString("id").orEmpty(),
                    repos = repos,
                )
            }
            composable("login") {
                LoginScreen(repos = repos, onDone = { navController.popBackStack() })
            }
            composable("logs") {
                LogViewerScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
