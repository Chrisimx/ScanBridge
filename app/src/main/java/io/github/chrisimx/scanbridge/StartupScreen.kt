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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerDiscoveryTopBar(header: String) {
    TopAppBar(
        title = @Composable { Text(header) }
    )
}

data class StartupScreen(
    val nameResource: Int,
    val titleResource: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val fabActivated: Boolean,
    val screenComposable: @Composable (
        innerPadding: PaddingValues,
        navController: NavController,
        showCustomDialog: Boolean,
        setShowCustomDialog: (Boolean) -> Unit,
        statefulScannerMap: SnapshotStateMap<String, DiscoveredScanner>,
        statefulScannerMapSecure: SnapshotStateMap<String, DiscoveredScanner>
    ) -> Unit
)

val INDEXED_TABS = STARTUP_TABS.withIndex()
val StartupScreenSaver = Saver<IndexedValue<StartupScreen>, Int>(save = { it.index }, restore = { IndexedValue(it, STARTUP_TABS[it]) })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupScreen(navController: NavController) {
    var selectedScreen by rememberSaveable(stateSaver = StartupScreenSaver) { mutableStateOf(INDEXED_TABS.first()) }
    val unindexedSelectedScreen = selectedScreen.value

    val context = LocalContext.current

    val statefulScannerMap = remember { mutableStateMapOf<String, DiscoveredScanner>() }
    val statefulScannerMapSecure = remember { mutableStateMapOf<String, DiscoveredScanner>() }

    DisposableEffect(Unit) {
        val discoveryPairOptional = startScannerDiscovery(context, statefulScannerMap, statefulScannerMapSecure)

        if (discoveryPairOptional.isEmpty) {
            return@DisposableEffect onDispose {
                Timber.e("Couldn't start discovery")
            }
        }

        val discoveryPair = discoveryPairOptional.get()

        onDispose {
            Timber.i("Discovery stopped")
            for (d in discoveryPair.second) {
                Timber.i("Stopping discovery for ${d.statefulScannerMap}")
                discoveryPair.first.stopServiceDiscovery(d)
            }
        }
    }

    var showCustomDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = @Composable { ScannerDiscoveryTopBar(stringResource(unindexedSelectedScreen.titleResource)) },
        bottomBar = @Composable {
            NavigationBar {
                STARTUP_TABS.forEachIndexed { idx, screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (unindexedSelectedScreen == screen) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = stringResource(screen.nameResource)
                            )
                        },
                        label = { Text(stringResource(screen.nameResource), style = MaterialTheme.typography.labelMedium) },
                        selected = unindexedSelectedScreen == screen,
                        onClick = { selectedScreen = IndexedValue(idx, screen) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (unindexedSelectedScreen.fabActivated) {
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
            }
        }
    ) { innerPadding ->

        AnimatedContent(
            targetState = unindexedSelectedScreen,
            label = "StartupScreen bottom navigation"
        ) { currentScreen ->
            currentScreen.screenComposable(
                innerPadding,
                navController,
                showCustomDialog,
                { showCustomDialog = it },
                statefulScannerMap,
                statefulScannerMapSecure
            )
        }
    }
}
