package dev.lssoftware.launchgate.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.runComposeUiTest
import dev.lssoftware.launchgate.model.OnboardingPage
import kotlin.test.Test
import kotlin.test.assertEquals

/** Onboarding is the same carousel with different content — including the skip control. */
@OptIn(ExperimentalTestApi::class)
class OnboardingScreenUiTest {

    /**
     * Asserts the pager came to rest on [showing] rather than stranded between it and [hidden].
     *
     * A stranded pager leaves part of both pages on screen, and `assertIsDisplayed` is true for a
     * node that is only partly visible — so the discriminating check is on the page that should
     * have gone. It may have been disposed outright once settled, which is also fine; only a
     * *visible* neighbour means stranding.
     */
    private fun ComposeUiTest.assertSettledOn(showing: String, hidden: String) {
        onNodeWithText(showing).assertIsDisplayed()
        if (onAllNodesWithText(hidden).fetchSemanticsNodes().isNotEmpty()) {
            onNodeWithText(hidden).assertIsNotDisplayed()
        }
    }

    private val pages = listOf(
        OnboardingPage(title = "First", description = "one"),
        OnboardingPage(title = "Second", description = "two"),
    )
    private val labels = CarouselLabels(next = "Next", finish = "Start", skip = "Skip")

    @Test
    fun skipLeavesTheIntroductionEarly() = runComposeUiTest {
        var skipped = 0
        setContent {
            OnboardingScreen(
                pages = pages,
                onFinished = {},
                labels = labels,
                onSkip = { skipped++ },
            )
        }

        onNodeWithText("First").assertIsDisplayed()
        onNodeWithTag(CAROUSEL_SKIP_BUTTON_TAG).performClick()
        assertEquals(1, skipped)
    }

    @Test
    fun aGatedPageHoldsTheFlowAndHidesSkip() = runComposeUiTest {
        var finished = 0
        val gated = listOf(
            OnboardingPage(
                title = "Blocked",
                description = "waiting on something",
                canAdvance = false,
                skippable = false,
            ),
        )
        setContent {
            OnboardingScreen(
                pages = gated,
                onFinished = { finished++ },
                labels = labels,
                onSkip = {},
            )
        }

        onNodeWithTag(CAROUSEL_ACTION_BUTTON_TAG).assertIsNotEnabled()
        onNodeWithTag(CAROUSEL_ACTION_BUTTON_TAG).performClick()
        assertEquals(0, finished, "a gated page must not complete the flow")
        // Skip would walk around the gate, so a page that gates has to suppress it.
        onNodeWithTag(CAROUSEL_SKIP_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun aGatedPageSwallowsForwardSwipesButNotBackwardOnes() = runComposeUiTest {
        val gated = listOf(
            OnboardingPage(title = "First", description = "one"),
            OnboardingPage(title = "Second", description = "two", canAdvance = false),
        )
        setContent { OnboardingScreen(pages = gated, onFinished = {}, labels = labels) }

        onNodeWithTag(CAROUSEL_ACTION_BUTTON_TAG).performClick()
        waitForIdle()
        onNodeWithText("Second").assertIsDisplayed()

        // Forward is where the gate bites: the disabled button must not be walked around by swiping.
        onNodeWithTag(CAROUSEL_PAGER_TAG).performTouchInput { swipeLeft() }
        waitForIdle()
        onNodeWithText("Second").assertIsDisplayed()

        // Backward is not the gate's business — re-reading an earlier page stays possible.
        onNodeWithTag(CAROUSEL_PAGER_TAG).performTouchInput { swipeRight() }
        waitForIdle()
        onNodeWithText("First").assertIsDisplayed()
    }

    @Test
    fun swipingOntoAGatedPageStillSettlesOnIt() = runComposeUiTest {
        val pages = listOf(
            OnboardingPage(title = "First", description = "one"),
            OnboardingPage(title = "Second", description = "two", canAdvance = false),
        )
        setContent { OnboardingScreen(pages = pages, onFinished = {}, labels = labels) }

        // Dragging from an open page onto a gated one must complete. Gating on the page the drag is
        // heading *to* closes the gate halfway through the gesture and eats the settling fling,
        // which strands the pager showing half of each page.
        onNodeWithTag(CAROUSEL_PAGER_TAG).performTouchInput { swipeLeft(durationMillis = 100L) }
        waitForIdle()
        assertSettledOn(showing = "Second", hidden = "First")
    }

    @Test
    fun aGatedPageIsTheLastOneTheUserCanReach() = runComposeUiTest {
        setContent {
            OnboardingScreen(
                pages = listOf(
                    OnboardingPage(title = "First", description = "one"),
                    OnboardingPage(title = "Second", description = "two", canAdvance = false),
                    OnboardingPage(title = "Third", description = "three"),
                ),
                onFinished = {},
                labels = labels,
            )
        }

        // The pager is bounded at the gate rather than fighting its scrolling, so the page beyond
        // one that gates does not exist as far as it is concerned. That is what makes stranding
        // between two pages impossible: there is no page there to be halfway to.
        onNodeWithTag(CAROUSEL_PAGER_TAG).assert(
            SemanticsMatcher("pager exposes 2 of the 3 pages") { node ->
                node.config.getOrNull(SemanticsProperties.CollectionInfo)?.columnCount == 2
            }
        )
        onNodeWithTag(CAROUSEL_ACTION_BUTTON_TAG).performClick()
        waitForIdle()
        assertSettledOn(showing = "Second", hidden = "First")

        onNodeWithTag(CAROUSEL_PAGER_TAG).performTouchInput { swipeLeft(durationMillis = 100L) }
        waitForIdle()
        assertSettledOn(showing = "Second", hidden = "Third")
    }

    @Test
    fun anOpenGateLeavesThePageAdvanceable() = runComposeUiTest {
        setContent {
            OnboardingScreen(
                pages = listOf(OnboardingPage(title = "Open", description = "ready", canAdvance = true)),
                onFinished = {},
                labels = labels,
                onSkip = {},
            )
        }
        onNodeWithTag(CAROUSEL_ACTION_BUTTON_TAG).assertIsEnabled()
        onNodeWithTag(CAROUSEL_SKIP_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun leavesAPageByItselfOnceItIsSatisfied() = runComposeUiTest {
        var satisfied by mutableStateOf(false)
        setContent {
            OnboardingScreen(
                pages = listOf(
                    OnboardingPage(title = "First", description = "one", autoAdvance = satisfied),
                    OnboardingPage(title = "Second", description = "two"),
                ),
                onFinished = {},
                labels = labels,
            )
        }

        onNodeWithText("First").assertIsDisplayed()
        satisfied = true
        waitForIdle()
        assertSettledOn(showing = "Second", hidden = "First")
    }

    @Test
    fun staysPutWhenAPageIsAlreadySatisfiedOnArrival() = runComposeUiTest {
        setContent {
            OnboardingScreen(
                pages = listOf(
                    OnboardingPage(title = "First", description = "one", autoAdvance = true),
                    OnboardingPage(title = "Second", description = "two"),
                ),
                onFinished = {},
                labels = labels,
            )
        }

        waitForIdle()
        // Otherwise paging back to re-read this page would throw the reader forward again.
        assertSettledOn(showing = "First", hidden = "Second")
    }

    @Test
    fun hasNoSkipControlWhenNoneIsOffered() = runComposeUiTest {
        setContent { OnboardingScreen(pages = pages, onFinished = {}, labels = labels) }
        onNodeWithText("First").assertIsDisplayed()
        onNodeWithTag(CAROUSEL_SKIP_BUTTON_TAG).assertDoesNotExist()
    }
}
