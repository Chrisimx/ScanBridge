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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.data.ui.CustomPrinterViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerDiscoveryTopBar(header: String) {
    TopAppBar(
        title = @Composable { Text(header) }
    )
}

@Composable
fun StartupScreen(navController: NavController) {
    var selectedItem = rememberSaveable { mutableIntStateOf(0) }
    val items = listOf(stringResource(R.string.discovery), stringResource(R.string.print), stringResource(R.string.settings))
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Share, Icons.Filled.Settings)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.Share, Icons.Outlined.Settings)
    val context = LocalContext.current
    val header =
        when (selectedItem.intValue) {
            0 -> stringResource(R.string.header_scannerbrowser)
            1 -> stringResource(R.string.header_printerbrowser)
            else -> stringResource(R.string.settings)
        }

    val statefulScannerMap = remember { mutableStateMapOf<String, DiscoveredScanner>() }
    val statefulScannerMapSecure = remember { mutableStateMapOf<String, DiscoveredScanner>() }
    val statefulPrinterMap = remember { mutableStateMapOf<String, DiscoveredPrinter>() }

    DisposableEffect(Unit) {
        val scannerDiscoveryPairOptional = startScannerDiscovery(context, statefulScannerMap, statefulScannerMapSecure)
        val printerDiscoveryPairOptional = startPrinterDiscovery(context, statefulPrinterMap)

        onDispose {
            Timber.i("Discovery stopped")
            
            if (scannerDiscoveryPairOptional.isPresent) {
                val scannerDiscoveryPair = scannerDiscoveryPairOptional.get()
                for (d in scannerDiscoveryPair.second) {
                    Timber.i("Stopping scanner discovery for ${d.statefulScannerMap}")
                    scannerDiscoveryPair.first.stopServiceDiscovery(d)
                }
            }
            
            if (printerDiscoveryPairOptional.isPresent) {
                val printerDiscoveryPair = printerDiscoveryPairOptional.get()
                for (d in printerDiscoveryPair.second) {
                    Timber.i("Stopping printer discovery for ${d.statefulPrinterMap}")
                    printerDiscoveryPair.first.stopServiceDiscovery(d)
                }
            }
        }
    }

    var showCustomDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = @Composable { ScannerDiscoveryTopBar(header) },
        bottomBar = @Composable {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedItem.intValue == index) selectedIcons[index] else unselectedIcons[index],
                                contentDescription = item
                            )
                        },
                        label = { Text(item, style = MaterialTheme.typography.labelMedium) },
                        selected = selectedItem.intValue == index,
                        onClick = { selectedItem.intValue = index }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedItem.intValue == 0) {
                FloatingActionButton(
                    modifier = Modifier.testTag("custom_scanner_fab"),
                    onClick = {
                        showCustomDialog = true
                    }
                ) {
                    Icon(
                        Icons.Filled.Create,
                        contentDescription = stringResource(R.string.custom_scanner_desc)
                    )
                }
            } else if (selectedItem.intValue == 1) {
                FloatingActionButton(
                    modifier = Modifier.testTag("custom_printer_fab"),
                    onClick = {
                        showCustomDialog = true
                    }
                ) {
                    Icon(
                        Icons.Filled.Create,
                        contentDescription = stringResource(R.string.custom_scanner_printer_desc)
                    )
                }
            }
        }
    ) { innerPadding ->

        AnimatedContent(
            targetState = selectedItem.intValue,
            label = "StartupScreen bottom navigation"
        ) {
            when (it) {
                0 -> ScannerBrowser(
                    innerPadding,
                    navController,
                    showCustomDialog,
                    { showCustomDialog = it },
                    statefulScannerMap,
                    statefulScannerMapSecure
                )
                1 -> PrinterBrowser(
                    innerPadding,
                    navController,
                    showCustomDialog,
                    { showCustomDialog = it },
                    statefulPrinterMap
                )
                2 -> {
                    AppSettingsScreen(innerPadding)
                }
            }
        }
    }
}
