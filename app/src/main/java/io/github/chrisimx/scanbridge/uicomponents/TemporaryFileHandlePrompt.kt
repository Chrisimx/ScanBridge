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

package io.github.chrisimx.scanbridge.uicomponents

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import io.github.chrisimx.scanbridge.uicomponents.dialog.TemporaryFileHandlingDialog
import io.github.chrisimx.scanbridge.util.zipFiles
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Optional
import kotlin.collections.mutableListOf

fun exportFilesAsZip(files: List<File>, context: Context): Optional<File> {
    val exportDir = File(context.filesDir, "exportTempFiles")

    if (!exportDir.exists()) {
        if (!exportDir.mkdirs()) {
            Log.e("TemporaryFileHandler", "Failed to create exports directory")
            return Optional.empty<File>()
        }
    }

    val tmpZipFileName = "zipexport-${
        LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH_mm_ss_SSS"))
    }.zip"

    val zipTempFile = File(exportDir, tmpZipFileName)

    zipFiles(files, zipTempFile)
    val share = Intent(Intent.ACTION_SEND)
    share.type = "application/zip"
    share.putExtra(
        Intent.EXTRA_STREAM,
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipTempFile
        )
    )

    context.startActivity(share)

    return Optional.of(zipTempFile)
}

@Composable
fun TemporaryFileHandler() {
    val context = LocalContext.current
    var tempFileList = remember { mutableStateListOf<File>() }

    var zipExports = remember { mutableListOf<File>() }

    LaunchedEffect(true) {
        val scans = context.filesDir.listFiles()?.filter { it.name.startsWith("scan") }
        if (scans != null) {
            tempFileList.addAll(scans)
        }

        val exportDir = File(context.filesDir, "exports")

        val exports = exportDir.listFiles()?.filter { it.name.startsWith("pdfexport") }
        if (exports != null) {
            tempFileList.addAll(exports)
        }
    }

    if (tempFileList.isNotEmpty()) {
        DisposableEffect(Unit) {
            onDispose {
                zipExports.forEach { it.delete() }
            }
        }

        TemporaryFileHandlingDialog(
            onDismiss = { tempFileList.clear() },
            onDelete = {
                tempFileList.forEach { it.delete() }
                tempFileList.clear()
            },
            onExport = {
                val result = exportFilesAsZip(tempFileList, context)
                if (result.isPresent) {
                    zipExports.add(result.get())
                }
            },
            tempFileList = tempFileList
        )
    }
}
    