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

import android.app.Application
import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import io.github.chrisimx.scanbridge.proto.scanningResponseTimeoutOrNull
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import io.github.chrisimx.scanbridge.uicomponents.TemporaryFileHandler
import io.github.chrisimx.scanbridge.util.doTempFilesExist
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
sealed interface BaseRoute

@Serializable
@SerialName("StartUpScreenRoute")
object StartUpScreenRoute : BaseRoute

@Serializable
@SerialName("ScannerRoute")
data class ScannerRoute(val scannerName: String, val scannerURL: String, val sessionID: String) : BaseRoute

@Serializable
@SerialName("CropImageRoute")
data class CropImageRoute(val sessionID: String, val pageIdx: Int, val returnRoute: String) : BaseRoute

@Serializable
@SerialName("ErrorRoute")
data class ErrorRoute(val error: String) : BaseRoute

fun NavBackStackEntry.toTypedRoute(): BaseRoute? {
    Timber.d("Route changed to: ${destination.route}")
    return when (destination.route) {
        "StartUpScreenRoute" -> StartUpScreenRoute

        "CropImageRoute/{sessionID}/{pageIdx}/{returnRoute}" -> {
            val sessionID = arguments?.getString("sessionID") ?: return null
            val pageIdx = arguments?.getInt("pageIdx") ?: return null
            val returnRouteString = arguments?.getString("returnRoute") ?: return null
            CropImageRoute(sessionID, pageIdx, returnRouteString)
        }

        "ScannerRoute/{scannerName}/{scannerURL}/{sessionID}" -> {
            val scannerName = arguments?.getString("scannerName") ?: return null
            val scannerURL = arguments?.getString("scannerURL") ?: return null
            val sessionID = arguments?.getString("sessionID") ?: return null
            ScannerRoute(scannerName, scannerURL, sessionID)
        }

        "ErrorRoute/{error}" -> {
            val error = arguments?.getString("error") ?: return null
            ErrorRoute(error)
        }

        else -> null
    }
}

@Composable
fun ScanBridgeNavHost(navController: NavHostController, startDestination: Any) {
    val context = LocalContext.current
    val appSettings by context.appSettingsStore.data.collectAsState(ScanBridgeSettings.getDefaultInstance())

    NavHost(
        modifier = Modifier.testTag("root_node"),
        navController = navController,
        startDestination = startDestination
    ) {
        composable<ErrorRoute> { backStackEntry ->
            val errorRoute: ErrorRoute = backStackEntry.toRoute()
            val errorMessage = errorRoute.error

            FullScreenError(R.drawable.outline_error_24, errorMessage, true)
        }
        composable<StartUpScreenRoute> {
            StartupScreen(navController)

            if (doTempFilesExist(context.filesDir)) {
                TemporaryFileHandler()
            }
        }
        composable<CropImageRoute> { backStackEntry ->
            val scannerRoute: CropImageRoute = backStackEntry.toRoute()
            val returnRoute = try {
                Json.decodeFromString<BaseRoute>(scannerRoute.returnRoute)
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode returnRoute: ${scannerRoute.returnRoute}")
                navController.navigate(StartUpScreenRoute)
                return@composable
            }
            CropScreen(scannerRoute.sessionID, scannerRoute.pageIdx, returnRoute, navController)
        }
        composable<ScannerRoute> { backStackEntry ->
            val scannerRoute: ScannerRoute = backStackEntry.toRoute()
            val debug = appSettings.writeDebug
            val certValidationDisabled = appSettings.disableCertChecks
            val timeout = appSettings.scanningResponseTimeoutOrNull?.value?.toUInt() ?: 25u
            Timber.tag("ScanBridgeNavHost")
                .d(
                    "Navigating to scanner ${scannerRoute.scannerName} at ${scannerRoute.scannerURL}. Timeout is $timeout seconds, Debug is $debug. Disabling of cert checks is $certValidationDisabled. Session id is ${scannerRoute.sessionID}"
                )
            ScanningScreen(
                scannerRoute.scannerName,
                Url(scannerRoute.scannerURL),
                navController,
                timeout,
                debug,
                certValidationDisabled,
                scannerRoute.sessionID,
                context.applicationContext as Application
            )
        }
    }
}
