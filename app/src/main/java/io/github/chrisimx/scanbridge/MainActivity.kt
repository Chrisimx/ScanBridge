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

package io.github.chrisimx.scanbridge

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (this.getSharedPreferences("scanbridge", MODE_PRIVATE)
                .getBoolean("auto_cleanup", false)
        ) {
            cleanUpScansAndExportFiles()
        }

        cleanUpCacheFiles()

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))

        setContent {
            ScanBridgeApp()
        }
    }

    private fun cleanUpCacheFiles() {
        Log.d("MainActivity", "Cleaning up cache files (used to provide data for sharing)")
        val tempFileDir = File(filesDir, "exportTempFiles")
        if (!tempFileDir.exists()) return
        if (!tempFileDir.isDirectory) {
            Log.e("MainActivity", "Temp file directory is not a directory!")
            return
        }
        File(filesDir, "exportTempFiles").listFiles()?.forEach { file ->
            file.delete()
        }
    }

    private fun cleanUpScansAndExportFiles() {
        Log.d("MainActivity", "Cleaning up scans and exports")
        filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("scan")) {
                file.delete()
            }
        }
        File(filesDir, "exports").listFiles()?.forEach { file ->
            if (file.name.startsWith("pdfexport")) {
                file.delete()
            }
        }
    }
}


