package dev.lssoftware.launchgate.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.lssoftware.launchgate.model.OnboardingPage
import kotlin.test.Test
import kotlin.test.assertEquals

/** Onboarding is the same carousel with different content — including the skip control. */
@OptIn(ExperimentalTestApi::class)
class OnboardingScreenUiTest {

    private val pages = listOf(
        OnboardingPage(title = "First", description = "one"),
        OnboardingPage(title = "Second", description = "two"),
    )
    private val labels = CarouselLabels(next = "Next", finish = "Start", skip = "Close")

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
    fun hasNoSkipControlWhenNoneIsOffered() = runComposeUiTest {
        setContent { OnboardingScreen(pages = pages, onFinished = {}, labels = labels) }
        onNodeWithText("First").assertIsDisplayed()
        onNodeWithTag(CAROUSEL_SKIP_BUTTON_TAG).assertDoesNotExist()
    }
}
