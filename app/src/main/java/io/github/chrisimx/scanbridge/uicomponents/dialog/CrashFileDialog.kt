package io.github.chrisimx.scanbridge.uicomponents.dialog

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.chrisimx.scanbridge.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CrashFileDialog(crash: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.crash_log_file_exists)) },
        text = {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Text(
                    stringResource(R.string.crash_log_was_found),
                    modifier = Modifier.padding(bottom = 14.dp),
                    style = MaterialTheme.typography.bodySmallEmphasized
                )
                Text(
                    crash,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm_crash_log_deletion))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    val clip = ClipData.newPlainText("Crash log", crash)
                    clipboard.setPrimaryClip(clip)
                }
            ) {
                Text(stringResource(R.string.copy))
            }
        }
    )
}