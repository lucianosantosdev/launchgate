package dev.lssoftware.launchgate.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.lssoftware.launchgate.ui.ChangeList

/**
 * One screen of a release's notes: a headline and whatever belongs under it.
 *
 * [content] is a composable slot, so a page is not limited to a bullet list — a screenshot, a
 * before/after, an animation or a link all fit. It renders inside the page's scrolling column,
 * under the version label and [title], and can read the screen's [WhatsNewColors] from
 * [dev.lssoftware.launchgate.ui.LocalWhatsNewColors] to match without being passed anything.
 *
 * For the usual bullet list, use the [ReleaseNotePage] overload taking `changes`.
 *
 * Splitting a release across several pages is a deliberate editorial act, not an automatic
 * overflow rule — a release that did three unrelated things reads better as three headlines than
 * as one page of nine bullets.
 */
@Immutable
data class ReleaseNotePage(
    val title: String,
    val content: @Composable () -> Unit,
)

/** A page whose body is the usual bullet list of [changes]. */
fun ReleaseNotePage(
    title: String,
    changes: List<String>,
): ReleaseNotePage = ReleaseNotePage(title) { ChangeList(changes) }

/**
 * One release's entry in a changelog, as one or more [pages].
 *
 * Titles and changes are plain strings, not resource handles: the library ships no copy of its
 * own, so a consumer resolves its own localized text — `stringResource` in composition, or Compose
 * Resources' suspend `getString` / Android's `Context.getString` when building the list for
 * [dev.lssoftware.launchgate.VersionGate].
 *
 * [versionCode] must be comparable against the consumer's own version code, and must not change
 * once a release has shipped — it is the only thing that decides whether a user has seen this
 * entry. Where a build's version code is not stable across rebuilds of the same release, use the
 * lowest code that release can carry.
 *
 * An entry with no pages is treated as no entry at all; see [notesBetween].
 */
@Immutable
data class ReleaseNote(
    val versionCode: Int,
    val versionName: String,
    val pages: List<ReleaseNotePage>,
)

/** A single-page release — the common case, spelled as if [ReleaseNote] took a title directly. */
fun ReleaseNote(
    versionCode: Int,
    versionName: String,
    title: String,
    changes: List<String>,
): ReleaseNote = ReleaseNote(
    versionCode = versionCode,
    versionName = versionName,
    pages = listOf(ReleaseNotePage(title, changes)),
)

/**
 * The releases a user on [lastSeenVersionCode] has not been told about, newest first.
 *
 * Half-open on purpose: the release they are already on is not news, the one they are updating to
 * is. Returns empty when they are up to date and — because the range is empty rather than
 * inverted — when they have moved backwards, so a downgrade or a debug build with a low version
 * code shows nothing rather than everything.
 *
 * Entries with no pages are dropped rather than rendered blank, and the sort is stable, so several
 * entries sharing a version code keep the order the catalog wrote them in.
 */
fun notesBetween(
    lastSeenVersionCode: Int,
    currentVersionCode: Int,
    notes: List<ReleaseNote>,
): List<ReleaseNote> = notes
    .filter {
        it.pages.isNotEmpty() &&
            it.versionCode > lastSeenVersionCode &&
            it.versionCode <= currentVersionCode
    }
    .sortedByDescending { it.versionCode }
