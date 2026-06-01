package de.openkleinanzeigen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.data.AppRepositories
import de.openkleinanzeigen.work.WorkScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repos: AppRepositories, onLogs: () -> Unit, onLogin: () -> Unit) {
    val settings by repos.settings.observeSettings().collectAsState(
        initial = de.openkleinanzeigen.core.domain.model.AppSettings(),
    )
    val session by repos.auth.observeSession().collectAsState(initial = null)
    var pollText by remember(settings) { mutableStateOf(settings.pollIntervalSeconds.toString()) }
    var updateText by remember(settings) { mutableStateOf(settings.updateCheckIntervalSeconds.toString()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_account), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            if (session != null) {
                Text(session!!.email)
                Button(onClick = { scope.launch { repos.auth.logout() } }) {
                    Text(stringResource(R.string.logout_button))
                }
            } else {
                Button(onClick = onLogin) { Text(stringResource(R.string.login_button)) }
            }

            OutlinedTextField(
                pollText,
                { pollText = it },
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_poll_interval)) },
            )
            OutlinedTextField(
                updateText,
                { updateText = it },
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_update_interval)) },
            )

            SettingSwitch(stringResource(R.string.settings_auto_update), settings.autoUpdateEnabled) { v ->
                scope.launch {
                    repos.settings.updateSettings { it.copy(autoUpdateEnabled = v) }
                    WorkScheduler.scheduleAll(context)
                }
            }
            SettingSwitch(stringResource(R.string.settings_debug_logging), settings.debugLoggingEnabled) { v ->
                scope.launch { repos.settings.updateSettings { it.copy(debugLoggingEnabled = v) } }
            }
            SettingSwitch(stringResource(R.string.settings_notify_listings), settings.notifyNewListings) { v ->
                scope.launch { repos.settings.updateSettings { it.copy(notifyNewListings = v) } }
            }
            SettingSwitch(stringResource(R.string.settings_notify_messages), settings.notifyMessages) { v ->
                scope.launch { repos.settings.updateSettings { it.copy(notifyMessages = v) } }
            }
            SettingSwitch(stringResource(R.string.settings_notify_agents), settings.notifyAgentMatches) { v ->
                scope.launch { repos.settings.updateSettings { it.copy(notifyAgentMatches = v) } }
            }
            SettingSwitch(stringResource(R.string.settings_notify_backend), settings.notifyBackendAgents) { v ->
                scope.launch { repos.settings.updateSettings { it.copy(notifyBackendAgents = v) } }
            }

            Button(
                onClick = {
                    scope.launch {
                        repos.settings.updateSettings {
                            it.copy(
                                pollIntervalSeconds = pollText.toLongOrNull()?.coerceAtLeast(30) ?: 300,
                                updateCheckIntervalSeconds = updateText.toLongOrNull()?.coerceAtLeast(5) ?: 5,
                            )
                        }
                        WorkScheduler.scheduleAll(context)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.agent_save)) }

            TextButton(onClick = onLogs) { Text(stringResource(R.string.settings_view_logs)) }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    RowSetting(label, checked, onChecked)
}

@Composable
private fun RowSetting(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
