package de.openkleinanzeigen.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.domain.model.Location
import kotlinx.coroutines.delay

data class LocationSelection(
    val id: Int?,
    val displayName: String,
)

@Composable
fun LocationRadiusField(
    locationText: String,
    onLocationTextChange: (String) -> Unit,
    radiusKm: String,
    onRadiusChange: (String) -> Unit,
    selection: LocationSelection?,
    onSelectionChange: (LocationSelection?) -> Unit,
    suggestions: List<Location>,
    onSearchQuery: (String) -> Unit,
    germanyLabel: String,
    germanyHint: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(locationText) {
        if (focused && locationText.length >= 2) {
            delay(280)
            onSearchQuery(locationText)
            expanded = true
        } else if (locationText.isBlank()) {
            expanded = false
        }
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = locationText,
                onValueChange = {
                    onLocationTextChange(it)
                    if (selection != null && it != selection.displayName) {
                        onSelectionChange(null)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged {
                        focused = it.isFocused
                        if (!it.isFocused) expanded = false
                    },
                label = { Text(stringResource(R.string.location_label)) },
                placeholder = { Text(stringResource(R.string.location_placeholder)) },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                trailingIcon = {
                    if (locationText.isNotEmpty()) {
                        IconButton(onClick = {
                            onLocationTextChange("")
                            onSelectionChange(null)
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { expanded = false }),
            )
            OutlinedTextField(
                value = radiusKm,
                onValueChange = onRadiusChange,
                modifier = Modifier.fillMaxWidth(0.28f),
                label = { Text(stringResource(R.string.radius_km)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        if (expanded && (suggestions.isNotEmpty() || locationText.length >= 2)) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(max = 240.dp),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                LazyColumn {
                    item {
                        SuggestionRow(
                            title = germanyLabel,
                            subtitle = germanyHint,
                            onClick = {
                                onSelectionChange(LocationSelection(0, germanyLabel))
                                onLocationTextChange(germanyLabel)
                                expanded = false
                            },
                        )
                    }
                    items(suggestions, key = { it.id }) { loc ->
                        SuggestionRow(
                            title = loc.name,
                            subtitle = loc.subtitle,
                            onClick = {
                                onSelectionChange(LocationSelection(loc.id, loc.name))
                                onLocationTextChange(loc.name)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
