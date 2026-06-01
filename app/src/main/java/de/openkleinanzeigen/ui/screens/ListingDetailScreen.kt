package de.openkleinanzeigen.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.data.AppRepositories
import de.openkleinanzeigen.core.domain.model.Listing

@Composable
fun ListingDetailScreen(listingId: String, repos: AppRepositories) {
    var listing by remember { mutableStateOf<Listing?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(listingId) {
        loading = true
        try {
            listing = repos.listing.getListing(listingId)
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.listing_details), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error!!)
            listing != null -> {
                val item = listing!!
                item.imageUrl?.let { url ->
                    AsyncImage(url, null, Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Crop)
                }
                Text(item.title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                item.priceLabel?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
                item.location?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
                item.description?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
                item.webUrl?.let { url ->
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        modifier = Modifier.padding(top = 16.dp),
                    ) { Text(stringResource(R.string.open_in_browser)) }
                }
            }
        }
    }
}
