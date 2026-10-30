package dev.byfedin.offercatalog.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.byfedin.offercatalog.R
import dev.byfedin.offercatalog.ui.*

@Composable
fun DetailScreen(state: DetailState, onRetry: () -> Unit, onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        when (state) {
            DetailState.Loading -> item { LoadingPanel() }
            DetailState.Error ->
                item {
                    StatusPanel(
                        stringResource(R.string.initial_error_title),
                        stringResource(R.string.error_message),
                        stringResource(R.string.retry),
                        onRetry,
                    )
                }
            DetailState.NotFound ->
                item {
                    StatusPanel(
                        stringResource(R.string.not_found_title),
                        stringResource(R.string.not_found_message),
                        stringResource(R.string.back),
                        onBack,
                    )
                }
            is DetailState.Ready -> {
                val offer = state.offer
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(R.string.detail_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(offer.title, style = MaterialTheme.typography.headlineLarge)
                        Text(
                            offer.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(24.dp)) {
                        OfferArtwork(offer.category, Modifier.fillMaxWidth().aspectRatio(1.65f))
                    }
                }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            stringResource(offer.category.label),
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(R.string.about_offer),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(offer.description, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            DetailFact(stringResource(R.string.format_label), offer.format)
                            HorizontalDivider()
                            DetailFact(stringResource(R.string.duration_label), offer.duration)
                            HorizontalDivider()
                            DetailFact(stringResource(R.string.included_label), offer.included)
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.demo_disclosure),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.demo_details),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
