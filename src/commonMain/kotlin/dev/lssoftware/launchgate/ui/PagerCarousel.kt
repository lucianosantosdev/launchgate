package dev.lssoftware.launchgate.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Identifies the advance/finish button, which has no stable label of its own to match on. */
const val CAROUSEL_ACTION_BUTTON_TAG: String = "launchgate_carousel_action"

/** Identifies the skip button. */
const val CAROUSEL_SKIP_BUTTON_TAG: String = "launchgate_carousel_skip"

/**
 * A swipeable, paged screen with dots and a single advancing button: [CarouselLabels.next] on
 * every page but the last, [CarouselLabels.finish] on the last, which calls [onFinished].
 *
 * Public because it is the reusable part underneath both [WhatsNewScreen] and [OnboardingScreen] —
 * a consumer wanting a third paged flow of its own should build it from this rather than
 * reimplementing the pager, dots and button.
 *
 * [onSkip] adds a text button in the top corner that leaves the whole flow at once, without paging
 * to the end. It needs [CarouselLabels.skip] for its label; without one, nothing is drawn. Omit it
 * — the default — for a flow with nowhere to skip to.
 */
@Composable
fun PagerCarousel(
    pageCount: Int,
    labels: CarouselLabels,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    indicator: IndicatorStyle = IndicatorStyle.default(),
    maxContentWidth: androidx.compose.ui.unit.Dp = 480.dp,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    page: @Composable (index: Int) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage >= pageCount - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = maxContentWidth)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (header != null || onSkip != null) {
            // The skip control overlays the header rather than sitting above it, so a centred
            // title stays centred and the pager loses no height to a row of its own.
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) { header?.invoke(this) }
                // Both halves are needed: a skip callback with no label would be an unreadable
                // control, and a label with no callback would do nothing.
                val skipLabel = labels.skip
                if (onSkip != null && skipLabel != null) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .testTag(CAROUSEL_SKIP_BUTTON_TAG),
                    ) {
                        Text(skipLabel)
                    }
                }
            }
        }
        // The pager takes the slack so the dots and the button stay pinned to the bottom whatever
        // a page contains — a release with six bullets must not push them off-screen.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) { index ->
            page(index)
        }
        Spacer(Modifier.height(16.dp))
        PageIndicator(pageCount = pageCount, currentPage = pagerState.currentPage, style = indicator)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (isLastPage) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag(CAROUSEL_ACTION_BUTTON_TAG),
        ) {
            Text(if (isLastPage) labels.finish else labels.next)
        }
    }
}

/** Dots under the pager: the current page's dot is wider and fully opaque. */
@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, style: IndicatorStyle) {
    // A single page has nothing to indicate, and one lone dot reads like a broken control.
    if (pageCount <= 1) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(style.spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(if (selected) style.activeWidth else style.dotSize)
            val color: Color by animateColorAsState(
                if (selected) style.activeColor else style.inactiveColor
            )
            Box(
                Modifier
                    .size(width = width, height = style.dotSize)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
