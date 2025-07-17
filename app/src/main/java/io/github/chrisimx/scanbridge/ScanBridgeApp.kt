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

import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import io.github.chrisimx.scanbridge.uicomponents.CrashFileHandler
import kotlinx.serialization.json.Json
import timber.log.Timber

@Composable
fun ScanBridgeApp() {
    ScanBridgeTheme {
        val navController = rememberNavController()
        var startDestination: Any? by remember { mutableStateOf(null) }
        val context = LocalContext.current

        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val typedRoute = currentBackStackEntry?.toTypedRoute()

        val sharedPreferences = context.getSharedPreferences("route_store", MODE_PRIVATE)

        LaunchedEffect(Unit) {
            Timber.d("Loading last route from shared preferences")
            val savedJson = sharedPreferences.getString("last_route", null)
            if (savedJson != null) {
                try {
                    Timber.d("Last route found: $savedJson")
                    val savedRoute = Json.decodeFromString<BaseRoute>(savedJson)
                    startDestination = savedRoute
                    return@LaunchedEffect
                } catch (e: Exception) {
                    Timber.e(e, "Failed to deserialize saved route")
                }
            }
            startDestination = StartUpScreenRoute
        }

        if (startDestination != null) {
            ScanBridgeNavHost(navController, startDestination!!)
        }

        LaunchedEffect(typedRoute) {
            typedRoute?.let {
                val json = Json.encodeToString(it)
                Timber.d("Route saved as: $json")

                sharedPreferences.edit {
                    putString("last_route", json)
                }
            }
        }

        CrashFileHandler()
    }
}
