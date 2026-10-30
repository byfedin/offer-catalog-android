package dev.byfedin.offercatalog.catalog

import dev.byfedin.offercatalog.model.CatalogFilter
import dev.byfedin.offercatalog.model.Offer

data class CatalogState(
    val filter: CatalogFilter = CatalogFilter(),
    val content: CatalogContent = CatalogContent.Loading,
)

sealed interface CatalogContent {
    data object Loading : CatalogContent

    data object Error : CatalogContent

    data class Results(
        val offers: List<Offer>,
        val nextOffset: Int?,
        val append: AppendState = AppendState.Idle,
    ) : CatalogContent
}

enum class AppendState {
    Idle,
    Loading,
    Error,
}
