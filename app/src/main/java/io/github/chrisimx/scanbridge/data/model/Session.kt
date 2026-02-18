package io.github.chrisimx.scanbridge.data.model

import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.data.ui.ScanMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Session(
    val sessionID: String,
    val scannedPages: List<ScanMetadata>,
    val scanSettings: StatelessImmutableESCLScanSettingsState?,
    val tmpFiles: List<String>
) {
    companion object {
        fun fromString(sessionFileString: String, json: Json): Session = try {
            json.decodeFromString<Session>(sessionFileString)
        } catch (_: Exception) {
            val oldSessionVersion = json.decodeFromString<SessionOld>(sessionFileString)
            oldSessionVersion.migrateToNew()
        }
    }
}

@Serializable
data class SessionOld(
    val sessionID: String,
    val scannedPages: List<Pair<String, ScanSettings>>,
    val scanSettings: StatelessImmutableESCLScanSettingsState?,
    val tmpFiles: List<String>
) {
    fun migrateToNew(): Session {
        val scannedPages = this.scannedPages.map { ScanMetadata(it.first, it.second) }
        return Session(this.sessionID, scannedPages, this.scanSettings, this.tmpFiles)
    }
}
