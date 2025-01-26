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
fun ConfirmCloseDialog(onDismiss: () -> Unit, onConfirmed: () -> Unit) {
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
        }
    )
}
