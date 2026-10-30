package dev.byfedin.offercatalog.detail

import androidx.lifecycle.ViewModelStore
import dev.byfedin.offercatalog.MainDispatcherRule
import dev.byfedin.offercatalog.data.FixtureOfferRepository
import dev.byfedin.offercatalog.data.OfferRepository
import dev.byfedin.offercatalog.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = ViewModelStore()

    @After
    fun close() {
        store.clear()
    }

    @Test
    fun `unknown identifier is a distinct not found state`() = runTest {
        val model = DetailViewModel(FixtureOfferRepository(dispatcher = main.dispatcher), "missing")
        store.put("detail", model)
        assertEquals(DetailState.Loading, model.state.value)
        advanceUntilIdle()
        assertEquals(DetailState.NotFound, model.state.value)
    }

    @Test
    fun `retry recovers a failed detail request without duplicate loads`() = runTest {
        val offer =
            Offer(
                "one",
                "One",
                Category.DIGITAL,
                "Summary",
                "Description",
                "Template",
                "Ongoing",
                "Pages",
            )
        var calls = 0
        val source =
            object : OfferRepository {
                override suspend fun page(filter: CatalogFilter, offset: Int) =
                    OfferPage(emptyList(), null)

                override suspend fun offer(id: String): Offer {
                    calls++
                    if (calls == 1) throw CatalogUnavailableException()
                    return offer
                }
            }
        val model = DetailViewModel(source, "one")
        store.put("detail", model)
        runCurrent()
        assertEquals(DetailState.Error, model.state.value)
        repeat(5) { model.retry() }
        runCurrent()
        assertEquals(DetailState.Ready(offer), model.state.value)
        assertEquals(2, calls)
    }
}
