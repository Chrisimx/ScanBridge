package io.github.chrisimx.scanbridge.localizationhelper

import androidx.compose.runtime.Composable
import io.github.chrisimx.scanbridge.model.UIInputSourceType
import org.jetbrains.compose.resources.stringResource
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.adf
import scanbridge.composeui.generated.resources.platen

@Composable
fun UIInputSourceType.toReadableString(): String = when (this) {
    UIInputSourceType.PLATEN ->
        stringResource(Res.string.platen)

    UIInputSourceType.ADF -> stringResource(Res.string.adf)
}
