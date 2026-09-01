package dev.lssoftware.launchgate.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lssoftware.launchgate.model.ReleaseNote
import dev.lssoftware.launchgate.model.ReleaseNotePage

/**
 * The changelog carousel, in the order given — newest release first is the sensible order, and
 * what [dev.lssoftware.launchgate.VersionGate] produces.
 *
 * A release contributes as many pages as it has [ReleaseNote.pages], so an update spanning three
 * releases of two pages each is six pages, each carrying the version it belongs to.
 *
 * Knows nothing about storage or navigation: it renders what it is handed and calls [onFinished]
 * when the user is done. Route on that callback, and call `VersionGate.markSeen()` from it.
 *
 * [releaseNotes] must not be empty, and neither must any entry's pages.
 *
 * @param title optional heading above the pages (e.g. a localized "What's New"). Omitted when null.
 */
@Composable
fun WhatsNewScreen(
    releaseNotes: List<ReleaseNote>,
    onFinished: () -> Unit,
    labels: CarouselLabels,
    modifier: Modifier = Modifier,
    title: String? = null,
    colors: WhatsNewColors = WhatsNewColors.default(),
    indicator: IndicatorStyle = IndicatorStyle.default(),
) {
    // Flattened once: the pager indexes pages, but each page has to know which release it came
    // from to label itself.
    val pages = remember(releaseNotes) {
        releaseNotes.flatMap { note -> note.pages.map { page -> note.versionName to page } }
    }
    // Provided around the whole screen so a page's own content slot can pick the colors up.
    CompositionLocalProvider(LocalWhatsNewColors provides colors) {
        Surface(modifier = modifier, color = colors.background) {
            Box(contentAlignment = Alignment.Center) {
                PagerCarousel(
                    pageCount = pages.size,
                    labels = labels,
                    onFinished = onFinished,
                    indicator = indicator,
                    header = title?.let {
                        {
                            Text(
                                text = it,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.title,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    },
                ) { index ->
                    val (versionName, page) = pages[index]
                    ReleaseNoteContent(versionName = versionName, page = page, colors = colors)
                }
            }
        }
    }
}

/**
 * The [WhatsNewColors] of the enclosing [WhatsNewScreen], so a page's own `content` can match the
 * screen it sits in without being handed anything. Falls back to [WhatsNewColors.default] outside
 * one.
 */
val LocalWhatsNewColors = compositionLocalOf<WhatsNewColors?> { null }

/**
 * The default page body: one bullet per change, coloured from the enclosing screen.
 *
 * Public so a custom [dev.lssoftware.launchgate.model.ReleaseNotePage] can put a bullet list
 * beside a screenshot rather than choosing between them.
 */
@Composable
fun ChangeList(changes: List<String>, modifier: Modifier = Modifier) {
    val colors = LocalWhatsNewColors.current ?: WhatsNewColors.default()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        changes.forEach { change ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.bullet,
                )
                Text(
                    text = change,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.body,
                )
            }
        }
    }
}

/**
 * One page: the release it belongs to, its headline, and whatever its content slot draws.
 *
 * The content scrolls within the page rather than the page growing: on a short window a page with
 * several changes is taller than the space between the header and the dots.
 */
@Composable
private fun ReleaseNoteContent(
    versionName: String,
    page: ReleaseNotePage,
    colors: WhatsNewColors,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollbar(scrollState = scrollState, thumbColor = colors.scrollbar)
            .verticalScroll(scrollState)
            // Keep the text clear of the indicator's gutter.
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(end = 12.dp),
    ) {
        Text(
            text = versionName,
            style = MaterialTheme.typography.labelLarge,
            color = colors.versionLabel,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.headline,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(20.dp))
        page.content()
    }
}

/**
 * Draws a thin scroll indicator down the right edge of a scrollable node. Compose Multiplatform's
 * own `VerticalScrollbar` is desktop-only, but this screen scrolls on every platform, so the thumb
 * is drawn by hand: its length is the visible fraction of the content, its offset the scroll
 * progress. Chain it *before* `verticalScroll` so it stays pinned to the viewport rather than
 * sliding away with the content. Nothing is drawn while everything already fits.
 */
internal fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    thumbColor: Color,
    width: Dp = 4.dp,
    minThumbHeight: Dp = 24.dp,
): Modifier = drawWithContent {
    drawContent()
    // maxValue stays Int.MAX_VALUE until the scrollable child has been measured.
    val maxScroll = scrollState.maxValue
    if (maxScroll <= 0 || maxScroll == Int.MAX_VALUE) return@drawWithContent

    val viewportHeight = size.height
    val thumbHeight = (viewportHeight * viewportHeight / (viewportHeight + maxScroll))
        .coerceAtLeast(minThumbHeight.toPx())
        .coerceAtMost(viewportHeight)
    val widthPx = width.toPx()
    val progress = scrollState.value.toFloat() / maxScroll
    drawRoundRect(
        color = thumbColor,
        topLeft = Offset(size.width - widthPx, (viewportHeight - thumbHeight) * progress),
        size = Size(widthPx, thumbHeight),
        cornerRadius = CornerRadius(widthPx / 2f),
    )
}
