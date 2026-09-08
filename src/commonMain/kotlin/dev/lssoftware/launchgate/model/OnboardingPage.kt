package dev.lssoftware.launchgate.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One page of a first-launch introduction.
 *
 * [illustration] is a composable slot rather than a drawable or icon reference so the library
 * stays free of resource plumbing: pass an `Icon`, an `Image`, a Lottie view, or nothing at all.
 * It is rendered centred above [title].
 */
@Immutable
data class OnboardingPage(
    val title: String,
    val description: String,
    val illustration: (@Composable () -> Unit)? = null,
    /**
     * Interactive content under [description] — a live status line, a button that asks for a
     * permission. Rendered inside the page, so it scrolls and animates with it.
     *
     * This is what makes a gated page usable: [canAdvance] says the user may not leave yet, and
     * this slot is where they are given the means to satisfy whatever is holding them.
     */
    val action: (@Composable () -> Unit)? = null,
    /**
     * False holds the carousel here: the advance button is disabled and the pager will not scroll.
     *
     * Recomputed with the page list, so a consumer building its pages inside a composable can
     * derive this straight from live state and the gate opens on its own the moment the condition
     * is met.
     */
    val canAdvance: Boolean = true,
    /**
     * False hides the skip control while this page shows. A page that gates on something has to
     * suppress it, or the whole gate is one tap away from being bypassed.
     */
    val skippable: Boolean = true,
    /**
     * Run once this page has settled in front of the user — asking for the permission it is there
     * to explain, say. Deliberately not "when composed": the pager builds the neighbouring page
     * mid-scroll, and acting then would fire while the previous page is still on screen.
     */
    val onShown: (() -> Unit)? = null,
)
