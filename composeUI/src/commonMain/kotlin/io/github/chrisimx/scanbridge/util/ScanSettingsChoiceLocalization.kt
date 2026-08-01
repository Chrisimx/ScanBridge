package io.github.chrisimx.scanbridge.util

import androidx.compose.runtime.Composable
import io.github.chrisimx.anyscan.ColorMode
import io.github.chrisimx.anyscan.DiscreteResolutionValue
import io.github.chrisimx.anyscan.ScanIntent
import io.github.chrisimx.anyscan.ScannerConcept
import io.github.chrisimx.anyscan.SettingValue
import io.github.chrisimx.anyscan.StringValue
import io.github.chrisimx.anyscan.toAnyScanEnumOrRaw
import io.github.chrisimx.enumorrawcodegen.AnyScanEnumOrRaw
import org.jetbrains.compose.resources.stringResource
import scanbridge.composeui.generated.resources.*
import scanbridge.composeui.generated.resources.Res

@JvmName("localizedColorModeString")
@Composable
fun AnyScanEnumOrRaw<ColorMode>.localizedString(): String = when (this) {
    is AnyScanEnumOrRaw.Known<ColorMode> -> stringResource(
        when (this.value) {
            ColorMode.BlackAndWhite1 -> Res.string.black_and_white
            ColorMode.RGB24 -> Res.string.color_scan_24
            ColorMode.RGB48 -> Res.string.color_scan_48
            ColorMode.AutoColorDetection -> Res.string.auto_detect
            ColorMode.Grayscale8 -> Res.string.grayscale_8
            ColorMode.Grayscale16 -> Res.string.grayscale_16
        }
    )

    is AnyScanEnumOrRaw.Unknown<ColorMode> -> this.asString()
}

@JvmName("localizedScanIntentString") // Else the declaration would clash on JVM because of type erasure
@Composable
fun AnyScanEnumOrRaw<ScanIntent>.localizedString(): String = when (this) {
    is AnyScanEnumOrRaw.Known<ScanIntent> -> stringResource(
        when (this.value) {
            ScanIntent.Document -> Res.string.scan_intent_document
            ScanIntent.TextAndGraphic -> Res.string.scan_intent_text_and_graphic
            ScanIntent.Photo -> Res.string.scan_intent_photo
            ScanIntent.Preview -> Res.string.scan_intent_preview
            ScanIntent.Object -> Res.string.scan_intent_object
            ScanIntent.BusinessCard -> Res.string.scan_intent_business_card
        }
    )

    is AnyScanEnumOrRaw.Unknown<ScanIntent> -> this.asString()
}

@Composable
fun <T : SettingValue> StringValue.toLocalizedName(scannerConcept: ScannerConcept<T>): String = when (scannerConcept) {
    ScannerConcept.ColorMode -> {
        this.value
            .toAnyScanEnumOrRaw<ColorMode>()
            .localizedString()
    }

    ScannerConcept.ScanIntent -> {
        this.value
            .toAnyScanEnumOrRaw<ScanIntent>()
            .localizedString()
    }

    else -> this.toString()
}

@Composable
fun <T : SettingValue> SettingValue.toLocalizedName(scannerConcept: ScannerConcept<T>): String = when (this) {
    is DiscreteResolutionValue -> if (this.value.widthDPI == this.value.heightDPI) {
        "${this.value.widthDPI}"
    } else {
        "${this.value.widthDPI}x${this.value.heightDPI}"
    }

    is StringValue -> this.toLocalizedName(scannerConcept)

    else -> this.toString()
}
