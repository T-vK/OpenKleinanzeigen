package de.openkleinanzeigen.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.openkleinanzeigen.R
import de.openkleinanzeigen.ui.TopRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    currentRoute: String?,
    @StringRes titleRes: Int,
    showDrawer: Boolean,
    onNavigate: (String) -> Unit,
    onLogs: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drawerItems = buildList {
        add(DrawerItem(TopRoute.Search.route, R.string.nav_search, Icons.Default.Search))
        add(DrawerItem(TopRoute.Agents.route, R.string.nav_agents, Icons.Default.Notifications))
        add(DrawerItem(TopRoute.Messages.route, R.string.nav_messages, Icons.AutoMirrored.Filled.Message))
        add(DrawerItem(TopRoute.Settings.route, R.string.nav_settings, Icons.Default.Settings))
        add(DrawerItem("logs", R.string.logs_title, Icons.Default.BugReport, isLogs = true))
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                )
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, null) },
                        label = { Text(stringResource(item.label)) },
                        selected = currentRoute == item.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (item.isLogs) onLogs() else onNavigate(item.route)
                        },
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                if (showDrawer) {
                    TopAppBar(
                        title = { Text(stringResource(titleRes)) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu_open))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            },
        ) { padding ->
            content(Modifier.padding(padding))
        }
    }
}

private data class DrawerItem(
    val route: String,
    @StringRes val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isLogs: Boolean = false,
)
