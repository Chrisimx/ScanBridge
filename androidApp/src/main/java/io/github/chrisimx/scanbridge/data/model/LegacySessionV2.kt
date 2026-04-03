package io.github.chrisimx.scanbridge.data.model

import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.esclkt.getInputSourceCaps
import io.github.chrisimx.esclkt.getInputSourceOptions
import io.github.chrisimx.scanbridge.data.ui.ScanRelativeRotation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class LegacyScanMetadata(
    val filePath: String,
    val originalScanSettings: ScanSettings,
    val rotation: ScanRelativeRotation = ScanRelativeRotation.Original
)

@Serializable
data class LegacySessionV2(
    val sessionID: String,
    val scannedPages: List<LegacyScanMetadata>,
    val scanSettings: ScanSettings?,
    val tmpFiles: List<String>
) {
    companion object {
        fun fromString(sessionFileString: String, json: Json, caps: ScannerCapabilities?): Result<LegacySessionV2> = try {
            Result.success(json.decodeFromString<LegacySessionV2>(sessionFileString))
        } catch (_: Exception) {
            try {
                Timber.e("Could not decode Session at $sessionFileString. Trying with old format")
                val oldSessionVersion = json.decodeFromString<LegacySessionV1>(sessionFileString)
                Result.success(oldSessionVersion.migrateToNew(caps))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

@Serializable
data class LegacySessionV1(
    val sessionID: String,
    val scannedPages: List<Pair<String, ScanSettings>>,
    val scanSettings: StatelessImmutableESCLScanSettingsState?,
    val tmpFiles: List<String>
) {
    fun migrateToNew(caps: ScannerCapabilities?): LegacySessionV2 {
        val scannedPages = this.scannedPages.map { LegacyScanMetadata(it.first, it.second) }

        val scanSettings = if (caps != null) {
            val selectedInput = this.scanSettings?.inputSource ?: caps.getInputSourceOptions().first()
            val duplex = this.scanSettings?.duplex ?: false
            val inputSourceCaps = caps.getInputSourceCaps(selectedInput, duplex)

            this.scanSettings?.toESCLKtScanSettings(inputSourceCaps)
        } else {
            this.scanSettings?.toESCLKtScanSettings(null)
        }

        return LegacySessionV2(this.sessionID, scannedPages, scanSettings, this.tmpFiles)
    }
}
