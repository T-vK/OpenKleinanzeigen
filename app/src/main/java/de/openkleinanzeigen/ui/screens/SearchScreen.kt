package de.openkleinanzeigen.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.data.AppRepositories
import de.openkleinanzeigen.core.domain.model.Listing
import de.openkleinanzeigen.core.domain.model.Location
import de.openkleinanzeigen.core.domain.model.SearchQuery
import de.openkleinanzeigen.ui.components.LocationRadiusField
import de.openkleinanzeigen.ui.components.LocationSelection
import de.openkleinanzeigen.ui.components.SearchFiltersSheet
import de.openkleinanzeigen.ui.components.SearchFiltersState
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(repos: AppRepositories, onListingClick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("") }
    var locationSelection by remember { mutableStateOf<LocationSelection?>(null) }
    var radius by remember { mutableStateOf("25") }
    var filters by remember { mutableStateOf(SearchFiltersState()) }
    var showFilters by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<Location>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Listing>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val germanyLabel = stringResource(R.string.location_germany)
    val germanyHint = stringResource(R.string.location_germany_hint)
    val genericError = stringResource(R.string.error_generic)

    fun buildQuery(): SearchQuery {
        val base = SearchQuery(
            query = query.trim(),
            locationId = locationSelection?.id,
            locationName = locationSelection?.displayName,
            radiusKm = radius.toIntOrNull()?.takeIf { it > 0 },
        )
        return filters.applyTo(base)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
        )

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
                    runCatching { repos.listing.searchLocations(q) }
                        .onSuccess { suggestions = it }
                }
            },
            germanyLabel = germanyLabel,
            germanyHint = germanyHint,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showFilters = true },
                modifier = Modifier.weight(1f),
            ) {
                BadgedBox(
                    badge = {
                        if (filters.activeCount() > 0) {
                            Badge { Text(filters.activeCount().toString()) }
                        }
                    },
                ) {
                    Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                }
                Text(
                    stringResource(R.string.filters_button),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            results = repos.listing.search(buildQuery())
                        } catch (e: Exception) {
                            error = e.message ?: genericError
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !loading,
            ) { Text(stringResource(R.string.search_button)) }
        }

        when {
            loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            results.isEmpty() -> Text(
                stringResource(R.string.no_results),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = { it.id }) { listing ->
                    ListingCard(listing, onClick = { onListingClick(listing.id) })
                }
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

@Composable
private fun ListingCard(listing: Listing, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (listing.imageUrl != null) {
                AsyncImage(
                    listing.imageUrl,
                    contentDescription = null,
                    Modifier.size(88.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier
                        .size(88.dp)
                        .padding(0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("—", style = MaterialTheme.typography.headlineSmall)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(listing.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                listing.priceLabel?.let {
                    Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                listing.location?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
