package dev.byfedin.offercatalog.model

import java.util.Locale

enum class Category {
    LEARNING,
    TRAVEL,
    DIGITAL,
    WELLBEING,
}

data class Offer(
    val id: String,
    val title: String,
    val category: Category,
    val summary: String,
    val description: String,
    val format: String,
    val duration: String,
    val included: String,
)

data class CatalogFilter(val query: String = "", val category: Category? = null) {
    fun normalized() = copy(query = query.trim().lowercase(Locale.ROOT))

    val isActive: Boolean
        get() = query.isNotBlank() || category != null
}

data class OfferPage(val offers: List<Offer>, val nextOffset: Int?)

class CatalogUnavailableException : Exception("Demo catalog unavailable")
