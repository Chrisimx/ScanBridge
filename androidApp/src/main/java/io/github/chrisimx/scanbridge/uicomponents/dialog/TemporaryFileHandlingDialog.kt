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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.chrisimx.scanbridge.R
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun TemporaryFileHandlingDialog(tempFileList: SnapshotStateList<File>, onDismiss: () -> Unit, onDelete: () -> Unit, onExport: () -> Unit) {
    val context = LocalContext.current

    val header = context.getString(
        R.string.temporary_files_found
    )

    val content = context.getString(
        R.string.temporary_files_dialog_content,
        tempFileList.map { it.name }.joinToString("\n")
    )

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp)
            ) {
                Text(text = header, style = MaterialTheme.typography.headlineMediumEmphasized)

                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .weight(1f, false)
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                FlowRow(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { onDelete() }) {
                        Text(text = context.getString(R.string.delete))
                    }

                    TextButton(onClick = { onExport() }) {
                        Text(text = context.getString(R.string.export))
                    }

                    TextButton(onClick = { onDismiss() }) {
                        Text(text = context.getString(R.string.do_nothing))
                    }
                }
            }
        }
    }
}
