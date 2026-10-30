package dev.byfedin.offercatalog.data

import dev.byfedin.offercatalog.model.CatalogFilter
import dev.byfedin.offercatalog.model.Offer
import dev.byfedin.offercatalog.model.OfferPage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class FixtureOfferRepository(
    private val offers: List<Offer> = OfferFixtures.offers,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val latencyMillis: Long = 450,
    private val pageSize: Int = 8,
) : OfferRepository {
    init {
        require(latencyMillis >= 0)
        require(pageSize > 0)
        require(offers.map { it.id }.distinct().size == offers.size)
    }

    override suspend fun page(filter: CatalogFilter, offset: Int): OfferPage =
        withContext(dispatcher) {
            require(offset >= 0)
            delay(latencyMillis)
            val criteria = filter.normalized()
            val matching =
                offers.filter {
                    ensureActive()
                    (criteria.category == null || it.category == criteria.category) &&
                        it.title.contains(criteria.query, ignoreCase = true)
                }
            val page = matching.drop(offset).take(pageSize)
            val next = (offset + page.size).takeIf { it < matching.size }
            OfferPage(page, next)
        }

    override suspend fun offer(id: String): Offer? =
        withContext(dispatcher) {
            delay(latencyMillis)
            offers.firstOrNull { it.id == id }
        }
}
