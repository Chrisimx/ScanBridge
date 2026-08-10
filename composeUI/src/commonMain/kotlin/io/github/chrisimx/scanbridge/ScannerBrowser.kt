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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.model.EditedCustomScanner
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import io.github.chrisimx.scanbridge.model.DiscoveredScanner
import io.github.chrisimx.scanbridge.model.UrlValidationResult
import io.github.chrisimx.scanbridge.scannerdiscovery.ScannerDiscoveryScreenViewModel
import io.github.chrisimx.scanbridge.uicomponents.FoundScannerItem
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import io.github.chrisimx.scanbridge.uicomponents.dialog.CustomScannerDialog
import io.github.chrisimx.scanbridge.uicomponents.dialog.DeletionDialog
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.custom_scanner
import scanbridge.composeui.generated.resources.custom_scanner_deletion
import scanbridge.composeui.generated.resources.custom_scanner_deletion_confirmation
import scanbridge.composeui.generated.resources.discovered_scanners
import scanbridge.composeui.generated.resources.no_scanners_found
import scanbridge.composeui.generated.resources.saved_scanners
import scanbridge.composeui.generated.resources.twotone_wifi_find_24

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ScannerList(
    innerPadding: PaddingValues,
    navController: NavController,
    customScanners: List<CustomScanner>,
    discoveredScanners: List<DiscoveredScanner>,
    setScannerToDelete: (Uuid?) -> Unit,
    setScannerToEdit: (EditedCustomScanner?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        reverseLayout = true
    ) {
        discoveredScanners.forEach { discoveredScanner ->
            item {
                val handle = discoveredScanner.handle
                FoundScannerItem(
                    handle.stringRepresentation,
                    handle.protocol.protocolIdentifier,
                    discoveredScanner.name,
                    discoveredScanner.iconUrl?.toString(),
                    navController
                )
            }
        }

        if (customScanners.isNotEmpty() && discoveredScanners.isNotEmpty()) {
            item {
                Text(
                    stringResource(Res.string.discovered_scanners),
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
                    customScanner.url.toString(),
                    customScanner.protocolIdentifier,
                    customScanner.name,
                    null,
                    navController,
                    {
                        setScannerToDelete(customScanner.uuid)
                    },
                    {
                        setScannerToEdit(
                            EditedCustomScanner.EditingOld(customScanner)
                        )
                    }
                )
            }
        }

        if (customScanners.isNotEmpty() && discoveredScanners.isNotEmpty()) {
            item {
                Text(
                    stringResource(Res.string.saved_scanners),
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
    currentlyEditedScanner: EditedCustomScanner?,
    setEditedCustomDialog: (EditedCustomScanner?) -> Unit
) {
    val scannerDiscoveryScreenViewModel: ScannerDiscoveryScreenViewModel = koinViewModel()
    val customScanners by scannerDiscoveryScreenViewModel.customScanners.collectAsState()
    val discoveredScanners by scannerDiscoveryScreenViewModel.discoveredScanners.collectAsStateWithLifecycle()
    val protocolsForCustomScanners = scannerDiscoveryScreenViewModel.protocolsForCustomScanners

    var deletionScheduledScanner: Uuid? by remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()

    AnimatedContent(
        targetState = discoveredScanners.isNotEmpty() || customScanners.isNotEmpty(),
        label = "ScannerList"
    ) {
        if (it) {
            ScannerList(
                innerPadding,
                navController,
                customScanners,
                discoveredScanners,
                {
                    deletionScheduledScanner = it
                },
                setEditedCustomDialog
            )
        } else {
            FullScreenError(
                Res.drawable.twotone_wifi_find_24,
                stringResource(Res.string.no_scanners_found)
            )
        }
    }

    val deletionScheduledScannerImmutable = deletionScheduledScanner
    if (deletionScheduledScannerImmutable != null) {
        DeletionDialog(
            Res.string.custom_scanner_deletion,
            Res.string.custom_scanner_deletion_confirmation,
            onDismiss = { deletionScheduledScanner = null },
            onConfirmed = {
                scannerDiscoveryScreenViewModel.deleteScannerByUuid(deletionScheduledScannerImmutable)
                deletionScheduledScanner = null
            }
        )
    }

    if (currentlyEditedScanner != null) {
        CustomScannerDialog(
            protocolsWithExampleHandle = protocolsForCustomScanners,
            onDismiss = { setEditedCustomDialog(null) },
            onConnectClicked = { name, url, protocol, save, navigate ->
                coroutineScope.launch {
                    val defaultName = getString(Res.string.custom_scanner)

                    val result = scannerDiscoveryScreenViewModel.onCustomScannerCreation(
                        name,
                        url,
                        protocol,
                        save,
                        defaultName,
                        currentlyEditedScanner
                    ) ?: return@launch

                    val newCustomScanner = result.customScanner
                    val sessionID = result.sessionId

                    setEditedCustomDialog(null)
                    if (navigate) {
                        navController.navigate(
                            ScannerRoute(
                                newCustomScanner.name,
                                newCustomScanner.url.toString(),
                                newCustomScanner.protocolIdentifier,
                                sessionID.toString()
                            )
                        )
                    }
                }
            },
            currentlyEditedScanner,
            scannerDiscoveryScreenViewModel::validateUrl
        )
    }
}
