package de.openkleinanzeigen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.domain.model.AdType
import de.openkleinanzeigen.core.domain.model.PosterType
import de.openkleinanzeigen.core.domain.model.SearchQuery

data class SearchFiltersState(
    val minPrice: String = "",
    val maxPrice: String = "",
    val categoryId: String = "",
    val pictureRequired: Boolean = false,
    val buyNowOnly: Boolean = false,
    val adType: AdType? = null,
    val posterType: PosterType? = null,
) {
    fun activeCount(): Int {
        var n = 0
        if (minPrice.isNotBlank()) n++
        if (maxPrice.isNotBlank()) n++
        if (categoryId.isNotBlank()) n++
        if (pictureRequired) n++
        if (buyNowOnly) n++
        if (adType != null) n++
        if (posterType != null) n++
        return n
    }

    fun applyTo(base: SearchQuery): SearchQuery = base.copy(
        minPrice = minPrice.toIntOrNull(),
        maxPrice = maxPrice.toIntOrNull(),
        categoryId = categoryId.toIntOrNull(),
        pictureRequired = pictureRequired,
        buyNowOnly = buyNowOnly,
        adType = adType,
        posterType = posterType,
    )

    companion object {
        fun from(query: SearchQuery) = SearchFiltersState(
            minPrice = query.minPrice?.toString().orEmpty(),
            maxPrice = query.maxPrice?.toString().orEmpty(),
            categoryId = query.categoryId?.toString().orEmpty(),
            pictureRequired = query.pictureRequired,
            buyNowOnly = query.buyNowOnly,
            adType = query.adType,
            posterType = query.posterType,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersSheet(
    visible: Boolean,
    state: SearchFiltersState,
    onStateChange: (SearchFiltersState) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.filters_title), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.minPrice,
                    onValueChange = { onStateChange(state.copy(minPrice = it.filter { c -> c.isDigit() })) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.min_price_hint)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.maxPrice,
                    onValueChange = { onStateChange(state.copy(maxPrice = it.filter { c -> c.isDigit() })) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.max_price_hint)) },
                    singleLine = true,
                )
            }

            OutlinedTextField(
                value = state.categoryId,
                onValueChange = { onStateChange(state.copy(categoryId = it.filter { c -> c.isDigit() })) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.category_id_hint)) },
                supportingText = { Text(stringResource(R.string.category_id_help)) },
                singleLine = true,
            )

            FilterSwitch(stringResource(R.string.filter_pictures_only), state.pictureRequired) {
                onStateChange(state.copy(pictureRequired = it))
            }
            FilterSwitch(stringResource(R.string.filter_buy_now), state.buyNowOnly) {
                onStateChange(state.copy(buyNowOnly = it))
            }

            Text(stringResource(R.string.filter_ad_type))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.adType == null,
                    onClick = { onStateChange(state.copy(adType = null)) },
                    label = { Text(stringResource(R.string.filter_any)) },
                )
                FilterChip(
                    selected = state.adType == AdType.OFFER,
                    onClick = { onStateChange(state.copy(adType = AdType.OFFER)) },
                    label = { Text(stringResource(R.string.filter_offers)) },
                )
                FilterChip(
                    selected = state.adType == AdType.WANTED,
                    onClick = { onStateChange(state.copy(adType = AdType.WANTED)) },
                    label = { Text(stringResource(R.string.filter_wanted)) },
                )
            }

            Text(stringResource(R.string.filter_seller_type))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.posterType == null,
                    onClick = { onStateChange(state.copy(posterType = null)) },
                    label = { Text(stringResource(R.string.filter_any)) },
                )
                FilterChip(
                    selected = state.posterType == PosterType.PRIVATE,
                    onClick = { onStateChange(state.copy(posterType = PosterType.PRIVATE)) },
                    label = { Text(stringResource(R.string.filter_private)) },
                )
                FilterChip(
                    selected = state.posterType == PosterType.COMMERCIAL,
                    onClick = { onStateChange(state.copy(posterType = PosterType.COMMERCIAL)) },
                    label = { Text(stringResource(R.string.filter_commercial)) },
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onStateChange(SearchFiltersState()) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.filters_reset)) }
                Button(
                    onClick = {
                        onApply()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.filters_apply)) }
            }
        }
    }
}

@Composable
private fun FilterSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
