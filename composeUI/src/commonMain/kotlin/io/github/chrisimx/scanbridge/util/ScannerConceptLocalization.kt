package io.github.chrisimx.scanbridge.util

import androidx.compose.runtime.Composable
import io.github.chrisimx.anyscan.ScannerConcept
import io.github.chrisimx.anyscan.SettingValue
import org.jetbrains.compose.resources.stringResource
import scanbridge.composeui.generated.resources.*
import scanbridge.composeui.generated.resources.Res

@Composable
fun <T : SettingValue> ScannerConcept<T>.toLocalizedName(): String = stringResource(
    when (this) {
        ScannerConcept.ColorMode -> Res.string.color_mode
        ScannerConcept.ScanIntent -> Res.string.intent
        ScannerConcept.ScanRegion -> Res.string.scan_region
        ScannerConcept.ScanResolution -> Res.string.resolution_dpi
    }
)
