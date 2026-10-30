package dev.byfedin.offercatalog.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.byfedin.offercatalog.MainDispatcherRule
import dev.byfedin.offercatalog.data.OfferRepository
import dev.byfedin.offercatalog.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = ViewModelStore()
    private val source = ControlledRepository()

    @After
    fun close() {
        store.clear()
    }

    private fun model(saved: SavedStateHandle = SavedStateHandle()): CatalogViewModel =
        CatalogViewModel(source, saved).also { store.put("catalog", it) }

    @Test
    fun `rapid input makes only the final request after debounce`() = runTest {
        val model = model()
        runCurrent()
        model.setQuery("c")
        advanceTimeBy(100)
        model.setQuery("co")
        advanceTimeBy(100)
        model.setQuery("coffee")
        advanceTimeBy(299)
        runCurrent()
        assertEquals(listOf(""), source.calls.map { it.filter.query })
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("", "coffee"), source.calls.map { it.filter.query })
        source.calls.last().succeed(listOf(offer("coffee")), next = null)
        runCurrent()
        assertEquals("coffee", model.results().offers.single().id)
    }

    @Test
    fun `equivalent normalized queries do not reload`() = runTest {
        val model = model()
        runCurrent()
        model.setQuery("Coffee")
        advanceTimeBy(300)
        runCurrent()
        source.calls.last().succeed(listOf(offer("coffee")), null)
        runCurrent()
        model.setQuery(" coffee ")
        advanceUntilIdle()
        assertEquals(2, source.calls.size)
        assertEquals(" coffee ", model.state.value.filter.query)
    }

    @Test
    fun `obsolete request is cancelled immediately before debounce finishes`() = runTest {
        val model = model()
        runCurrent()
        model.setQuery("new")
        runCurrent()
        assertTrue(source.calls.first().cancelled)
        assertEquals(CatalogContent.Loading, model.state.value.content)
        advanceTimeBy(299)
        assertEquals(1, source.calls.size)
    }

    @Test
    fun `late success cannot overwrite a newer query`() = runTest {
        source.ignoreCancellation = true
        val model = model()
        runCurrent()
        val old = source.calls.first()
        model.setQuery("new")
        advanceTimeBy(300)
        runCurrent()
        source.calls.last().succeed(listOf(offer("new")), null)
        runCurrent()
        old.succeed(listOf(offer("old")), 8)
        runCurrent()
        assertEquals(listOf("new"), model.results().offers.map { it.id })
        assertNull(model.results().nextOffset)
    }

    @Test
    fun `late failure cannot replace newer results`() = runTest {
        source.ignoreCancellation = true
        val model = model()
        runCurrent()
        val old = source.calls.first()
        model.setCategory(Category.TRAVEL)
        runCurrent()
        source.calls.last().succeed(listOf(offer("travel")), null)
        runCurrent()
        old.fail()
        runCurrent()
        assertEquals("travel", model.results().offers.single().id)
    }

    @Test
    fun `category change bypasses debounce and cancels pending search`() = runTest {
        val model = model()
        runCurrent()
        model.setQuery("walk")
        advanceTimeBy(100)
        model.setCategory(Category.TRAVEL)
        runCurrent()
        assertEquals(2, source.calls.size)
        assertEquals(CatalogFilter("walk", Category.TRAVEL), source.calls.last().filter)
        assertTrue(source.calls.first().cancelled)
    }

    @Test
    fun `changing parameters resets pagination and rejects an old append`() = runTest {
        source.ignoreCancellation = true
        val model = model()
        runCurrent()
        source.calls.first().succeed(listOf(offer("old-first")), 8)
        runCurrent()
        model.loadMore()
        runCurrent()
        val oldAppend = source.calls.last()
        model.setCategory(Category.DIGITAL)
        runCurrent()
        assertEquals(0, source.calls.last().offset)
        source.calls.last().succeed(listOf(offer("new-first")), null)
        runCurrent()
        oldAppend.succeed(listOf(offer("old-second")), 16)
        runCurrent()
        assertEquals(listOf("new-first"), model.results().offers.map { it.id })
    }

    @Test
    fun `repeated scroll events start one append and duplicate IDs are removed`() = runTest {
        val model = model()
        runCurrent()
        source.calls.first().succeed(listOf(offer("one")), 8)
        runCurrent()
        repeat(20) { model.loadMore() }
        runCurrent()
        assertEquals(listOf(0, 8), source.calls.map { it.offset })
        source.calls.last().succeed(listOf(offer("one"), offer("two")), null)
        runCurrent()
        assertEquals(listOf("one", "two"), model.results().offers.map { it.id })
    }

    @Test
    fun `append failure preserves content and explicit retry uses same cursor`() = runTest {
        val model = model()
        runCurrent()
        source.calls.first().succeed(listOf(offer("one")), 8)
        runCurrent()
        model.loadMore()
        runCurrent()
        source.calls.last().fail()
        runCurrent()
        assertEquals(AppendState.Error, model.results().append)
        assertEquals("one", model.results().offers.single().id)
        repeat(10) { model.loadMore() }
        runCurrent()
        assertEquals(2, source.calls.size)
        repeat(10) { model.retry() }
        runCurrent()
        assertEquals(3, source.calls.size)
        assertEquals(8, source.calls.last().offset)
        source.calls.last().succeed(listOf(offer("two")), null)
        runCurrent()
        assertEquals(AppendState.Idle, model.results().append)
        assertEquals(listOf("one", "two"), model.results().offers.map { it.id })
    }

    @Test
    fun `initial failure can retry and changing filters uses current criteria`() = runTest {
        val model = model()
        runCurrent()
        source.calls.first().fail()
        runCurrent()
        assertEquals(CatalogContent.Error, model.state.value.content)
        repeat(5) { model.retry() }
        runCurrent()
        assertEquals(2, source.calls.size)
        model.setQuery("coffee")
        model.retry()
        advanceTimeBy(300)
        runCurrent()
        assertEquals(CatalogFilter("coffee"), source.calls.last().filter)
        source.calls.last().succeed(listOf(offer("coffee")), null)
        runCurrent()
        assertEquals("coffee", model.results().offers.single().id)
    }

    @Test
    fun `empty results and end of pagination cannot request more pages`() = runTest {
        val model = model()
        runCurrent()
        source.calls.first().succeed(emptyList(), null)
        runCurrent()
        model.loadMore()
        model.retry()
        runCurrent()
        assertTrue(model.results().offers.isEmpty())
        assertNull(model.results().nextOffset)
        assertEquals(1, source.calls.size)
    }

    @Test
    fun `a full terminal page does not imply another page`() = runTest {
        val model = model()
        runCurrent()
        source.calls.first().succeed((1..8).map { offer("$it") }, null)
        runCurrent()
        model.loadMore()
        runCurrent()
        assertEquals(1, source.calls.size)
    }

    @Test
    fun `saved search parameters are restored and new edits are saved`() = runTest {
        val saved =
            SavedStateHandle(mapOf("catalog.query" to "Walk", "catalog.category" to "TRAVEL"))
        val model = model(saved)
        runCurrent()
        assertEquals(CatalogFilter("walk", Category.TRAVEL), source.calls.single().filter)
        assertEquals("Walk", model.state.value.filter.query)
        model.setQuery("Garden")
        model.setCategory(Category.LEARNING)
        assertEquals("Garden", saved.get<String>("catalog.query"))
        assertEquals("LEARNING", saved.get<String>("catalog.category"))
        store.clear()
        val restored =
            model(
                SavedStateHandle(
                    mapOf(
                        "catalog.query" to saved.get<String>("catalog.query"),
                        "catalog.category" to saved.get<String>("catalog.category"),
                    )
                )
            )
        runCurrent()
        assertEquals(CatalogFilter("garden", Category.LEARNING), source.calls.last().filter)
        assertEquals("Garden", restored.state.value.filter.query)
    }

    @Test
    fun `unrecognized restored category falls back to all`() = runTest {
        val model = model(SavedStateHandle(mapOf("catalog.category" to "REMOVED")))
        runCurrent()
        assertNull(model.state.value.filter.category)
    }

    @Test
    fun `cancellation is never presented as an error`() = runTest {
        val model = model()
        runCurrent()
        source.calls.first().response.completeExceptionally(CancellationException())
        runCurrent()
        assertEquals(CatalogContent.Loading, model.state.value.content)
    }

    private fun CatalogViewModel.results() = state.value.content as CatalogContent.Results

    private class ControlledRepository : OfferRepository {
        var ignoreCancellation = false
        val calls = mutableListOf<Call>()

        override suspend fun page(filter: CatalogFilter, offset: Int): OfferPage {
            val call = Call(filter, offset)
            calls += call
            return try {
                if (ignoreCancellation) withContext(NonCancellable) { call.response.await() }
                else call.response.await()
            } catch (cancelled: CancellationException) {
                call.cancelled = true
                throw cancelled
            }
        }

        override suspend fun offer(id: String): Offer? = null
    }

    private class Call(val filter: CatalogFilter, val offset: Int) {
        val response = CompletableDeferred<OfferPage>()
        var cancelled = false

        fun succeed(offers: List<Offer>, next: Int?) {
            response.complete(OfferPage(offers, next))
        }

        fun fail() {
            response.completeExceptionally(CatalogUnavailableException())
        }
    }

    private fun offer(id: String) =
        Offer(id, id, Category.LEARNING, "Summary", "Description", "Course", "4 weeks", "Lessons")
}
