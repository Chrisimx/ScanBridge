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
import io.github.chrisimx.scanbridge.appsettings.AppSettingsRepository
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.protocol.ScanningProtocolManager
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.outline_error_24

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ScanBridgeNavHost(
    navController: NavHostController,
    startDestination: Any,
    loggerFactory: ScanBridgeLoggerFactory = koinInject()
) {
    val protocolManager = koinInject<ScanningProtocolManager>()
    val appSettingsRepo = koinInject<AppSettingsRepository>()
    val logger = loggerFactory.withTag("ScanBridgeNavHost")

    NavHost(
        modifier = Modifier.testTag("root_node"),
        navController = navController,
        startDestination = startDestination
    ) {
        composable<ErrorRoute> { backStackEntry ->
            val errorRoute: ErrorRoute = backStackEntry.toRoute()
            val errorMessage = errorRoute.error

            FullScreenError(Res.drawable.outline_error_24, errorMessage, true)
        }
        composable<StartUpScreenRoute> {
            StartupScreen(navController)
        }
        composable<CropImageRoute> { backStackEntry ->
            val scannerRoute: CropImageRoute = backStackEntry.toRoute()
            val returnRoute = try {
                Json.decodeFromString<BaseRoute>(scannerRoute.returnRoute)
            } catch (e: Exception) {
                logger.error { "Failed to decode returnRoute: ${scannerRoute.returnRoute}: $e" }
                navController.navigate(StartUpScreenRoute)
                return@composable
            }
            CropScreen(Uuid.parse(scannerRoute.scanId), returnRoute, navController)
        }
        composable<ScannerRoute> { backStackEntry ->
            val scannerRoute: ScannerRoute = backStackEntry.toRoute()
            val appSettingsCurrent = runBlocking { appSettingsRepo.getAppSettings() }
            val debug = appSettingsCurrent.writeDebugLogs
            val certValidationDisabled = appSettingsCurrent.disableCertValidation
            val timeout = appSettingsCurrent.scanningResponseTimeoutInS
            val scannerHandle = protocolManager.getScannerHandle(
                scannerRoute.protocolId,
                scannerRoute.scannerHandleString
            )
            logger.debug {
                "Navigating to scanner ${scannerRoute.scannerName} at ${scannerRoute.scannerHandleString}. Timeout is $timeout seconds, Debug is $debug. Disabling of cert checks is $certValidationDisabled. Session id is ${scannerRoute.sessionID}"
            }

            check(scannerHandle != null) {
                "Scanner handle not found for protocol ${scannerRoute.protocolId} and handle ${scannerRoute.scannerHandleString}"
            }
            ScanningScreen(
                scannerRoute.scannerName,
                scannerHandle,
                navController,
                timeout.toUInt(),
                debug,
                certValidationDisabled,
                Uuid.parse(scannerRoute.sessionID)
            )
        }
    }
}
