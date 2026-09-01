package dev.lssoftware.launchgate

import dev.lssoftware.launchgate.model.ReleaseNote
import dev.lssoftware.launchgate.model.ReleaseNotePage
import dev.lssoftware.launchgate.model.notesBetween
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The gathering rule the whole gate rests on. Pure — no DataStore, no Compose. */
class NotesBetweenTest {

    private fun note(code: Int, name: String) =
        ReleaseNote(versionCode = code, versionName = name, title = name, changes = listOf(name))

    private val notes = listOf(note(300, "3.0"), note(200, "2.0"), note(100, "1.0"))

    @Test
    fun gathersEveryReleaseMissedNewestFirst() {
        val missed = notesBetween(lastSeenVersionCode = 100, currentVersionCode = 300, notes = notes)
        assertEquals(listOf("3.0", "2.0"), missed.map { it.versionName })
    }

    @Test
    fun sortsNewestFirstWhateverOrderTheCatalogIsIn() {
        val shuffled = listOf(note(100, "1.0"), note(300, "3.0"), note(200, "2.0"))
        val missed = notesBetween(0, 300, shuffled)
        assertEquals(listOf("3.0", "2.0", "1.0"), missed.map { it.versionName })
    }

    @Test
    fun excludesTheReleaseTheUserIsAlreadyOn() {
        assertTrue(notesBetween(300, 300, notes).isEmpty())
    }

    /** A rebuild of the same release carries a higher code but is not news. */
    @Test
    fun higherCodeWithNoNewEntryShowsNothing() {
        assertTrue(notesBetween(300, 350, notes).isEmpty())
    }

    /** Downgrade, reinstall of an older build, or a debug build with a low code. */
    @Test
    fun movingBackwardsShowsNothing() {
        assertTrue(notesBetween(300, 50, notes).isEmpty())
    }

    @Test
    fun skipsVersionsWithNoEntry() {
        assertTrue(notesBetween(200, 299, notes).isEmpty())
    }

    /** A release can span several pages; they travel together, in the order written. */
    @Test
    fun keepsEveryPageOfAMultiPageRelease() {
        val multiPage = ReleaseNote(
            versionCode = 200,
            versionName = "2.0",
            pages = listOf(
                ReleaseNotePage("first", listOf("a")),
                ReleaseNotePage("second", listOf("b")),
                // A page can bring its own composable instead of a bullet list.
                ReleaseNotePage("third") { },
            ),
        )
        val shown = notesBetween(100, 200, listOf(multiPage))
        assertEquals(listOf("first", "second", "third"), shown.single().pages.map { it.title })
    }

    /** An entry with no pages is no entry at all — it must not become a blank screen. */
    @Test
    fun dropsEntriesWithNoPages() {
        val empty = ReleaseNote(versionCode = 200, versionName = "2.0", pages = emptyList())
        assertTrue(notesBetween(100, 200, listOf(empty)).isEmpty())
    }
}
