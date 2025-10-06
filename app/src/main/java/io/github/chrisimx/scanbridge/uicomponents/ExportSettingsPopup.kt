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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.github.chrisimx.scanbridge.R

@Composable
fun ExportSettingsPopup(
    exportOptionsPopupPosition: Pair<Int, Int>?,
    exportOptionsWidth: Int,
    alpha: Float,
    onDismiss: () -> Unit,
    updateWidth: (Int) -> Unit,
    onExportPdf: () -> Unit,
    onExportArchive: () -> Unit,
    onPrint: () -> Unit
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            exportOptionsPopupPosition?.first?.minus(exportOptionsWidth / 4) ?: 0,
            (exportOptionsPopupPosition?.second?.minus(400)) ?: 0
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
                IconButton(onClick = { onExportPdf() }) {
                    Icon(
                        painterResource(R.drawable.baseline_picture_as_pdf_24),
                        contentDescription = stringResource(R.string.export_pdf)
                    )
                }
                IconButton(onClick = { onExportArchive() }) {
                    Icon(
                        painterResource(R.drawable.baseline_image_24),
                        contentDescription = stringResource(R.string.export_as_archive)
                    )
                }
                IconButton(onClick = { onPrint() }) {
                    Icon(
                        painterResource(R.drawable.round_print_36),
                        contentDescription = stringResource(R.string.print)
                    )
                }
            }
        }
    }
}
