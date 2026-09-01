package dev.lssoftware.launchgate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lssoftware.launchgate.model.OnboardingPage

/**
 * The first-launch introduction: one page per [OnboardingPage], finishing on
 * [CarouselLabels.finish].
 *
 * Like [WhatsNewScreen] it knows nothing about storage or navigation — call
 * `VersionGate.markSeen()` from [onFinished] so the version the user installed is recorded and
 * neither this nor the changelog greets them again.
 *
 * @param onSkip dismisses the introduction from any page, for someone who would rather get on with
 *   it. Needs [CarouselLabels.skip] for its label. Mark the version seen here too — a user who
 *   skipped the introduction has still been offered it, and should not meet it again on the next
 *   launch.
 *
 * @param illustrationSize diameter of the tinted circle behind each page's illustration. Pass
 *   `0.dp` to drop the circle and let the illustration stand alone.
 */
@Composable
fun OnboardingScreen(
    pages: List<OnboardingPage>,
    onFinished: () -> Unit,
    labels: CarouselLabels,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    colors: OnboardingColors = OnboardingColors.default(),
    indicator: IndicatorStyle = IndicatorStyle.default(),
    illustrationSize: Dp = 96.dp,
) {
    Surface(modifier = modifier, color = colors.background) {
        PagerCarousel(
            pageCount = pages.size,
            labels = labels,
            onFinished = onFinished,
            onSkip = onSkip,
            indicator = indicator,
        ) { index ->
            OnboardingPageContent(pages[index], colors, illustrationSize)
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    colors: OnboardingColors,
    illustrationSize: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        page.illustration?.let { illustration ->
            if (illustrationSize > 0.dp) {
                Box(
                    modifier = Modifier
                        .size(illustrationSize)
                        .background(shape = CircleShape, color = colors.illustrationBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    illustration()
                }
            } else {
                illustration()
            }
            Spacer(Modifier.height(32.dp))
        }
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.title,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.body,
            textAlign = TextAlign.Center,
        )
    }
}
