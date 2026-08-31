package dev.lssoftware.launchgate.model

import androidx.compose.runtime.Immutable

/**
 * One release's entry in a changelog.
 *
 * [title] and [changes] are plain strings, not resource handles: the library ships no copy of its
 * own, so a consumer resolves its own localized text — `stringResource` in composition, or Compose
 * Resources' suspend `getString` / Android's `Context.getString` when building the list for
 * [dev.lssoftware.launchgate.VersionGate].
 *
 * [versionCode] must be comparable against the consumer's own version code, and must not change
 * once a release has shipped — it is the only thing that decides whether a user has seen this
 * entry. Where a build's version code is not stable across rebuilds of the same release, use the
 * lowest code that release can carry.
 */
@Immutable
data class ReleaseNote(
    val versionCode: Int,
    val versionName: String,
    val title: String,
    val changes: List<String>,
)

/**
 * The releases a user on [lastSeenVersionCode] has not been told about, newest first.
 *
 * Half-open on purpose: the release they are already on is not news, the one they are updating to
 * is. Returns empty when they are up to date and — because the range is empty rather than
 * inverted — when they have moved backwards, so a downgrade or a debug build with a low version
 * code shows nothing rather than everything.
 */
fun notesBetween(
    lastSeenVersionCode: Int,
    currentVersionCode: Int,
    notes: List<ReleaseNote>,
): List<ReleaseNote> = notes
    .filter { it.versionCode > lastSeenVersionCode && it.versionCode <= currentVersionCode }
    .sortedByDescending { it.versionCode }
