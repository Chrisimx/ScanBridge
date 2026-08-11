package io.github.chrisimx.scanbridge.uicomponents

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.github.chrisimx.scanbridge.export.ExportModuleType
import io.github.chrisimx.scanbridge.model.PositionAndHeight
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.baseline_image_24
import scanbridge.composeui.generated.resources.baseline_picture_as_pdf_24
import scanbridge.composeui.generated.resources.export_as_archive
import scanbridge.composeui.generated.resources.export_pdf

@Composable
fun ExportSettingsPopup(
    exportOptionsPopupPosition: PositionAndHeight<Int>?,
    exportOptionsWidth: Int,
    alpha: Float,
    onDismiss: () -> Unit,
    updateWidth: (Int) -> Unit,
    availableExportModuleTypes: List<ExportModuleType>,
    onExportModuleSelected: (ExportModuleType) -> Unit,
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            exportOptionsPopupPosition?.x?.minus(exportOptionsWidth / 4) ?: 0,
            exportOptionsPopupPosition?.y?.minus(exportOptionsPopupPosition.height) ?: 0
        ),
        onDismissRequest = { onDismiss() }
    ) {
        ElevatedCard(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    updateWidth(coordinates.size.width)
                }
                .graphicsLayer {
                    this.alpha = alpha
                },
            shape = RoundedCornerShape(30.dp)
        ) {
            Row {
                availableExportModuleTypes.forEach { module ->
                    val moduleUIInfo = module.uiInformation()

                    IconButton(onClick = { onExportModuleSelected(module) }) {
                        Icon(
                            painterResource(moduleUIInfo.icon),
                            contentDescription = moduleUIInfo.name
                        )
                    }
                }
            }
        }
    }
}

data class ExportModuleUIInformation(
    val name: String,
    val icon: DrawableResource,
)

@Composable
fun ExportModuleType.uiInformation(): ExportModuleUIInformation {
    return when (this) {
        ExportModuleType.PDF -> ExportModuleUIInformation(
            name = stringResource(Res.string.export_pdf),
            icon = Res.drawable.baseline_picture_as_pdf_24
        )
        ExportModuleType.ZIP -> ExportModuleUIInformation(
            name = stringResource(Res.string.export_as_archive),
            icon = Res.drawable.baseline_image_24
        )
    }
}
