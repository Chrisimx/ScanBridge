package io.github.chrisimx.scanbridge.stores

import android.content.Context
import androidx.room.util.copy
import androidx.room.withTransaction
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.data.model.LegacySessionV2
import io.github.chrisimx.scanbridge.data.model.LegacySessionV2.Companion.fromString
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.db.entities.Session
import io.github.chrisimx.scanbridge.db.entities.TempFile
import io.github.chrisimx.scanbridge.proto.copy
import io.github.chrisimx.scanbridge.util.ScanSettingsJson
import java.io.File
import java.nio.file.Files
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

object LegacySessionsStore {

    @OptIn(ExperimentalAtomicApi::class)
    private val migrationStarted = AtomicBoolean(false)

    @Deprecated("Replaced with Room database")
    fun loadSession(application: Context, sessionID: String, caps: ScannerCapabilities?): Result<LegacySessionV2?> {
        Timber.d("Loading session $sessionID")

        val path = application.applicationInfo.dataDir + "/files/" + sessionID + ".session"
        val file = File(path)

        if (!file.exists()) {
            Timber.d("Could not find session file at $path")
            return Result.success(null)
        }

        val sessionFileString = file.readText()

        return fromString(sessionFileString, ScanSettingsJson.json, caps)
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun ScanBridgeDb.migrateLegacySessions(context: Context): ScanBridgeDb {
        if (!migrationStarted.compareAndSet(expectedValue = false, newValue = true)) {
            return this
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val migrationDone =
                    context.appSettingsStore.data.firstOrNull()?.migratedSessionFiles ?: false

                if (migrationDone) {
                    Timber.i("Migrating legacy sessions already done. Skipping!")
                    return@launch
                }

                Timber.i("Migrating legacy sessions")

                val sessionDir = Path(context.applicationInfo.dataDir, "files")

                val db = this@migrateLegacySessions

                val sessionIds = sessionDir.toFile()
                    .listFiles()
                    ?.asSequence()
                    ?.filter { it.isFile && it.extension == "session" }
                    ?.map { it.nameWithoutExtension }
                    ?.filter { it.isNotBlank() }
                    ?.toList() ?: emptyList()

                sessionIds.forEach { sessionIdString ->
                    val legacySession =
                        loadSession(context, sessionIdString, null).getOrNull() ?: return@forEach

                    val sessionId = Uuid.parse(legacySession.sessionID)

                    db.withTransaction {
                        sessionDao().insertAll(
                            Session(sessionId, legacySession.scanSettings, null)
                        )

                        tmpFileDao().insertAllList(
                            legacySession.tmpFiles.map {
                                TempFile(ownerSessionId = sessionId, path = it)
                            }
                        )

                        scannedPageDao().insertAllList(
                            legacySession.scannedPages.mapIndexed { index, page ->
                                ScannedPage(
                                    scanId = Uuid.generateV4(),
                                    ownerSessionId = sessionId,
                                    filePath = page.filePath,
                                    originalScanSettings = page.originalScanSettings,
                                    rotation = page.rotation,
                                    orderIndex = index
                                )
                            }
                        )
                    }

                    Files.deleteIfExists(sessionDir.resolve("$sessionIdString.session"))
                }

                context.appSettingsStore.updateData {
                    it.copy { migratedSessionFiles = true }
                }
            } catch (e: Exception) {
                Timber.e(e, "Legacy migration failed")
            } finally {
                migrationStarted.store(false)
            }
        }

        return this
    }
}
