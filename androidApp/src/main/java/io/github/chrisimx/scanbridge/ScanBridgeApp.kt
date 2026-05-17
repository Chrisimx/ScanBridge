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
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import io.github.chrisimx.scanbridge.uicomponents.CrashFileHandler
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@Composable
fun ScanBridgeApp() {
    ScanBridgeTheme {
        val navController = rememberNavController()
        var startDestination: Any? by remember { mutableStateOf(null) }
        val coroutineScope = rememberCoroutineScope()

        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val typedRoute = currentBackStackEntry?.toTypedRoute()

        val lastRouteRepository = koinInject<LastRouteRepository>()
        val shownMessagesRepository = koinInject<ShownMessagesRepository> {
            parametersOf(coroutineScope)
        }

        val thanksForPurchaseAlreadyShown by shownMessagesRepository
            .getWasShownFlow(UserInformationMessage.THANKS_FOR_PURCHASE)
            .collectAsState(true)

        LaunchedEffect(Unit) {
            Timber.d("Loading last route from shared preferences")
            val lastRoute = lastRouteRepository.getLastRoute()
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

                lastRouteRepository.setLastRoute(json)
            }
        }

        val markThanksMessageAsRead = {
            coroutineScope.launch {
                shownMessagesRepository.setShown(UserInformationMessage.THANKS_FOR_PURCHASE, true)
            }
        }

        if (!thanksForPurchaseAlreadyShown) {
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
