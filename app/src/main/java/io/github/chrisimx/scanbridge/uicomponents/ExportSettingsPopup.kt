package io.github.chrisimx.scanbridge.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    exportOptionsPopupPosition: Triple<Int, Int, Int>?,
    exportOptionsWidth: Int,
    alpha: Float,
    onDismiss: () -> Unit,
    updateWidth: (Int) -> Unit,
    onExportOcrPdf: () -> Unit,
    onExportPdf: () -> Unit,
    onExportArchive: () -> Unit
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            exportOptionsPopupPosition?.first?.minus(exportOptionsWidth / 4) ?: 0,
            exportOptionsPopupPosition?.second?.minus(exportOptionsPopupPosition.third) ?: 0
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
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExportActionButton(
                    iconRes = R.drawable.baseline_description_24,
                    label = stringResource(R.string.export_pdf_with_ocr_short),
                    contentDescription = stringResource(R.string.export_pdf_with_ocr),
                    onClick = onExportOcrPdf
                )
                ExportActionButton(
                    iconRes = R.drawable.baseline_picture_as_pdf_24,
                    label = stringResource(R.string.export_pdf_short),
                    contentDescription = stringResource(R.string.export_pdf),
                    onClick = onExportPdf
                )
                ExportActionButton(
                    iconRes = R.drawable.baseline_image_24,
                    label = stringResource(R.string.export_archive_short),
                    contentDescription = stringResource(R.string.export_as_archive),
                    onClick = onExportArchive
                )
            }
        }
    }
}

@Composable
private fun ExportActionButton(
    iconRes: Int,
    label: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                painterResource(iconRes),
                contentDescription = contentDescription
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
