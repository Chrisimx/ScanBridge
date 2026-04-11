package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.PaperFormat
import io.github.chrisimx.scanbridge.loadDefaultFormats
import kotlinx.serialization.Serializable


@Serializable
data class ScanSettingsEnterableData(
    val capabilities: ScannerCapabilities,
    val paperFormats: List<PaperFormat> = loadDefaultFormats(),
    val customMenuEnabled: Boolean = false,
    val widthString: String = "",
    val heightString: String = "",
    val maximumSize: Boolean = true
)
