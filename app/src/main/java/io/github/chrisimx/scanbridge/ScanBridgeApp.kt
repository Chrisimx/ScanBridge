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

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import io.github.chrisimx.scanbridge.uicomponents.CrashFileHandler
import io.github.chrisimx.scanbridge.uicomponents.TemporaryFileHandler
import io.github.chrisimx.scanbridge.util.doTempFilesExist

@Composable
fun ScanBridgeApp() {
    ScanBridgeTheme {
        val navController = rememberNavController()
        val context = LocalContext.current

        ScanBridgeNavHost(navController)

        if (doTempFilesExist(context.filesDir)) {
            TemporaryFileHandler()
        }

        CrashFileHandler()
    }
}
