package de.openkleinanzeigen.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.data.AppRepositories
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(repos: AppRepositories, onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    val agents by repos.searchAgent.observeAgents().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.agents_add))
            }
        },
    ) { padding ->
        if (agents.isEmpty()) {
            Text(
                stringResource(R.string.agents_empty),
                modifier = Modifier.padding(padding).padding(16.dp),
            )
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(agents, key = { it.id }) { agent ->
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onEdit(agent.id) },
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(agent.name, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            Text(agent.query.query.ifBlank { "*" })
                            Switch(
                                checked = agent.enabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        repos.searchAgent.saveAgent(agent.copy(enabled = enabled))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
