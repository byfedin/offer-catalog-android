package dev.byfedin.offercatalog.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.byfedin.offercatalog.R
import dev.byfedin.offercatalog.model.Category
import dev.byfedin.offercatalog.model.Offer
import dev.byfedin.offercatalog.ui.*
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CatalogScreen(
    state: CatalogState,
    onQuery: (String) -> Unit,
    onCategory: (Category?) -> Unit,
    onReset: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOffer: (String) -> Unit,
) {
    val list = rememberLazyListState()
    val focus = LocalFocusManager.current
    val results = state.content as? CatalogContent.Results
    val currentLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(list, results?.offers?.size, results?.append) {
        snapshotFlow {
                val last = list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                last >= list.layoutInfo.totalItemsCount - 2 && last >= 0
            }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd && results?.append == AppendState.Idle) currentLoadMore()
            }
    }
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().testTag("catalog-list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.demo_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.catalog_title),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    stringResource(R.string.catalog_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "search") {
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().testTag("search"),
                label = { Text(stringResource(R.string.search_label)) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                },
                trailingIcon =
                    if (state.filter.query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onQuery("") }) {
                                Icon(
                                    painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.clear_search),
                                )
                            }
                        }
                    } else null,
            )
        }
        item(key = "categories") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("categories"),
            ) {
                item {
                    FilterChip(
                        selected = state.filter.category == null,
                        onClick = {
                            focus.clearFocus()
                            onCategory(null)
                        },
                        label = { Text(stringResource(R.string.all_categories)) },
                    )
                }
                items(Category.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = state.filter.category == category,
                        onClick = {
                            focus.clearFocus()
                            onCategory(category)
                        },
                        label = { Text(stringResource(category.label)) },
                        modifier = Modifier.testTag("category-${category.name}"),
                    )
                }
            }
        }
        when (val content = state.content) {
            CatalogContent.Loading -> item(key = "loading") { LoadingPanel() }
            CatalogContent.Error ->
                item(key = "error") {
                    StatusPanel(
                        stringResource(R.string.initial_error_title),
                        stringResource(R.string.error_message),
                        stringResource(R.string.retry),
                        onRetry,
                    )
                }
            is CatalogContent.Results -> {
                if (content.offers.isEmpty()) {
                    item(key = "empty") {
                        if (state.filter.isActive) {
                            StatusPanel(
                                stringResource(R.string.no_matches_title),
                                stringResource(R.string.no_matches_message),
                                stringResource(R.string.reset_filters),
                                onReset,
                            )
                        } else {
                            StatusPanel(
                                stringResource(R.string.empty_title),
                                stringResource(R.string.empty_message),
                            )
                        }
                    }
                } else {
                    item(key = "count") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.collection_label),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                stringResource(R.string.loaded_count, content.offers.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(content.offers, key = { "offer-${it.id}" }, contentType = { "offer" }) {
                        offer ->
                        OfferCard(offer) {
                            focus.clearFocus()
                            onOffer(offer.id)
                        }
                    }
                    item(key = "footer") {
                        when {
                            content.append == AppendState.Loading ->
                                LoadingPanel(stringResource(R.string.loading_more))
                            content.append == AppendState.Error ->
                                StatusPanel(
                                    stringResource(R.string.append_error),
                                    action = stringResource(R.string.retry),
                                    onAction = onRetry,
                                )
                            content.nextOffset == null ->
                                StatusPanel(stringResource(R.string.end_of_list))
                            else ->
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    OutlinedButton(onClick = onLoadMore) {
                                        Text(stringResource(R.string.load_more))
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(offer: Offer, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("offer-${offer.id}"),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        OfferArtwork(offer.category, Modifier.fillMaxWidth().height(132.dp))
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                stringResource(offer.category.label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(offer.title, style = MaterialTheme.typography.titleLarge)
            Text(
                offer.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${offer.format} · ${offer.duration}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
