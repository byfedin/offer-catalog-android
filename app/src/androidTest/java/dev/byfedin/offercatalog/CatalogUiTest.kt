package dev.byfedin.offercatalog

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun searchOpenDetailsAndReturnPreservesSearch() {
        compose.onNodeWithTag("search").performTextInput("coffee")
        Espresso.closeSoftKeyboard()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Coffee, explained").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("offer-coffee-explained").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("About this offer").fetchSemanticsNodes().isNotEmpty()
        }
        Espresso.pressBack()
        compose.onNodeWithTag("search").performScrollTo().assertTextContains("coffee")
        compose.onNodeWithText("Coffee, explained").assertExists()
    }

    @Test
    fun categoryAndQuerySurviveActivityRecreation() {
        compose.onNodeWithTag("category-TRAVEL").performClick()
        compose.onNodeWithTag("search").performTextInput("coast")
        Espresso.closeSoftKeyboard()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Along the coast").fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Along the coast").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("search").assertTextContains("coast")
        compose.onNodeWithTag("category-TRAVEL").assertIsSelected()
    }

    @Test
    fun emptySearchCanResetFilters() {
        compose.onNodeWithTag("search").performTextInput("zzzz-no-result")
        Espresso.closeSoftKeyboard()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("No matches this time").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Reset filters").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("The art of noticing").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("search").performScrollTo().assertTextContains("")
    }

    @Test
    fun returnFromDetailsKeepsScrolledPosition() {
        compose.waitUntil(5_000) {
            compose
                .onAllNodesWithTag("offer-the-art-of-noticing")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose
            .onNodeWithTag("catalog-list")
            .performScrollToNode(hasTestTag("offer-the-reading-room"))
        compose.onNodeWithTag("offer-the-reading-room").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("About this offer").fetchSemanticsNodes().isNotEmpty()
        }
        Espresso.pressBack()
        compose.onNodeWithTag("offer-the-reading-room").assertIsDisplayed()
    }
}
