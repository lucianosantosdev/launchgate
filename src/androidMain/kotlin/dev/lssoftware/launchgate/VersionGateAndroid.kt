package dev.lssoftware.launchgate

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dev.lssoftware.launchgate.model.ReleaseNote
import okio.Path.Companion.toPath

/**
 * Convenience for Android consumers with no DataStore of their own: creates (and owns) a small
 * one named [dataStoreName].
 *
 * An app that already has a Preferences DataStore should pass it to the main
 * [VersionGate.Companion.create] instead — a second store is a second file and a second write
 * queue for one integer. Hold the returned gate as a singleton: creating two DataStores over the
 * same file in one process throws.
 */
fun VersionGate.Companion.create(
    context: Context,
    currentVersionCode: Int,
    dataStoreName: String = "launch_gate",
    keyName: String = VersionGate.DEFAULT_KEY_NAME,
    releaseNotes: suspend () -> List<ReleaseNote> = { emptyList() },
    existingUserProbe: (suspend () -> Boolean)? = null,
): VersionGate {
    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath {
        context.filesDir.resolve("$dataStoreName.preferences_pb").absolutePath.toPath()
    }
    return VersionGate.create(
        dataStore = dataStore,
        currentVersionCode = currentVersionCode,
        keyName = keyName,
        releaseNotes = releaseNotes,
        existingUserProbe = existingUserProbe,
    )
}
