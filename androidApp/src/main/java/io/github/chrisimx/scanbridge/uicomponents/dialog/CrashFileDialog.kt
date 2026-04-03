/*
 *     Copyright (C) 2024-2025 Christian Nagel and contributors
 *
 *     This file is part of ScanBridge.
 *
 *     ScanBridge is free software: you can redistribute it and/or modify it under the terms of
 *     the GNU General Public License as published by the Free Software Foundation, either
 *     version 3 of the License, or (at your option) any later version.
 *
 *     ScanBridge is distributed in the hope that it will be useful, but WITHOUT ANY
 *     WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *     FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along with eSCLKt.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 *     SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.chrisimx.scanbridge.uicomponents.dialog

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm_crash_log_deletion))
            }
        },
        dismissButton = {
            TextButton(
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
