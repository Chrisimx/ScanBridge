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

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.protobuf.StringValue
import io.github.chrisimx.scanbridge.datastore.lastRouteStore
import io.github.chrisimx.scanbridge.datastore.shownMessagesStore
import io.github.chrisimx.scanbridge.proto.copy
import io.github.chrisimx.scanbridge.proto.lastRouteOrNull
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import io.github.chrisimx.scanbridge.uicomponents.CrashFileHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanBridgeApp() {
    ScanBridgeTheme {
        val navController = rememberNavController()
        var startDestination: Any? by remember { mutableStateOf(null) }
        val context = LocalContext.current

        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val typedRoute = currentBackStackEntry?.toTypedRoute()

        LaunchedEffect(Unit) {
            Timber.d("Loading last route from shared preferences")
            val lastRoute = context.lastRouteStore.data.firstOrNull()?.lastRouteOrNull?.value
            if (lastRoute != null) {
                try {
                    Timber.d("Last route found: $lastRoute")
                    val savedRoute = Json.decodeFromString<BaseRoute>(lastRoute)
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
            typedRoute?.let { route ->
                val json = Json.encodeToString(route)
                Timber.d("Route saved as: $json")

                context.lastRouteStore.updateData {
                    it.copy {
                        lastRoute = StringValue.of(json)
                    }
                }
            }
        }

        val playThankShouldBeShown by remember {
            context.shownMessagesStore.data.map {
                BuildConfig.FLAVOR == "play" && !it.thankPlayOne
            }
        }.collectAsState(false)

        val coroutineScope = rememberCoroutineScope()

        val markThanksMessageAsRead = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    context.shownMessagesStore.updateData {
                        it.copy {
                            thankPlayOne = true
                        }
                    }
                }
            }
        }

        if (playThankShouldBeShown) {
            AlertDialog(
                {
                    markThanksMessageAsRead()
                },
                {
                    TextButton(
                        onClick = {
                            markThanksMessageAsRead()
                        }
                    ) {
                        Text(stringResource(R.string.okay_text))
                    }
                },
                icon = {
                    Icon(
                        Icons.Rounded.Favorite,
                        "Thank you",
                        Modifier.size(60.dp),
                        tint = Color.Red
                    )
                },
                title = {
                    Text(
                        stringResource(R.string.thank_message_play_one_title),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.thank_message_play_one)
                    )
                }
            )
        }

        CrashFileHandler()
    }
}
