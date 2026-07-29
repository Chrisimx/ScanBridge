package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.anyscan.CommonScannerCapabilities
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.PaperFormat
import io.github.chrisimx.scanbridge.loadDefaultFormats
import kotlinx.serialization.Serializable

@Serializable
data class ScanSettingsEnterableDataV0(
    val capabilities: ScannerCapabilities,
    val paperFormats: List<PaperFormat> = loadDefaultFormats(),
    val customMenuEnabled: Boolean = false,
    val widthString: String = "",
    val heightString: String = "",
    val maximumSize: Boolean = true
) {
    fun toV1(): ScanSettingsEnterableDataV1 = ScanSettingsEnterableDataV1(
        customMenuEnabled = customMenuEnabled,
        widthString = widthString,
        heightString = heightString,
        maximumSize = maximumSize
    )
}


@Serializable
data class ScanSettingsEnterableDataV1(
    val customMenuEnabled: Boolean = false,
    val widthString: String = "",
    val heightString: String = "",
    val maximumSize: Boolean = true
)
