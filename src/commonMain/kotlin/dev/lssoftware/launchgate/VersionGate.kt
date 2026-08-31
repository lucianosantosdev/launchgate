package dev.lssoftware.launchgate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.lssoftware.launchgate.model.ReleaseNote
import dev.lssoftware.launchgate.model.StartDestination
import dev.lssoftware.launchgate.model.notesBetween
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Decides what a launch should show, and remembers what the user has already seen.
 *
 * The gate owns exactly one stored value — the version code the user was last shown — and every
 * branch of [resolve] follows from it, which is what keeps onboarding and What's New mutually
 * exclusive.
 *
 * It knows nothing about navigation, view models, or where the version code comes from. Build it
 * with [create], call [resolve] once at startup, and call [markSeen] when whichever screen it sent
 * you to is dismissed.
 */
class VersionGate internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val currentVersionCode: Int,
    private val lastSeenKey: Preferences.Key<Int>,
    private val releaseNotes: suspend () -> List<ReleaseNote>,
    private val existingUserProbe: (suspend () -> Boolean)?,
) {

    companion object {
        /** Default preferences key. Override it when two gates share one DataStore. */
        const val DEFAULT_KEY_NAME: String = "launch_gate_last_seen_version"

        /**
         * @param dataStore the consumer's own Preferences DataStore. Android callers with no
         *   DataStore of their own can use the `Context` overload instead.
         * @param currentVersionCode the running build's version code. Passed in rather than read
         *   from `BuildConfig`, which belongs to the consumer's module.
         * @param keyName the preferences key holding the last seen version. Namespace it per app
         *   (or per gate) so two users of one DataStore cannot collide.
         * @param releaseNotes the full changelog, newest or oldest first — [resolve] sorts. It is
         *   a suspend provider because resolving localized copy is itself suspending on some
         *   platforms, and because a launch should not pay for the list when nothing is shown.
         * @param existingUserProbe optional evidence that someone has used the app before, for
         *   apps adding a gate to an installed base: without it, every existing user looks brand
         *   new on the update that introduces the gate and gets onboarded. Point it at any state
         *   only a real user would have — a saved account, a paired device, a non-empty database.
         */
        fun create(
            dataStore: DataStore<Preferences>,
            currentVersionCode: Int,
            keyName: String = DEFAULT_KEY_NAME,
            releaseNotes: suspend () -> List<ReleaseNote> = { emptyList() },
            existingUserProbe: (suspend () -> Boolean)? = null,
        ): VersionGate = VersionGate(
            dataStore = dataStore,
            currentVersionCode = currentVersionCode,
            lastSeenKey = intPreferencesKey(keyName),
            releaseNotes = releaseNotes,
            existingUserProbe = existingUserProbe,
        )

        /**
         * Reads a [String] preference and reports whether it holds anything — the common shape of
         * an [existingUserProbe] when the evidence is a serialized map or id under a known key.
         */
        fun stringPreferenceProbe(
            dataStore: DataStore<Preferences>,
            keyName: String,
        ): suspend () -> Boolean = {
            !dataStore.data.first()[stringPreferencesKey(keyName)].isNullOrBlank()
        }
    }

    /**
     * Resolve where this launch should go. Safe to call from any dispatcher; it reads DataStore
     * once and does not write, except on the branches that have nothing to show — those quietly
     * bring the stored value up to date so the same non-event is not re-evaluated next launch.
     */
    suspend fun resolve(): StartDestination {
        val preferences = dataStore.data.first()
        val lastSeen = preferences[lastSeenKey]

        if (lastSeen == null) {
            // Never ran, or ran before this gate existed. The probe is what tells those apart.
            val usedBefore = existingUserProbe?.invoke() ?: false
            return if (usedBefore) {
                // We cannot know what they have already read, so say nothing and start the record.
                markSeen()
                StartDestination.None
            } else {
                StartDestination.Onboarding
            }
        }

        // Up to date, or moved backwards: a downgrade, a reinstall of an older build, or a debug
        // build whose version code sits below anything released. Nothing to announce either way.
        if (lastSeen >= currentVersionCode) {
            markSeen()
            return StartDestination.None
        }

        val missed = notesBetween(lastSeen, currentVersionCode, releaseNotes())
        if (missed.isEmpty()) {
            // Releases they missed, but none with an entry — rebuilds, fix-only builds. Skipped
            // rather than shown as blank pages.
            markSeen()
            return StartDestination.None
        }
        return StartDestination.WhatsNew(missed)
    }

    /** [resolve] as a cold single-value [Flow], for view models that expose state as a flow. */
    fun startDestination(): Flow<StartDestination> = flow { emit(resolve()) }

    /**
     * Record the running version as seen. Call this when onboarding or What's New is dismissed —
     * both end the same way, so there is one path that writes the mark, and once written it is
     * never cleared, so onboarding cannot come back.
     */
    suspend fun markSeen() {
        dataStore.edit { it[lastSeenKey] = currentVersionCode }
    }

    /** The stored version code, or null if this user has never been marked. Mostly for tests. */
    suspend fun lastSeenVersionCode(): Int? = dataStore.data.first()[lastSeenKey]
}
