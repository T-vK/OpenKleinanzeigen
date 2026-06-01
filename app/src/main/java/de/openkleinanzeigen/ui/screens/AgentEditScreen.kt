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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.data.AppRepositories
import de.openkleinanzeigen.core.domain.model.LocalSearchAgent
import de.openkleinanzeigen.core.domain.model.SearchQuery
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentEditScreen(repos: AppRepositories, agentId: Long?, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf("") }
    var autoMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(agentId) {
        agentId?.let { id ->
            repos.searchAgent.getAgent(id)?.let { agent ->
                name = agent.name
                query = agent.query.query
                locationId = agent.query.locationId?.toString().orEmpty()
                autoMessage = agent.autoMessageTemplate.orEmpty()
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.agents_add)) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.agent_name)) })
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search_hint)) })
            OutlinedTextField(locationId, { locationId = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.location_hint)) })
            OutlinedTextField(autoMessage, { autoMessage = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.agent_auto_message)) })
            Button(
                onClick = {
                    scope.launch {
                        repos.searchAgent.saveAgent(
                            LocalSearchAgent(
                                id = agentId ?: 0,
                                name = name.ifBlank { query },
                                query = SearchQuery(query = query, locationId = locationId.toIntOrNull()),
                                autoMessageTemplate = autoMessage.ifBlank { null },
                            ),
                        )
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.agent_save)) }
            if (agentId != null) {
                TextButton(
                    onClick = {
                        scope.launch {
                            repos.searchAgent.deleteAgent(agentId)
                            onDone()
                        }
                    },
                ) { Text(stringResource(R.string.agent_delete)) }
            }
        }
    }
}
