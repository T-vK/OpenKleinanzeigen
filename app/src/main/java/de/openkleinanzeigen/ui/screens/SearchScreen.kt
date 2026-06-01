package de.openkleinanzeigen.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import de.openkleinanzeigen.core.domain.model.SearchQuery
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(repos: AppRepositories, onListingClick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("") }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Listing>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search_hint)) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(locationId, { locationId = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.location_hint)) })
            OutlinedTextField(radius, { radius = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.radius_hint)) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(minPrice, { minPrice = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.min_price_hint)) })
            OutlinedTextField(maxPrice, { maxPrice = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.max_price_hint)) })
        }
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    try {
                        results = repos.listing.search(
                            SearchQuery(
                                query = query,
                                locationId = locationId.toIntOrNull(),
                                radiusKm = radius.toIntOrNull(),
                                minPrice = minPrice.toIntOrNull(),
                                maxPrice = maxPrice.toIntOrNull(),
                            ),
                        )
                    } catch (e: Exception) {
                        error = e.message ?: "Error"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.search_button)) }

        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            error != null -> Text(error!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            results.isEmpty() -> Text(stringResource(R.string.no_results))
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                items(results, key = { it.id }) { listing ->
                    ListingCard(listing, onClick = { onListingClick(listing.id) })
                }
            }
        }
    }
}

@Composable
private fun ListingCard(listing: Listing, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listing.imageUrl?.let { url ->
                AsyncImage(url, null, Modifier.size(72.dp), contentScale = ContentScale.Crop)
            }
            Column {
                Text(listing.title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                listing.priceLabel?.let { Text(it) }
                listing.location?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
