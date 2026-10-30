package dev.byfedin.offercatalog.data

import dev.byfedin.offercatalog.model.CatalogFilter
import dev.byfedin.offercatalog.model.Offer
import dev.byfedin.offercatalog.model.OfferPage

interface OfferRepository {
    suspend fun page(filter: CatalogFilter, offset: Int): OfferPage

    suspend fun offer(id: String): Offer?
}
