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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.data.model.EditedCustomScanner
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import io.github.chrisimx.scanbridge.escl.EsclScanningProtocol
import io.github.chrisimx.scanbridge.model.DiscoveredScanner
import io.github.chrisimx.scanbridge.model.UrlScannerHandle
import io.github.chrisimx.scanbridge.scannerdiscovery.ScannerDiscoveryScreenViewModel
import io.github.chrisimx.scanbridge.uicomponents.FoundScannerItem
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import io.github.chrisimx.scanbridge.uicomponents.dialog.CustomScannerDialog
import io.github.chrisimx.scanbridge.uicomponents.dialog.DeletionDialog
import io.ktor.http.Url
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

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
                FoundScannerItem(discoveredScanner.name, discoveredScanner.iconUrl, discoveredScanner.handle, navController)
            }
        }

        if (customScanners.isNotEmpty() && discoveredScanners.isNotEmpty()) {
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
                // TODO: Store handles directly instead of converting to UrlHandle here
                FoundScannerItem(
                    customScanner.name,
                    null, // No downloaded icon for custom scanners
                    UrlScannerHandle(getKoin().get<EsclScanningProtocol>(), customScanner.url),
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
    currentlyEditedScanner: EditedCustomScanner?,
    setEditedCustomDialog: (EditedCustomScanner?) -> Unit,
) {
    val scannerDiscoveryScreenViewModel: ScannerDiscoveryScreenViewModel = koinViewModel()
    val customScanners by scannerDiscoveryScreenViewModel.customScanners.collectAsState()
    val discoveredScanners by scannerDiscoveryScreenViewModel.discoveredScanners.collectAsState()

    var deletionScheduledScanner: Uuid? by remember { mutableStateOf(null) }

    AnimatedContent(
        targetState = discoveredScanners.isNotEmpty() || customScanners.isNotEmpty(),
        label = "ScannerList"
    ) {
        if (it) {
            ScannerList(innerPadding,
                navController,
                customScanners,
                discoveredScanners,
                {
                deletionScheduledScanner = it
            }, setEditedCustomDialog)
        } else {
            FullScreenError(
                R.drawable.twotone_wifi_find_24,
                stringResource(R.string.no_scanners_found)
            )
        }
    }

    val deletionScheduledScannerImmutable = deletionScheduledScanner
    if (deletionScheduledScannerImmutable != null) {
        DeletionDialog(
            R.string.custom_scanner_deletion,
            R.string.custom_scanner_deletion_confirmation,
            onDismiss = { deletionScheduledScanner = null },
            onConfirmed = {
                scannerDiscoveryScreenViewModel.deleteScannerByUuid(deletionScheduledScannerImmutable)
                deletionScheduledScanner = null
            }
        )
    }

    if (currentlyEditedScanner != null) {
        val context = LocalContext.current

        CustomScannerDialog(
            onDismiss = { setEditedCustomDialog(null) },
            onConnectClicked = { name, url, save, navigate ->
                val name = name.ifEmpty { context.getString(R.string.custom_scanner) }
                val url = if (url.toString().endsWith("/")) url.toString() else "$url/"
                val sessionID = Uuid.random()
                val uuid = when (currentlyEditedScanner) {
                    is EditedCustomScanner.EditingOld -> currentlyEditedScanner.scanner.uuid
                    EditedCustomScanner.New -> Uuid.random()
                }
                if (save) {
                    scannerDiscoveryScreenViewModel.addScanner(CustomScanner(uuid, name, Url(url)))
                }
                setEditedCustomDialog(null)
                if (navigate) {
                    navController.navigate(
                        ScannerRoute(
                            name,
                            url,
                            sessionID.toString()
                        )
                    )
                }
            },
            currentlyEditedScanner
        )
    }
}
