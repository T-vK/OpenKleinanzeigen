package de.openkleinanzeigen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import de.openkleinanzeigen.core.domain.model.Location
import de.openkleinanzeigen.core.domain.model.SearchQuery
import de.openkleinanzeigen.ui.components.LocationRadiusField
import de.openkleinanzeigen.ui.components.LocationSelection
import de.openkleinanzeigen.ui.components.SearchFiltersSheet
import de.openkleinanzeigen.ui.components.SearchFiltersState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentEditScreen(repos: AppRepositories, agentId: Long?, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("") }
    var locationSelection by remember { mutableStateOf<LocationSelection?>(null) }
    var radius by remember { mutableStateOf("25") }
    var filters by remember { mutableStateOf(SearchFiltersState()) }
    var showFilters by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<Location>>(emptyList()) }
    var autoMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val germanyLabel = stringResource(R.string.location_germany)
    val germanyHint = stringResource(R.string.location_germany_hint)

    LaunchedEffect(agentId) {
        agentId?.let { id ->
            repos.searchAgent.getAgent(id)?.let { agent ->
                name = agent.name
                query = agent.query.query
                locationText = agent.query.locationName.orEmpty()
                locationSelection = agent.query.locationId?.let {
                    LocationSelection(it, agent.query.locationName.orEmpty())
                }
                radius = agent.query.radiusKm?.toString().orEmpty().ifBlank { "25" }
                filters = SearchFiltersState.from(agent.query)
                autoMessage = agent.autoMessageTemplate.orEmpty()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.agents_add)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.agent_name)) })
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search_hint)) })

            LocationRadiusField(
                locationText = locationText,
                onLocationTextChange = { locationText = it },
                radiusKm = radius,
                onRadiusChange = { radius = it.filter { c -> c.isDigit() } },
                selection = locationSelection,
                onSelectionChange = { locationSelection = it },
                suggestions = suggestions,
                onSearchQuery = { q ->
                    scope.launch {
                        runCatching { repos.listing.searchLocations(q) }.onSuccess { suggestions = it }
                    }
                },
                germanyLabel = germanyLabel,
                germanyHint = germanyHint,
            )

            OutlinedButton(onClick = { showFilters = true }, modifier = Modifier.fillMaxWidth()) {
                BadgedBox(badge = {
                    if (filters.activeCount() > 0) Badge { Text(filters.activeCount().toString()) }
                }) {
                    Icon(Icons.Default.Tune, null)
                }
                Text(stringResource(R.string.filters_button), Modifier.padding(start = 8.dp))
            }

            OutlinedTextField(
                autoMessage,
                { autoMessage = it },
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.agent_auto_message)) },
                minLines = 2,
            )

            Button(
                onClick = {
                    scope.launch {
                        val base = SearchQuery(
                            query = query.trim(),
                            locationId = locationSelection?.id,
                            locationName = locationSelection?.displayName,
                            radiusKm = radius.toIntOrNull()?.takeIf { it > 0 },
                        )
                        repos.searchAgent.saveAgent(
                            LocalSearchAgent(
                                id = agentId ?: 0,
                                name = name.ifBlank { query },
                                query = filters.applyTo(base),
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

    SearchFiltersSheet(
        visible = showFilters,
        state = filters,
        onStateChange = { filters = it },
        onDismiss = { showFilters = false },
        onApply = { },
    )
}
