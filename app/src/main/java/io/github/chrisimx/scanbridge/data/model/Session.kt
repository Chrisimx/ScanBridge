package io.github.chrisimx.scanbridge.data.model

import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.data.ui.ImmutableScanSettingsComposableData
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsComposableData
import io.github.chrisimx.scanbridge.data.ui.StatelessImmutableScanSettingsComposableData
import kotlinx.serialization.Serializable

@Serializable
data class Session(val sessionID: String, val scannedPages: List<Pair<String, ScanSettings>>, val scanSettings: StatelessImmutableESCLScanSettingsState?, val tmpFiles: List<String>)
