package io.github.chrisimx.scanbridge.data.model

import io.github.chrisimx.esclkt.ScanSettings
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val sessionID: String,
    val scannedPages: List<Pair<String, ScanSettings>>,
    val scanSettings: StatelessImmutableESCLScanSettingsState?,
    val tmpFiles: List<String>
)
