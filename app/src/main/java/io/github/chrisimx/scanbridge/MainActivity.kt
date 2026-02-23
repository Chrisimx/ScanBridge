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
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.logs.FileLogger
import io.github.chrisimx.scanbridge.services.AndroidLocaleProvider
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityScope
import timber.log.Timber
import org.koin.core.scope.Scope

class MainActivity : ComponentActivity(), AndroidScopeComponent {
    private val localeProvider: AndroidLocaleProvider by inject()

    override val scope: Scope by activityScope()

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // if permission was denied, the scan job executor can still run. Only the notification won't be visible
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this.applicationContext))
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        localeProvider.update()

        Timber.i(
            "ScanBridge (${BuildConfig.VERSION_NAME}, ${BuildConfig.VERSION_CODE}, ${BuildConfig.GIT_COMMIT_HASH}, ${BuildConfig.BUILD_TYPE}) starts"
        )

        CoroutineScope(Dispatchers.IO).launch {
            cleanUpCacheFiles()
        }

        setContent {
            ScanBridgeApp()
        }

        checkAndRequestNotificationPermission()
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

    /**
     * Check for notification permission before starting the service so that the notification is visible
     */
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )) {
                android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    // permission already granted
                }

                else -> {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
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
}
