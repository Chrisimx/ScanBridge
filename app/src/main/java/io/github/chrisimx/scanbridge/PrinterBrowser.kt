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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.data.ui.CustomPrinterViewModel
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import timber.log.Timber
import java.util.*
import android.app.Application

@Composable
fun PrinterBrowser(
    innerPadding: PaddingValues,
    navController: NavController,
    showCustomDialog: Boolean,
    setShowCustomDialog: (Boolean) -> Unit,
    statefulPrinterMap: SnapshotStateMap<String, DiscoveredPrinter>
) {
    val customPrinterViewModel: CustomPrinterViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(LocalContext.current.applicationContext as Application)
    )

    AnimatedContent(
        targetState = statefulPrinterMap.isNotEmpty() || customPrinterViewModel.customPrinters.isNotEmpty(),
        label = "PrinterList"
    ) {
        if (it) {
            PrinterList(innerPadding, navController, statefulPrinterMap, customPrinterViewModel)
        } else {
            FullScreenError(
                R.drawable.twotone_wifi_find_24,
                stringResource(R.string.no_printers_found)
            )
        }
    }
}

fun startPrinterDiscovery(
    context: Context,
    printerMap: SnapshotStateMap<String, DiscoveredPrinter>
): Optional<Pair<NsdManager, Array<PrinterDiscovery>>> {
    val service = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    if (service == null) {
        Timber.e("Couldn't get NsdManager service")
        return Optional.empty()
    }
    val listener = PrinterDiscovery(service, isSecure = false, printerMap)
    val listenerSecure = PrinterDiscovery(service, isSecure = true, printerMap)
    service.discoverServices("_ipp._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
    service.discoverServices("_ipps._tcp", NsdManager.PROTOCOL_DNS_SD, listenerSecure)
    Timber.i("Printer discovery started")
    return Optional.of(Pair(service, arrayOf(listener, listenerSecure)))
}

@Composable
fun PrinterList(
    innerPadding: PaddingValues,
    navController: NavController,
    statefulPrinterMap: SnapshotStateMap<String, DiscoveredPrinter>,
    customPrinterViewModel: CustomPrinterViewModel
) {
    LazyColumn(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        reverseLayout = true
    ) {
        statefulPrinterMap.forEach {
            val discoveredPrinter = it.value
            discoveredPrinter.addresses.forEach { address ->
                item {
                    FoundPrinterItem(discoveredPrinter.name, address, navController)
                }
            }
        }

        if (statefulPrinterMap.isEmpty()) {
            item {
                FullScreenError(
                    R.drawable.twotone_wifi_find_24,
                    stringResource(R.string.no_printers_found)
                )
            }
        }
    }
}

@Composable
fun FoundPrinterItem(name: String, address: String, navController: NavController) {
    // TODO: Implement printer item UI similar to FoundScannerItem
    // For now, just a placeholder
}