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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.github.chrisimx.scanbridge.logs.FileLogger
import io.github.chrisimx.scanbridge.services.AndroidLocaleProvider
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class MainActivity : ComponentActivity() {
    var debugWriter: BufferedWriter? = null
    var tree: Timber.Tree? = null
    var saveDebugFileLauncher: ActivityResultLauncher<Intent>? = null

    private val localeProvider: AndroidLocaleProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        localeProvider.update()

        val sharedPreferences = this.getSharedPreferences("scanbridge", MODE_PRIVATE)

        if (sharedPreferences.getBoolean("write_debug", false)) {
            val debugDir = File(filesDir, "debug")
            if (!debugDir.exists()) {
                debugDir.mkdir()
            }
            val output = File(debugDir, "debug.txt")
            if (!output.exists()) {
                output.createNewFile()
            }
            debugWriter = BufferedWriter(FileWriter(output, true))
            tree = FileLogger(debugWriter!!)

            Timber.forest().filterIsInstance<FileLogger>().forEach {
                Timber.d("Old tree removed $it")
                it.output.close()
                Timber.uproot(it)
            }
            Timber.plant(tree!!)
        }

        Timber.i(
            "ScanBridge (${BuildConfig.VERSION_NAME}, ${BuildConfig.VERSION_CODE}, ${BuildConfig.GIT_COMMIT_HASH}, ${BuildConfig.BUILD_TYPE}) starts"
        )

        saveDebugFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                saveDebugFileTo(uri)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            cleanUpCacheFiles()
        }

        setContent {
            ScanBridgeApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.i("ScanBridge stops: onDestroy")
        Timber.i("Cleaning up debug writer")

        tree?.let { Timber.uproot(it) }
        debugWriter?.close()
    }

    private fun cleanUpCacheFiles() {
        Timber.tag("MainActivity").d("Cleaning up cache files (used to provide data for sharing)")
        val tempFileDir = File(filesDir, "exportTempFiles")
        if (!tempFileDir.exists()) return
        if (!tempFileDir.isDirectory) {
            Timber.tag("MainActivity").e("Temp file directory is not a directory!")
            return
        }
        File(filesDir, "exportTempFiles").listFiles()?.forEach { file ->
            file.delete()
        }
    }

    private fun cleanUpScansAndExportFiles() {
        Timber.d("Cleaning up scans and exports")
        filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("scan")) {
                file.delete()
            }
        }
        File(filesDir, "exports").listFiles()?.forEach { file ->
            if (file.name.startsWith("pdfexport") || file.name.startsWith("zipexport")) {
                file.delete()
            }
        }
    }

    private fun saveDebugFileTo(uri: Uri?) {
        val debugDir = File(filesDir, "debug")
        val debugFile = File(debugDir, "debug.txt")

        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                debugFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}
