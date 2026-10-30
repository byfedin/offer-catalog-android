package dev.byfedin.offercatalog.data

import dev.byfedin.offercatalog.MainDispatcherRule
import dev.byfedin.offercatalog.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FixtureOfferRepositoryTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `all fixtures are reachable exactly once in a deterministic order`() = runTest {
        val repository = FixtureOfferRepository(dispatcher = main.dispatcher)
        val ids = mutableListOf<String>()
        var offset: Int? = 0
        while (offset != null) {
            val page = repository.page(CatalogFilter(), offset)
            ids += page.offers.map { it.id }
            offset = page.nextOffset
        }
        assertEquals(32, ids.size)
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(OfferFixtures.offers.map { it.id }, ids)
        assertEquals(ids.take(8), repository.page(CatalogFilter(), 0).offers.map { it.id })
    }

    @Test
    fun `search combines title matching with category before paging`() = runTest {
        val repository =
            FixtureOfferRepository(dispatcher = main.dispatcher, latencyMillis = 0, pageSize = 2)
        val page = repository.page(CatalogFilter("  THE  ", Category.TRAVEL), 0)
        assertEquals(listOf("along-the-coast", "the-mountain-notebook"), page.offers.map { it.id })
        assertEquals(2, page.nextOffset)
        val next = repository.page(CatalogFilter("THE", Category.TRAVEL), 2)
        assertEquals(listOf("gardens-of-the-city", "the-local-explorer"), next.offers.map { it.id })
        assertNull(next.nextOffset)
    }

    @Test
    fun `empty fixture list and unmatched query have no continuation`() = runTest {
        val empty = FixtureOfferRepository(offers = emptyList(), dispatcher = main.dispatcher)
        assertEquals(OfferPage(emptyList(), null), empty.page(CatalogFilter(), 0))
        val source = FixtureOfferRepository(dispatcher = main.dispatcher)
        assertEquals(OfferPage(emptyList(), null), source.page(CatalogFilter("not-an-offer"), 0))
    }

    @Test
    fun `delay is controlled by virtual time and cooperates with cancellation`() = runTest {
        val source = FixtureOfferRepository(dispatcher = main.dispatcher, latencyMillis = 450)
        val response = async { source.page(CatalogFilter(), 0) }
        advanceTimeBy(449)
        assertFalse(response.isCompleted)
        response.cancel()
        runCurrent()
        assertTrue(response.isCancelled)
    }

    @Test
    fun `details return the same fixture and an unknown ID returns null`() = runTest {
        val source = FixtureOfferRepository(dispatcher = main.dispatcher)
        assertEquals(OfferFixtures.offers.first(), source.offer("the-art-of-noticing"))
        assertNull(source.offer("missing"))
    }
}
