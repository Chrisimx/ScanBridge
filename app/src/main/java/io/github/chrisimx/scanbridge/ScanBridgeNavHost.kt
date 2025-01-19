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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl

@Serializable
object StartUpScreenRoute

@Serializable
data class ScannerRoute(val scannerName: String, val scannerURL: String)

@Composable
fun ScanBridgeNavHost(navController: NavHostController) {
    NavHost(
        modifier = Modifier.testTag("root_node"),
        navController = navController,
        startDestination = StartUpScreenRoute
    ) {
        composable<StartUpScreenRoute> { StartupScreen(navController) }
        composable<ScannerRoute> { backStackEntry ->
            val scannerRoute: ScannerRoute = backStackEntry.toRoute()
            ScanningScreen(
                scannerRoute.scannerName,
                scannerRoute.scannerURL.toHttpUrl(),
                navController
            )
        }
    }
}