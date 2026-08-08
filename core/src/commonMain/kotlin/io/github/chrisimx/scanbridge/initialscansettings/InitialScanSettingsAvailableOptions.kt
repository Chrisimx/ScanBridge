package io.github.chrisimx.scanbridge.initialscansettings

import io.github.chrisimx.anyscan.Area
import io.github.chrisimx.anyscan.Choice
import io.github.chrisimx.anyscan.ColorMode
import io.github.chrisimx.anyscan.CommonInputSourceCaps
import io.github.chrisimx.anyscan.CommonInputSourceType
import io.github.chrisimx.anyscan.CommonScannerCapabilities
import io.github.chrisimx.anyscan.DiscreteResolution
import io.github.chrisimx.anyscan.DiscreteResolutionValue
import io.github.chrisimx.anyscan.FileFormat
import io.github.chrisimx.anyscan.MutableScannerCapabilityMap
import io.github.chrisimx.anyscan.ScanIntent
import io.github.chrisimx.anyscan.ScanRegionValue
import io.github.chrisimx.anyscan.ScanSettingParam
import io.github.chrisimx.anyscan.ScannerCapabilityMap
import io.github.chrisimx.anyscan.ScannerConcept
import io.github.chrisimx.anyscan.StringValue
import io.github.chrisimx.anyscan.inches
import io.github.chrisimx.anyscan.millimeters
import io.github.chrisimx.enumorrawcodegen.asEnumOrRaw

/**
 * A fake scanner capability that provides options for the user when configuring their preferred initial scan settings
 */
val INITIAL_SCAN_SETTINGS_AVAILABLE_OPTIONS_CAPS = CommonScannerCapabilities(
    null,
    listOf(
        fakeInputSourceCaps(CommonInputSourceType.PLATEN),
        fakeInputSourceCaps(CommonInputSourceType.ADF_SIMPLEX),
        fakeInputSourceCaps(CommonInputSourceType.ADF_DUPLEX)
    )
)

private fun fakeInputSourceCaps(inputSourceType: CommonInputSourceType): CommonInputSourceCaps = CommonInputSourceCaps(
    listOf(FileFormat.JPEG.asEnumOrRaw(), FileFormat.PDF.asEnumOrRaw()),
    inputSourceType,
    fakeScannerCapabilitiesMap()
)

private fun fakeScannerCapabilitiesMap(): ScannerCapabilityMap {
    val scannerCapabilitiesMap = MutableScannerCapabilityMap()

    val scanRegionParam = ScanSettingParam.ScanSettingRegionParam(
        ScannerConcept.ScanRegion,
        ScanRegionValue(Area(297.millimeters(), 210.millimeters())),
        ScanRegionValue(Area(0.millimeters(), 0.millimeters())),
        ScanRegionValue(Area(200000.inches(), 200000.inches()))
    )

    val resolutionParam = ScanSettingParam.ScanSettingChoiceParam(
        ScannerConcept.ScanResolution,
        DiscreteResolutionValue(DiscreteResolution(600u, 600u)),
        listOf(
            100u,
            200u,
            300u,
            400u,
            500u,
            600u,
            1200u,
            2400u
        ).map { Choice(DiscreteResolutionValue(DiscreteResolution(it, it))) }
    )

    val colorModeParam = ScanSettingParam.ScanSettingChoiceParam(
        ScannerConcept.ColorMode,
        StringValue(ColorMode.RGB24.name),
        ColorMode.entries.map { Choice(StringValue(it.name)) }
    )

    val scanIntentParam = ScanSettingParam.ScanSettingChoiceParam(
        ScannerConcept.ScanIntent,
        StringValue(ScanIntent.Document.name),
        ScanIntent.entries.map { Choice(StringValue(it.name)) }
    )

    scannerCapabilitiesMap += scanRegionParam
    scannerCapabilitiesMap += resolutionParam
    scannerCapabilitiesMap += colorModeParam
    scannerCapabilitiesMap += scanIntentParam

    return ScannerCapabilityMap.fromMutable(scannerCapabilitiesMap)
}
