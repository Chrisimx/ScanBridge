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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.model.EditedCustomScanner
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.custom_scanner_desc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerDiscoveryTopBar(header: String) {
    TopAppBar(
        title = @Composable { Text(header) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupScreen(
    navController: NavController,
    startupTabsProvider: StartupTabsProvider = koinInject()
) {
    val startupTabs = startupTabsProvider
        .getStartupTabs()

    val indexedStartupTabs = startupTabs.withIndex()

    val startupScreenSaver = Saver<IndexedValue<StartupTabDefinition>, Int>(save = { it.index }, restore = {
        IndexedValue(it, startupTabs[it])
    })

    var selectedScreen by rememberSaveable(stateSaver = startupScreenSaver) { mutableStateOf(indexedStartupTabs.first()) }
    val unindexedSelectedScreen = selectedScreen.value

    var showCustomDialog: EditedCustomScanner? by remember { mutableStateOf(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = @Composable { ScannerDiscoveryTopBar(stringResource(unindexedSelectedScreen.titleResource)) },
        bottomBar = @Composable {
            NavigationBar {
                startupTabs.forEachIndexed { idx, screen ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("bottombutton$idx"),
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
                        showCustomDialog = EditedCustomScanner.New
                    }
                ) {
                    Icon(
                        Icons.Filled.Create,
                        contentDescription = stringResource(Res.string.custom_scanner_desc)
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
                { showCustomDialog = it }
            )
        }
    }
}
