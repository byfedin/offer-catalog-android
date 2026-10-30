package dev.byfedin.offercatalog.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.byfedin.offercatalog.data.OfferRepository
import dev.byfedin.offercatalog.model.CatalogUnavailableException
import dev.byfedin.offercatalog.model.Offer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState

    data object NotFound : DetailState

    data object Error : DetailState

    data class Ready(val offer: Offer) : DetailState
}

class DetailViewModel(private val repository: OfferRepository, private val id: String) :
    ViewModel() {
    private val mutableState = MutableStateFlow<DetailState>(DetailState.Loading)
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        if (state.value == DetailState.Error) load()
    }

    private fun load() {
        mutableState.value = DetailState.Loading
        viewModelScope.launch {
            mutableState.value =
                try {
                    repository.offer(id)?.let(DetailState::Ready) ?: DetailState.NotFound
                } catch (_: CatalogUnavailableException) {
                    DetailState.Error
                }
        }
    }
}
