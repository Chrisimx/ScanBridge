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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
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
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.ScanRegion
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.millimeters
import io.github.chrisimx.esclkt.threeHundredthsOfInch
import io.github.chrisimx.scanbridge.data.ui.ScanningScreenViewModel
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import io.github.chrisimx.scanbridge.uicomponents.LoadingScreen
import io.github.chrisimx.scanbridge.uicomponents.dialog.ConfirmCloseDialog
import io.github.chrisimx.scanbridge.uicomponents.dialog.LoadingDialog
import io.github.chrisimx.scanbridge.util.rotateBy90
import io.github.chrisimx.scanbridge.util.snackBarError
import io.github.chrisimx.scanbridge.util.toJobStateString
import io.github.chrisimx.scanbridge.util.zipFiles
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
    scanningViewModel: ScanningScreenViewModel,
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


fun String.extractBaseFilename(): String? {
    val regex = Regex("^scan-[a-f0-9-]+")
    return regex.find(this)?.value
}

fun rotate(
    context: Context,
    scanningViewModel: ScanningScreenViewModel,
) {
    if (scanningViewModel.scanningScreenData.currentScansState.isEmpty()) {
        return
    }
    scanningViewModel.setLoadingText(R.string.rotating_page)

    val currentScans = scanningViewModel.scanningScreenData.currentScansState
    val currentPagePath =
        currentScans[scanningViewModel.scanningScreenData.pagerState.currentPage].first
    val currentPageFile = File(currentPagePath)

    Log.d(TAG, "Decoding $currentPagePath")
    val originalBitmap = BitmapFactory.decodeFile(currentPagePath)
    Log.d(TAG, "Rotating $currentPagePath")
    val rotatedBitmap = originalBitmap.rotateBy90()
    originalBitmap.recycle()

    val baseFileName = currentPageFile.name.extractBaseFilename()

    val newFile = File(context.filesDir, "$baseFileName edit-${System.currentTimeMillis()}.jpg")

    Log.d(TAG, "Saving rotated $currentPagePath")
    newFile.outputStream().use {
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }

    Log.d(TAG, "Finished saving rotated $currentPagePath")

    val index = scanningViewModel.scanningScreenData.pagerState.currentPage
    val scanSettings = currentScans[index].second
    Log.d(TAG, "Updating UI state after rotation")
    scanningViewModel.removeScanAtIndex(index)
    scanningViewModel.addTempFile(currentPageFile)
    scanningViewModel.addScanAtIndex(newFile.absolutePath, scanSettings, index)

    scanningViewModel.setLoadingText(null)
}

