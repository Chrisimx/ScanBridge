package io.github.chrisimx.scanbridge.uicomponents.dialog

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.chrisimx.scanbridge.R

@Composable
fun ConfirmCloseDialog(
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = stringResource(R.string.warning_desc),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(text = stringResource(R.string.scans_will_be_lost_title))
        },
        text = {
            Text(text = stringResource(R.string.scans_will_be_lost_text))
        },
        onDismissRequest = {
            onDismiss()
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.cancel_text))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmed()
                }
            ) {
                Text(stringResource(R.string.leave_text))
            }
        },
    )
}