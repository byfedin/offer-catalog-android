package dev.byfedin.offercatalog

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
        compose
            .onNodeWithTag("search")
            .performScrollTo()
            .assert(
                SemanticsMatcher("Editable text is empty") {
                    it.config[SemanticsProperties.EditableText].text.isEmpty()
                }
            )
    }

    @Test
    fun returnFromDetailsKeepsScrolledPosition() {
        val catalog = compose.onNodeWithTag("catalog-list")
        val offerKey = "offer-the-reading-room"
        // Off-screen lazy items have no semantic node. Wait for their key, then scroll to them.
        compose.waitUntil(5_000) {
            catalog.fetchSemanticsNode().config[SemanticsProperties.IndexForKey](offerKey) >= 0
        }
        catalog.performScrollToKey(offerKey)
        catalog.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) {
            it(0f, 80f)
        }
        val anchor = compose.onNodeWithTag(offerKey)
        val topBefore = anchor.getUnclippedBoundsInRoot().top.value
        val scrollBefore =
            catalog.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        anchor.performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("About this offer").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Back to catalog").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("catalog-list").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        assertEquals(
            "Catalog scroll after navigation:\n${compose.onRoot().printToString()}",
            scrollBefore,
            catalog
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value(),
            0.0001f,
        )
        anchor.assertIsDisplayed()
        assertEquals(topBefore, anchor.getUnclippedBoundsInRoot().top.value, 0.5f)
    }
}
