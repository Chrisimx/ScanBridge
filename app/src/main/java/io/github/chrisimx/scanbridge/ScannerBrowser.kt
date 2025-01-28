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

import android.content.Context
import android.net.nsd.NsdManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.uicomponents.FoundScannerItem
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import io.github.chrisimx.scanbridge.uicomponents.dialog.CustomScannerDialog
import java.util.Optional
import timber.log.Timber

fun startScannerDiscovery(
    context: Context,
    scannerMap: SnapshotStateMap<String, DiscoveredScanner>
): Optional<Pair<NsdManager, ScannerDiscovery>> {
    val service = getSystemService(context, NsdManager::class.java)
    if (service == null) {
        Timber.e("Couldn't get NsdManager service")
        return Optional.empty()
    }
    val listener = ScannerDiscovery(service, scannerMap)
    service.discoverServices("_uscan._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
    Timber.i("Discovery started")
    return Optional.of(Pair(service, listener))
}

@Composable
fun ScannerList(
    innerPadding: PaddingValues,
    navController: NavController,
    statefulScannerMap: SnapshotStateMap<String, DiscoveredScanner>
) {
    LazyColumn(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        reverseLayout = true
    ) {
        statefulScannerMap.forEach {
            val discoveredScanner = it.value
            discoveredScanner.addresses.forEach {
                item {
                    FoundScannerItem(discoveredScanner.name, it, navController)
                }
            }
        }
    }
}

@Composable
fun ScannerBrowser(
    innerPadding: PaddingValues,
    navController: NavController,
    showCustomDialog: Boolean,
    setShowCustomDialog: (Boolean) -> Unit,
    statefulScannerMap: SnapshotStateMap<String, DiscoveredScanner>
) {
    AnimatedContent(targetState = statefulScannerMap.isNotEmpty(), label = "ScannerList") {
        if (it) {
            ScannerList(innerPadding, navController, statefulScannerMap)
        } else {
            FullScreenError(
                R.drawable.twotone_wifi_find_24,
                stringResource(R.string.no_scanners_found)
            )
        }
    }

    if (showCustomDialog) {
        val context = LocalContext.current
        CustomScannerDialog(
            onDismiss = { setShowCustomDialog(false) },
            onConnectClicked = { url ->
                setShowCustomDialog(false)
                navController.navigate(
                    ScannerRoute(
                        context.getString(R.string.custom_scanner),
                        if (url.toString().endsWith("/")) url.toString() else "$url/"
                    )
                )
            }
        )
    }
}
