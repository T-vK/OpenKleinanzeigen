package de.openkleinanzeigen.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.data.AppRepositories
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

@Composable
fun OpenKleinanzeigenAppUi(repos: AppRepositories) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val topLevelRoutes = TopRoute.entries.toList()
    val showBottomBar = currentDestination?.route in topLevelRoutes.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelRoutes.forEach { top ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    when (top) {
                                        TopRoute.Search -> Icons.Default.Search
                                        TopRoute.Agents -> Icons.Default.Notifications
                                        TopRoute.Messages -> Icons.AutoMirrored.Filled.Message
                                        TopRoute.Settings -> Icons.Default.Settings
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(top.label)) },
                            selected = currentDestination?.hierarchy?.any { it.route == top.route } == true,
                            onClick = {
                                navController.navigate(top.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopRoute.Search.route,
            modifier = Modifier.padding(padding),
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
