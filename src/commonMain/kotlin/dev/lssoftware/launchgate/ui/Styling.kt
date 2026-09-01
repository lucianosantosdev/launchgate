package dev.lssoftware.launchgate.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The two button labels a carousel needs. Required, with no defaults, so the library never ships
 * English copy a consumer would have to override — or worse, silently show untranslated.
 */
@Immutable
data class CarouselLabels(
    /** Advances a page. Shown on every page but the last. */
    val next: String,
    /** Completes the flow. Shown on the last page only. */
    val finish: String,
    /**
     * Accessibility label for the skip button, which shows an icon rather than text. Required
     * whenever a skip callback is supplied — an unlabelled control is invisible to a screen reader.
     */
    val skip: String? = null,
)

/** Page-indicator appearance. The current page's dot is [activeWidth] wide instead of [dotSize]. */
@Immutable
data class IndicatorStyle(
    val activeColor: Color,
    val inactiveColor: Color,
    val dotSize: Dp = 8.dp,
    val activeWidth: Dp = 24.dp,
    val spacing: Dp = 8.dp,
) {
    companion object {
        @Composable
        fun default(): IndicatorStyle = IndicatorStyle(
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        )
    }
}

/**
 * Colors for [WhatsNewScreen]. Every default is a [MaterialTheme] role, so an app that themes
 * normally passes nothing and still matches its own branding.
 */
@Immutable
data class WhatsNewColors(
    val background: Color,
    val title: Color,
    val versionLabel: Color,
    val headline: Color,
    val body: Color,
    val bullet: Color,
    val scrollbar: Color,
) {
    companion object {
        @Composable
        fun default(): WhatsNewColors = WhatsNewColors(
            background = MaterialTheme.colorScheme.surface,
            title = MaterialTheme.colorScheme.onSurface,
            versionLabel = MaterialTheme.colorScheme.primary,
            headline = MaterialTheme.colorScheme.onSurface,
            body = MaterialTheme.colorScheme.onSurface,
            bullet = MaterialTheme.colorScheme.primary,
            scrollbar = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

/** Colors for [OnboardingScreen]. See [WhatsNewColors] for the defaulting rule. */
@Immutable
data class OnboardingColors(
    val background: Color,
    val illustrationBackground: Color,
    val title: Color,
    val body: Color,
) {
    companion object {
        @Composable
        fun default(): OnboardingColors = OnboardingColors(
            background = MaterialTheme.colorScheme.surface,
            illustrationBackground = MaterialTheme.colorScheme.primaryContainer,
            title = MaterialTheme.colorScheme.onSurface,
            body = MaterialTheme.colorScheme.onSurface,
        )
    }
}
