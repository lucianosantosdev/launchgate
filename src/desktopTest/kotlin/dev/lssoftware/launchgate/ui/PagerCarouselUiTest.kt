package dev.lssoftware.launchgate.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The carousel shared by What's New and onboarding: the button finishes only on the last page,
 * and advances everywhere else.
 */
@OptIn(ExperimentalTestApi::class)
class PagerCarouselUiTest {

    private val labels = CarouselLabels(next = "Next", finish = "Got it", skip = "Close")

    @Test
    fun singlePageFinishesImmediately() = runComposeUiTest {
        var finished = 0
        setContent {
            PagerCarousel(pageCount = 1, labels = labels, onFinished = { finished++ }) {
                Text("only page")
            }
        }

        onNodeWithText("only page").assertIsDisplayed()
        onNodeWithText("Got it").performClick()
        assertEquals(1, finished)
    }

    /** The skip control leaves the whole flow from the first page, without paging to the end. */
    @Test
    fun skipLeavesFromTheFirstPage() = runComposeUiTest {
        var skipped = 0
        var finished = 0
        setContent {
            PagerCarousel(
                pageCount = 3,
                labels = labels,
                onFinished = { finished++ },
                onSkip = { skipped++ },
            ) { index -> Text("page $index") }
        }

        onNodeWithText("page 0").assertIsDisplayed()
        onNodeWithTag(CAROUSEL_SKIP_BUTTON_TAG).performClick()
        assertEquals(1, skipped)
        assertEquals(0, finished, "skipping is not finishing the last page")
    }

    /** Without a skip callback there is no control to press — onboarding should be read through. */
    @Test
    fun noSkipControlByDefault() = runComposeUiTest {
        setContent {
            PagerCarousel(pageCount = 2, labels = labels, onFinished = {}) { Text("page") }
        }
        onNodeWithTag(CAROUSEL_SKIP_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun finishesOnlyOnTheLastPage() = runComposeUiTest {
        var finished = 0
        setContent {
            PagerCarousel(pageCount = 2, labels = labels, onFinished = { finished++ }) { index ->
                Text("page $index")
            }
        }

        // The first page offers Next, not the finish button, so the flow cannot be dismissed yet.
        onNodeWithText("page 0").assertIsDisplayed()
        onNodeWithText("Got it").assertDoesNotExist()

        onNodeWithTag(CAROUSEL_ACTION_BUTTON_TAG).performClick()
        waitForIdle()
        assertEquals(0, finished, "advancing a page must not dismiss the flow")

        onNodeWithText("page 1").assertIsDisplayed()
        onNodeWithText("Got it").assertIsDisplayed()
        onNodeWithTag(CAROUSEL_ACTION_BUTTON_TAG).performClick()
        assertEquals(1, finished)
    }
}
