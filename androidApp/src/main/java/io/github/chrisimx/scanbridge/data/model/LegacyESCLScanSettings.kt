package io.github.chrisimx.scanbridge.data.model

import io.github.chrisimx.esclkt.BinaryRendering
import io.github.chrisimx.esclkt.CcdChannelEnumOrRaw
import io.github.chrisimx.esclkt.ColorModeEnumOrRaw
import io.github.chrisimx.esclkt.ContentTypeEnumOrRaw
import io.github.chrisimx.esclkt.FeedDirection
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.InputSourceCaps
import io.github.chrisimx.esclkt.LengthUnit
import io.github.chrisimx.esclkt.ScanIntentEnumOrRaw
import io.github.chrisimx.esclkt.ScanRegion
import io.github.chrisimx.esclkt.ScanRegions
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.millimeters
import io.github.chrisimx.scanbridge.util.toDoubleLocalized
import kotlinx.serialization.Serializable

@Serializable
data class StatelessImmutableScanRegion(
    // These are to be given in millimeters!
    val height: String,
    val width: String,
    val xOffset: String,
    val yOffset: String
) {

    fun toESCLScanRegion(selectedInputSourceCaps: InputSourceCaps): ScanRegion {
        val height: LengthUnit = when (height) {
            "max" -> selectedInputSourceCaps.maxHeight
            else -> height.toDoubleLocalized().millimeters()
        }
        val width: LengthUnit = when (width) {
            "max" -> selectedInputSourceCaps.maxWidth
            else -> width.toDoubleLocalized().millimeters()
        }

        return ScanRegion(
            height.toThreeHundredthsOfInch(),
            width.toThreeHundredthsOfInch(),
            xOffset.toDoubleLocalized().millimeters().toThreeHundredthsOfInch(),
            yOffset.toDoubleLocalized().millimeters().toThreeHundredthsOfInch()
        )
    }
}

@Serializable
data class StatelessImmutableESCLScanSettingsState(
    val version: String,
    val intent: ScanIntentEnumOrRaw?,
    val scanRegions: StatelessImmutableScanRegion?,
    val documentFormatExt: String?,
    val contentType: ContentTypeEnumOrRaw?,
    val inputSource: InputSource?,
    val xResolution: UInt,
    val yResolution: UInt,
    val colorMode: ColorModeEnumOrRaw?,
    val colorSpace: String?,
    val mediaType: String?,
    val ccdChannel: CcdChannelEnumOrRaw?,
    val binaryRendering: BinaryRendering?,
    val duplex: Boolean?,
    val numberOfPages: UInt?,
    val brightness: UInt?,
    val compressionFactor: UInt?,
    val contrast: UInt?,
    val gamma: UInt?,
    val highlight: UInt?,
    val noiseRemoval: UInt?,
    val shadow: UInt?,
    val sharpen: UInt?,
    val threshold: UInt?,
    val contextID: String?,
    val blankPageDetection: Boolean?,
    val feedDirection: FeedDirection?,
    val blankPageDetectionAndRemoval: Boolean?
) {
    fun toESCLKtScanSettings(selectedInputSourceCaps: InputSourceCaps?): ScanSettings {
        val scanRegionsESCL = if (scanRegions != null && selectedInputSourceCaps != null) {
            listOf(scanRegions.toESCLScanRegion(selectedInputSourceCaps))
        } else {
            emptyList()
        }
        return ScanSettings(
            version = version,
            intent = intent,
            scanRegions = ScanRegions(scanRegionsESCL),
            documentFormatExt = documentFormatExt,
            contentType = contentType,
            inputSource = inputSource,
            xResolution = xResolution,
            yResolution = yResolution,
            colorMode = colorMode,
            colorSpace = colorSpace,
            mediaType = mediaType,
            ccdChannel = ccdChannel,
            binaryRendering = binaryRendering,
            duplex = duplex,
            numberOfPages = numberOfPages,
            brightness = brightness,
            compressionFactor = compressionFactor,
            contrast = contrast,
            gamma = gamma,
            highlight = highlight,
            noiseRemoval = noiseRemoval,
            shadow = shadow,
            sharpen = sharpen,
            threshold = threshold,
            contextID = contextID,
            blankPageDetection = blankPageDetection,
            feedDirection = feedDirection,
            blankPageDetectionAndRemoval = blankPageDetectionAndRemoval
        )
    }
}
