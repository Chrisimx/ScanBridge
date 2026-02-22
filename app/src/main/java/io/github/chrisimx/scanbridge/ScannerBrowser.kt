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
import android.content.Context
import android.net.nsd.NsdManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.data.ui.CustomScannerViewModel
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import io.github.chrisimx.scanbridge.uicomponents.FoundScannerItem
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import io.github.chrisimx.scanbridge.uicomponents.dialog.CustomScannerDialog
import io.ktor.http.Url
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

fun startScannerDiscovery(
    context: Context,
    scannerMap: SnapshotStateMap<String, DiscoveredScanner>,
    scannerMapSecure: SnapshotStateMap<String, DiscoveredScanner>
): Optional<Pair<NsdManager, Array<ScannerDiscovery>>> {
    val service = getSystemService(context, NsdManager::class.java)
    if (service == null) {
        Timber.e("Couldn't get NsdManager service")
        return Optional.empty()
    }
    val listener = ScannerDiscovery(service, isSecure = false, scannerMap)
    val listenerSecure = ScannerDiscovery(service, isSecure = true, scannerMapSecure)
    service.discoverServices("_uscan._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
    service.discoverServices("_uscans._tcp", NsdManager.PROTOCOL_DNS_SD, listenerSecure)
    Timber.i("Discovery started")
    return Optional.of(Pair(service, arrayOf(listener, listenerSecure)))
}

@Composable
fun ScannerList(
    innerPadding: PaddingValues,
    navController: NavController,
    statefulScannerMap: SnapshotStateMap<String, DiscoveredScanner>,
    statefulScannerMapSecure: SnapshotStateMap<String, DiscoveredScanner>,
    customScannerViewModel: CustomScannerViewModel
) {
    val customScanners by customScannerViewModel.customScanners.collectAsState()

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

        statefulScannerMapSecure.forEach {
            val discoveredScanner = it.value
            discoveredScanner.addresses.forEach {
                item {
                    FoundScannerItem(discoveredScanner.name, it, navController)
                }
            }
        }

        if (customScanners.isNotEmpty() && statefulScannerMap.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.discovered_scanners),
                    modifier = Modifier.fillMaxWidth(1f).padding(start = 16.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp))
            }
        }

        customScanners.forEach { customScanner ->
            item {
                FoundScannerItem(
                    customScanner.name,
                    customScanner.url.toString(),
                    navController,
                    {
                        customScannerViewModel.deleteScanner(customScanner)
                    }
                )
            }
        }

        if (customScanners.isNotEmpty() && statefulScannerMap.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.saved_scanners),
                    modifier = Modifier.fillMaxWidth(1f).padding(start = 16.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ScannerBrowser(
    innerPadding: PaddingValues,
    navController: NavController,
    showCustomDialog: Boolean,
    setShowCustomDialog: (Boolean) -> Unit,
    statefulScannerMap: SnapshotStateMap<String, DiscoveredScanner>,
    statefulScannerMapSecure: SnapshotStateMap<String, DiscoveredScanner>
) {
    val customScannerViewModel: CustomScannerViewModel = koinViewModel()
    val customScanners by customScannerViewModel.customScanners.collectAsState()

    AnimatedContent(
        targetState = statefulScannerMap.isNotEmpty() || customScanners.isNotEmpty(),
        label = "ScannerList"
    ) {
        if (it) {
            ScannerList(innerPadding, navController, statefulScannerMap, statefulScannerMapSecure, customScannerViewModel)
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
            onConnectClicked = { name, url, save ->
                val name = name.ifEmpty { context.getString(R.string.custom_scanner) }
                val url = if (url.toString().endsWith("/")) url.toString() else "$url/"
                val sessionID = Uuid.random().toString()
                if (save) {
                    customScannerViewModel.addScanner(CustomScanner(Uuid.random(), name, Url(url)))
                }
                setShowCustomDialog(false)
                navController.navigate(
                    ScannerRoute(
                        name,
                        url,
                        sessionID
                    )
                )
            }
        )
    }
}
