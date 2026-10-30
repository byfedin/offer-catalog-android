package dev.byfedin.offercatalog

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.byfedin.offercatalog.data.OfferRepository
import dev.byfedin.offercatalog.detail.DetailScreen
import dev.byfedin.offercatalog.detail.DetailState
import dev.byfedin.offercatalog.model.*
import dev.byfedin.offercatalog.ui.CatalogTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogErrorUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun initialFailureAndAppendFailureRecoverThroughVisibleActions() {
        var firstAttempts = 0
        var appendAttempts = 0
        val repository =
            object : OfferRepository {
                override suspend fun page(filter: CatalogFilter, offset: Int): OfferPage {
                    if (offset == 0) {
                        if (++firstAttempts == 1) throw CatalogUnavailableException()
                        return OfferPage(listOf(exampleOffer("First offer")), 1)
                    }
                    if (++appendAttempts == 1) throw CatalogUnavailableException()
                    return OfferPage(listOf(exampleOffer("Second offer")), null)
                }

                override suspend fun offer(id: String): Offer? = null
            }
        compose.setContent { CatalogTheme { OfferCatalogApp(repository) } }
        compose.onNodeWithText("Couldn’t load the collection").assertExists()
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        compose
            .onNodeWithTag("catalog-list")
            .performScrollToNode(
                hasText("Couldn’t load more offers. Your collection is still here.")
            )
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithTag("catalog-list").performScrollToNode(hasText("Second offer"))
        compose.onNodeWithText("Second offer").assertIsDisplayed()
        compose.onNodeWithTag("catalog-list").performScrollToNode(hasText("First offer"))
        compose.onNodeWithText("First offer").assertIsDisplayed()
    }

    @Test
    fun unknownOfferHasAnAccessibleWayBack() {
        var back = false
        compose.setContent {
            CatalogTheme { DetailScreen(DetailState.NotFound, {}, { back = true }) }
        }
        compose.onNodeWithText("Offer not found").assertIsDisplayed()
        compose.onNodeWithText("Back to catalog").performClick()
        assertTrue(back)
    }

    private fun exampleOffer(title: String) =
        Offer(
            title,
            title,
            Category.LEARNING,
            "Summary",
            "Description",
            "Course",
            "4 weeks",
            "Lessons",
        )
}
