package dev.lssoftware.launchgate

import dev.lssoftware.launchgate.model.ReleaseNote
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
}
