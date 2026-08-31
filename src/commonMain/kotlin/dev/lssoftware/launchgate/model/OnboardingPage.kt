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
)
