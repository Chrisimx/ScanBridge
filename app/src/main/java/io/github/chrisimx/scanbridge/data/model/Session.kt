package io.github.chrisimx.scanbridge.data.model

import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.esclkt.getInputSourceCaps
import io.github.chrisimx.esclkt.getInputSourceOptions
import io.github.chrisimx.scanbridge.data.ui.ScanMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class Session(
    val sessionID: String,
    val scannedPages: List<ScanMetadata>,
    val scanSettings: ScanSettings?,
    val tmpFiles: List<String>
) {
    companion object {
        fun fromString(sessionFileString: String, json: Json, caps: ScannerCapabilities?): Result<Session> = try {
            Result.success(json.decodeFromString<Session>(sessionFileString))
        } catch (_: Exception) {
            try {
                Timber.e("Could not decode Session at $sessionFileString. Trying with old format")
                val oldSessionVersion = json.decodeFromString<SessionOld>(sessionFileString)
                Result.success(oldSessionVersion.migrateToNew(caps))
            } catch (e: Exception) {
                Result.failure(e)
            }
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
    fun migrateToNew(caps: ScannerCapabilities?): Session {
        val scannedPages = this.scannedPages.map { ScanMetadata(it.first, it.second) }

        val scanSettings = if (caps != null) {
            val selectedInput = this.scanSettings?.inputSource ?: caps.getInputSourceOptions().first()
            val duplex = this.scanSettings?.duplex ?: false
            val inputSourceCaps = caps.getInputSourceCaps(selectedInput, duplex)

            this.scanSettings?.toESCLKtScanSettings(inputSourceCaps)
        } else {
            this.scanSettings?.toESCLKtScanSettings(null)
        }

        return Session(this.sessionID, scannedPages, scanSettings, this.tmpFiles)
    }
}
