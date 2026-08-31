package dev.lssoftware.launchgate.model

/**
 * Where a launch should go, resolved once per app start by
 * [dev.lssoftware.launchgate.VersionGate].
 *
 * The three cases are mutually exclusive by construction — one resolved value, so a launch can
 * never show both onboarding and a changelog.
 */
sealed interface StartDestination {
    /** First launch ever: introduce the app rather than announcing releases they never had. */
    data object Onboarding : StartDestination

    /** A returning user with releases to catch up on. [releaseNotes] is never empty. */
    data class WhatsNew(val releaseNotes: List<ReleaseNote>) : StartDestination

    /** Nothing to show — straight into the app. */
    data object None : StartDestination
}
