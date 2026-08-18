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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.savelastroute.LastRouteRepository
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import io.github.chrisimx.scanbridge.uicomponents.StartupMessagesDisplay
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

@Composable
fun ScanBridgeApp(
    loggerFactory: ScanBridgeLoggerFactory = koinInject()
) {
    ScanBridgeTheme {
        val navController = rememberNavController()
        var startDestination: Any? by remember { mutableStateOf(null) }
        val logger = loggerFactory.withTag("ScanBridgeApp")

        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val typedRoute = currentBackStackEntry?.toTypedRoute()

        val lastRouteRepository = koinInject<LastRouteRepository>()

        LaunchedEffect(Unit) {
            logger.debug { "Loading last route from shared preferences" }
            val lastRoute = lastRouteRepository.getLastRoute()
            if (lastRoute != null) {
                try {
                    logger.debug { "Last route found: $lastRoute" }
                    val savedRoute = Json.decodeFromString<BaseRoute>(lastRoute)
                    startDestination = savedRoute
                    return@LaunchedEffect
                } catch (e: Exception) {
                    logger.error { "Failed to deserialize saved route: $e" }
                }
            }
            startDestination = StartUpScreenRoute
        }

        if (startDestination != null) {
            ScanBridgeNavHost(navController, startDestination!!)
        }

        LaunchedEffect(typedRoute) {
            typedRoute?.let { route ->
                val json = Json.encodeToString(route)
                logger.debug { "Route saved as: $json" }

                lastRouteRepository.setLastRoute(json)
            }
        }

        StartupMessagesDisplay()
    }
}