fun doExport(scanningViewModel: ScanningScreenViewModel, context: Context) {
    if (scanningViewModel.scanningScreenData.currentScansState.isEmpty()) {
        return
    }

    scanningViewModel.setLoadingText(R.string.exporting)

    val parentDir = File(context.filesDir, "exports")
    if (!parentDir.exists()) {
        parentDir.mkdir()
    }

    val nameRoot = "pdfexport-${
        LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH_mm_ss_SSS"))
    }"

    var pageCounter = 0

    val chunks = scanningViewModel.scanningScreenData.currentScansState.chunked(50)

    chunks.forEachIndexed { index, chunk ->
        val pdfFile = File(
            parentDir, "$nameRoot-${index}.pdf"
        )
        PdfWriter(pdfFile).use { writer ->
            PdfDocument(writer).use { pdf ->
                Document(pdf).use { document ->
                    chunk.forEachIndexed { i, scan ->
                        val scanRegion =
                            scan.second.scanRegions?.regions?.first() ?: ScanRegion(
                                297.millimeters().toThreeHundredthsOfInch(),
                                210.millimeters().toThreeHundredthsOfInch(),
                                0.threeHundredthsOfInch(),
                                0.threeHundredthsOfInch()
                            )

                        val imageData = ImageDataFactory.create(scan.first)
                        val scanRegionOrientation = scanRegion.height.value > scanRegion.width.value
                        val actualImageOrientation = imageData.height > imageData.width

                        var width72thInches =
                            scanRegion.width.toInches().value * 72.0
                        var height72thInches =
                            scanRegion.height.toInches().value * 72.0

                        if (actualImageOrientation != scanRegionOrientation) {
                            val tmp = width72thInches
                            width72thInches = height72thInches
                            height72thInches = tmp
                        }

                        pdf.addNewPage(
                            PageSize(
                                width72thInches.toFloat(),
                                height72thInches.toFloat()
                            )
                        )

                        val imageElem = Image(imageData)
                        imageElem.setFixedPosition(i + 1, 0f, 0f)
                        imageElem.setHeight(height72thInches.toFloat())
                        imageElem.setWidth(width72thInches.toFloat())

                        document.add(imageElem)

                        pageCounter++
                        Log.d(TAG, "Added page $pageCounter to PDF")
                    }
                }
            }
        }
    }

    val tempPdfFiles = chunks.mapIndexed { index, _ ->
        File(parentDir, "$nameRoot-${index}.pdf")
    }

    tempPdfFiles.forEach { scanningViewModel.addTempFile(it) }

    var outputFile: File
    if (tempPdfFiles.size > 1) {
        outputFile = File(parentDir, "$nameRoot.zip")
        zipFiles(tempPdfFiles, outputFile)
        scanningViewModel.addTempFile(outputFile)
    } else {
        outputFile = tempPdfFiles[0]
    }

    scanningViewModel.setLoadingText(null)
    val share = Intent(Intent.ACTION_SEND)
    share.type = if (tempPdfFiles.size > 1) "application/zip" else "application/pdf"
    share.putExtra(
        Intent.EXTRA_STREAM,
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )
    )
    context.startActivity(share)
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
        viewModel.scrollToPage(
            scope = scope,
            pageNr = viewModel.scanningScreenData.currentScansState.size
        )

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
                    doExport(scanningViewModel, context)
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
                    thread {
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
                    }
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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

    if (!isLoaded) {
        Scaffold { innerPadding ->
            if (!isError) {
                LaunchedEffect(Unit) {
                    thread {
                        retrieveScannerCapabilities(
                            scanningViewModel,
                        )
                    }
                }

                LoadingScreen(
                    loadingText = R.string.trying_to_retrieve_scannercapabilities
                )
            }

            AnimatedVisibility(
                isError,
                enter = fadeIn(animationSpec = tween(1000)),
                exit = fadeOut(animationSpec = tween(1000)),
            ) {
                FullScreenError(
                    R.drawable.twotone_wifi_find_24,
                    stringResource(
                        R.string.scannercapabilities_retrieve_error,
                        scanningViewModel.scanningScreenData.errorString!!
                    ),
                    copyButton = true
                )
            }
        }

        return
    }

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
            ScanContent(innerPadding, scannerName, scanningViewModel, scope)
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

        if (scanningViewModel.scanningScreenData.progressStringResource != null) {
            LoadingDialog(text = scanningViewModel.scanningScreenData.progressStringResource!!)
        }

        if (scanningViewModel.scanningScreenData.confirmDialogShown) {
            ConfirmCloseDialog(
                onDismiss = { scanningViewModel.setConfirmDialogShown(false) },
                onConfirmed = {
                    scanningViewModel.scanningScreenData.currentScansState.forEach {
                        Files.delete(Path(it.first))
                    }
                    scanningViewModel.scanningScreenData.createdTempFiles.forEach(File::delete)

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
    scannerName: String,
    scanningViewModel: ScanningScreenViewModel,
    coroutineScope: CoroutineScope
) {
    val pagerState = scanningViewModel.scanningScreenData.pagerState

    if (!scanningViewModel.scanningScreenData.currentScansState.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(scannerName)
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
        FullScreenError(
            R.drawable.rounded_document_scanner_24,
            stringResource(R.string.no_scans_yet)
        )
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
                    val context = LocalContext.current
                    IconButton(onClick = {
                        thread {
                            rotate(context, scanningViewModel)
                        }
                    }) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = stringResource(
                                R.string.rotate_left
                            )
                        )
                    }
                }
            }
        }
    }
}