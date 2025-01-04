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
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.data.ui.ScanningScreenViewModel
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import io.github.chrisimx.scanbridge.uicomponents.LoadingScreen
import io.github.chrisimx.scanbridge.uicomponents.dialog.ConfirmCloseDialog
import io.github.chrisimx.scanbridge.uicomponents.dialog.LoadingDialog
import io.github.chrisimx.scanbridge.util.snackBarError
import io.github.chrisimx.scanbridge.util.toJobStateString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import okhttp3.HttpUrl
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val TAG = "ScanningScreen"

fun retrieveScannerCapabilities(
    scannerAddress: HttpUrl,
    scanningViewModel: ScanningScreenViewModel,
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {

    val esclClient = scanningViewModel.scanningScreenData.esclClient

    val scannerCapabilitiesResult = esclClient.getScannerCapabilities()

    if (scannerCapabilitiesResult !is ESCLRequestClient.ScannerCapabilitiesResult.Success) {
        Log.e(TAG, "Error while retrieving ScannerCapabilities: $scannerCapabilitiesResult")
        scanningViewModel.setError("Error: $scannerCapabilitiesResult")
        return
    }

    scanningViewModel.setScannerCapabilities(scannerCapabilitiesResult.scannerCapabilities)
}

@OptIn(ExperimentalUuidApi::class)
fun doScan(
    esclRequestClient: ESCLRequestClient,
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    scanSettings: ScanSettings,
    viewModel: ScanningScreenViewModel,
) {
    thread {
        if (viewModel.scanningScreenData.scanJobRunning) {
            snackBarError(
                context.getString(R.string.job_still_running),
                scope,
                context,
                snackbarHostState,
                false
            )
            return@thread
        }

        viewModel.setScanJobRunning(true)

        val job =
            esclRequestClient.createJob(scanSettings)
        Log.d(TAG, "Sent scan request to scanner. Result: $job")
        if (job !is ESCLRequestClient.ScannerCreateJobResult.Success) {
            viewModel.setScanJobRunning(false)
            snackBarError(job.toString(), scope, context, snackbarHostState)
            return@thread
        }

        while (true) {
            var nextPage = job.scanJob.retrieveNextPage()
            val status = job.scanJob.getJobStatus()
            val jobStateString = status?.jobState.toJobStateString(context)
            if (nextPage is ESCLRequestClient.ScannerNextPageResult.NoFurtherPages) {
                viewModel.setScanJobRunning(false)
                scope.launch {
                    snackbarHostState.showSnackbar("No further pages. $jobStateString")
                }
                return@thread
            } else if (nextPage !is ESCLRequestClient.ScannerNextPageResult.Success) {
                viewModel.setScanJobRunning(false)
                return@thread
            }
            nextPage.page.use {
                val scanPageFile = "scan-" + Uuid.random().toString() + ".jpg"
                val filePath = File(context.filesDir, scanPageFile).toPath()

                Log.d(TAG, "File created: $filePath")

                Files.copy(it.data.body!!.byteStream(), filePath)
                val imageBitmap = BitmapFactory.decodeFile(filePath.toString())?.asImageBitmap()
                if (imageBitmap == null) {
                    Log.e(TAG, "Couldn't decode received image")
                    snackBarError(
                        "Couldn't decode received image. $jobStateString",
                        scope,
                        context,
                        snackbarHostState
                    )
                    filePath.toFile().delete()
                    return@thread
                }
                viewModel.addScan(filePath.toString(), scanSettings)
            }
        }
    }
}

@Composable
fun ScanningScreenBottomBar(
    scanningViewModel: ScanningScreenViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    BottomAppBar(
        actions = {
            IconButton(onClick = {
                scanningViewModel.setScanSettingsMenuOpen(
                    true
                )
            }) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
            }
            IconButton(onClick = {
                thread {
                    scanningViewModel.setExportRunning(true)
                    PdfDocument().apply {
                        for (scan in scanningViewModel.scanningScreenData.currentScansState) {
                            val bitmap =
                                BitmapFactory.decodeFile(scan.first)
                            val scanRegion =
                                scan.second.scanRegions!!.regions.first()
                            val width72thInches =
                                scanRegion.width.toInches().value * 72.0
                            val height72thInches =
                                scanRegion.height.toInches().value * 72.0
                            val pageInfo = PdfDocument.PageInfo.Builder(
                                width72thInches.toInt(),
                                height72thInches.toInt(),
                                1
                            ).create()
                            val page = startPage(pageInfo)

                            page.canvas.drawBitmap(
                                bitmap,
                                null,
                                Rect(
                                    0,
                                    0,
                                    width72thInches.toInt(),
                                    height72thInches.toInt()
                                ),
                                null
                            )
                            this.finishPage(page)
                        }

                        val parentDir = File(context.filesDir, "exports")
                        if (!parentDir.exists()) {
                            parentDir.mkdir()
                        }
                        val pdfFile = File(
                            parentDir, "pdfexport-${
                                LocalDateTime.now()
                                    .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH_mm_ss_SSS"))
                            }.pdf"
                        )
                        writeTo(pdfFile.outputStream())
                        scanningViewModel.setExportRunning(false)
                        val share = Intent(Intent.ACTION_SEND)
                        share.type = "application/pdf"
                        share.putExtra(
                            Intent.EXTRA_STREAM,
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                pdfFile
                            )
                        )
                        context.startActivity(share)
                    }
                }
            }) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = stringResource(R.string.export)
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val scanSettingsData =
                        scanningViewModel.scanningScreenData.scanSettingsVM!!.scanSettingsComposableData
                    doScan(
                        scanningViewModel.scanningScreenData.esclClient,
                        context,
                        scope,
                        snackbarHostState,
                        scanSettingsData.scanSettingsState.toESCLKtScanSettings(
                            scanSettingsData.selectedInputSourceCapabilities
                        ),
                        scanningViewModel
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.outline_scan_24),
                        contentDescription = stringResource(
                            R.string.scan
                        )
                    )
                },
                text = { Text(stringResource(R.string.scan)) }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanningScreen(
    scannerName: String,
    scannerAddress: HttpUrl,
    navController: NavHostController,
    scanningViewModel: ScanningScreenViewModel = viewModel {
        ScanningScreenViewModel(
            address = scannerAddress
        )
    },
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isLoaded by remember {
        derivedStateOf {
            scanningViewModel.scanningScreenData.capabilities != null &&
                    scanningViewModel.scanningScreenData.scanSettingsVM != null
        }
    }

    val isError by remember {
        derivedStateOf {
            scanningViewModel.scanningScreenData.errorString != null
        }
    }

    if (!isLoaded && !isError) {
        LaunchedEffect(Unit) {
            thread {
                retrieveScannerCapabilities(
                    scannerAddress,
                    scanningViewModel,
                    context,
                    scope,
                    snackbarHostState
                )
            }
        }

        LoadingScreen(
            loadingText = R.string.trying_to_retrieve_scannercapabilities
        )
    }

    if (isError) {
        FullScreenError(
            R.drawable.rounded_scanner_24,
            stringResource(
                R.string.scannercapabilities_retrieve_error,
                scanningViewModel.scanningScreenData.errorString!!
            ),
            copyButton = true
        )
    }

    if (!isLoaded) return

    BackHandler {
        scanningViewModel.setConfirmDialogShown(true)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    data,
                    containerColor = if (data.visuals.message.contains("Error")) MaterialTheme.colorScheme.error
                    else SnackbarDefaults.color
                )
            }
        },
        topBar = { },
        bottomBar = {
            ScanningScreenBottomBar(
                scanningViewModel = scanningViewModel,
                scope = scope,
                snackbarHostState = snackbarHostState
            )
        },
    ) { innerPadding ->

        if (scanningViewModel.scanningScreenData.capabilities != null) {
            ScanContent(innerPadding, scanningViewModel, scope)
        }

        if (scanningViewModel.scanningScreenData.scanSettingsMenuOpen) {
            ModalBottomSheet({ scanningViewModel.setScanSettingsMenuOpen(false) }) {
                ScanSettingsUI(
                    Modifier,
                    context,
                    scanningViewModel.scanningScreenData.scanSettingsVM!!
                )
            }
        }

        if (scanningViewModel.scanningScreenData.exportRunning) {
            LoadingDialog(text = R.string.exporting)
        }

        if (scanningViewModel.scanningScreenData.confirmDialogShown) {
            ConfirmCloseDialog(
                onDismiss = { scanningViewModel.setConfirmDialogShown(false) },
                onConfirmed = {
                    scanningViewModel.scanningScreenData.currentScansState.forEach {
                        Files.delete(Path(it.first))
                    }
                    scanningViewModel.scanningScreenData.currentScansState.clear()
                    scanningViewModel.setConfirmDialogShown(false)
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun ScanContent(
    innerPadding: PaddingValues,
    scanningViewModel: ScanningScreenViewModel,
    coroutineScope: CoroutineScope
) {
    val pagerState = rememberPagerState(
        pageCount = {
            scanningViewModel.scanningScreenData.currentScansState.size + if (scanningViewModel.scanningScreenData.scanJobRunning) 1 else 0
        }
    )

    if (!scanningViewModel.scanningScreenData.currentScansState.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(
                    R.string.page_x_of_y,
                    pagerState.currentPage + 1,
                    scanningViewModel.scanningScreenData.currentScansState.size + if (scanningViewModel.scanningScreenData.scanJobRunning) 1 else 0
                )
            )

            if (scanningViewModel.scanningScreenData.currentScansState.size > pagerState.currentPage) {
                Text(scanningViewModel.scanningScreenData.currentScansState[pagerState.currentPage].second.inputSource.toString())
            }
        }
    } else if (!scanningViewModel.scanningScreenData.scanJobRunning) {
        FullScreenError(R.drawable.rounded_scanner_24, stringResource(R.string.no_scans_yet))
    }

    HorizontalPager(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        state = pagerState
    ) { page ->
        if (page == scanningViewModel.scanningScreenData.currentScansState.size) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.retrieving_page))
            }
            return@HorizontalPager
        } else {
            val zoomState = rememberZoomableState(zoomSpec = ZoomSpec(5f))

            AsyncImage(
                model = scanningViewModel.scanningScreenData.currentScansState[page].first,
                contentDescription = stringResource(R.string.desc_scanned_page),
                modifier = Modifier
                    .zoomable(zoomState)
                    .padding(vertical = 5.dp),
            )
        }
    }

    if (pagerState.currentPage < scanningViewModel.scanningScreenData.currentScansState.size) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .clip(
                        RoundedCornerShape(16.dp)
                    )
                    .background(MaterialTheme.colorScheme.inverseOnSurface),
            ) {
                Row {
                    IconButton(onClick = {
                        if (scanningViewModel.scanningScreenData.currentScansState.size <= pagerState.currentPage) {
                            return@IconButton
                        }
                        Files.delete(Path(scanningViewModel.scanningScreenData.currentScansState[pagerState.currentPage].first))
                        scanningViewModel.removeScanAtIndex(pagerState.currentPage)
                    }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(
                                R.string.delete_current_page
                            )
                        )
                    }
                    IconButton(onClick = {
                        scanningViewModel.swapTwoPages(
                            pagerState.currentPage,
                            pagerState.currentPage - 1
                        )
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(
                                R.string.swap_with_previous_page
                            )
                        )
                    }
                    IconButton(onClick = {
                        scanningViewModel.swapTwoPages(
                            pagerState.currentPage,
                            pagerState.currentPage + 1
                        )
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = stringResource(
                                R.string.swap_with_next_page
                            )
                        )
                    }
                }
            }
        }
    }
}