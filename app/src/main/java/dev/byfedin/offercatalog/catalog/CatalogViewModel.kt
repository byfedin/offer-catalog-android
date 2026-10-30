package dev.byfedin.offercatalog.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.byfedin.offercatalog.data.OfferRepository
import dev.byfedin.offercatalog.model.CatalogFilter
import dev.byfedin.offercatalog.model.CatalogUnavailableException
import dev.byfedin.offercatalog.model.Category
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val repository: OfferRepository,
    private val savedState: SavedStateHandle,
    private val debounceMillis: Long = 300,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(
            CatalogState(
                filter =
                    CatalogFilter(
                        query = savedState[QUERY] ?: "",
                        category =
                            Category.entries.firstOrNull {
                                it.name == savedState.get<String>(CATEGORY)
                            },
                    )
            )
        )
    val state = mutableState.asStateFlow()
    private var generation = 0L
    private var request: Job? = null

    init {
        reload(debounce = false)
    }

    fun setQuery(query: String) =
        changeFilter(state.value.filter.copy(query = query), debounce = true)

    fun setCategory(category: Category?) =
        changeFilter(state.value.filter.copy(category = category), debounce = false)

    fun resetFilters() = changeFilter(CatalogFilter(), debounce = false)

    private fun changeFilter(filter: CatalogFilter, debounce: Boolean) {
        val previous = state.value.filter.normalized()
        savedState[QUERY] = filter.query
        savedState[CATEGORY] = filter.category?.name
        mutableState.value = state.value.copy(filter = filter)
        if (previous != filter.normalized()) reload(debounce)
    }

    private fun reload(debounce: Boolean) {
        // Invalidate synchronously: an old response must not win during the debounce window.
        generation++
        request?.cancel()
        val token = generation
        val filter = state.value.filter.normalized()
        mutableState.value = state.value.copy(content = CatalogContent.Loading)
        request =
            viewModelScope.launch {
                if (debounce) delay(debounceMillis)
                load(filter, offset = 0, token = token)
            }
    }

    fun loadMore() {
        val results = state.value.content as? CatalogContent.Results ?: return
        val offset = results.nextOffset ?: return
        if (results.append != AppendState.Idle) return
        append(offset, results)
    }

    fun retry() {
        when (val content = state.value.content) {
            CatalogContent.Error -> reload(debounce = false)
            is CatalogContent.Results -> {
                val offset = content.nextOffset
                if (content.append == AppendState.Error && offset != null) append(offset, content)
            }
            CatalogContent.Loading -> Unit
        }
    }

    private fun append(offset: Int, results: CatalogContent.Results) {
        // Mark busy before launching so repeated scroll events cannot enqueue the same page.
        mutableState.value = state.value.copy(content = results.copy(append = AppendState.Loading))
        val token = generation
        val filter = state.value.filter.normalized()
        request = viewModelScope.launch { load(filter, offset, token) }
    }

    private suspend fun load(filter: CatalogFilter, offset: Int, token: Long) {
        try {
            val page = repository.page(filter, offset)
            // Cancellation is cooperative. The token also rejects sources that return after
            // cancellation.
            if (token != generation) return
            val previous = (state.value.content as? CatalogContent.Results)?.offers.orEmpty()
            val offers = (previous + page.offers).distinctBy { it.id }
            mutableState.value =
                state.value.copy(content = CatalogContent.Results(offers, page.nextOffset))
        } catch (_: CatalogUnavailableException) {
            if (token != generation) return
            val content = state.value.content
            mutableState.value =
                state.value.copy(
                    content =
                        if (content is CatalogContent.Results) {
                            content.copy(append = AppendState.Error)
                        } else {
                            CatalogContent.Error
                        }
                )
        }
    }

    companion object {
        internal const val QUERY = "catalog.query"
        internal const val CATEGORY = "catalog.category"
    }
}
