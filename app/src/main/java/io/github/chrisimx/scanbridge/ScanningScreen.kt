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
import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.ScanRegion
import io.github.chrisimx.esclkt.inches
import io.github.chrisimx.esclkt.millimeters
import io.github.chrisimx.esclkt.threeHundredthsOfInch
import io.github.chrisimx.scanbridge.data.ui.ScanRelativeRotation
import io.github.chrisimx.scanbridge.data.ui.ScanningScreenViewModel
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.proto.chunkSizePdfExportOrNull
import io.github.chrisimx.scanbridge.uicomponents.ExportSettingsPopup
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import io.github.chrisimx.scanbridge.uicomponents.LoadingScreen
import io.github.chrisimx.scanbridge.uicomponents.dialog.ConfirmCloseDialog
import io.github.chrisimx.scanbridge.uicomponents.dialog.DeletionDialog
import io.github.chrisimx.scanbridge.uicomponents.dialog.LoadingDialog
import io.github.chrisimx.scanbridge.util.clearAndNavigateTo
import io.github.chrisimx.scanbridge.util.getMaxResolution
import io.github.chrisimx.scanbridge.util.snackBarError
import io.github.chrisimx.scanbridge.util.toReadableString
import io.github.chrisimx.scanbridge.util.zipFiles
import io.ktor.http.Url
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import timber.log.Timber

private const val TAG = "ScanningScreen"

fun doZipExport(
    scanningViewModel: ScanningScreenViewModel,
    context: Context,
    onError: (String) -> Unit,
    saveFileLauncher: ActivityResultLauncher<String>? = null
) {
    if (scanningViewModel.scanningScreenData.currentScansState.isEmpty()) {
        onError(context.getString(R.string.no_scans_yet))
        return
    }
    if (scanningViewModel.scanningScreenData.scanJobRunning) {
        onError(context.getString(R.string.job_still_running))
        return
    }

    scanningViewModel.setLoadingText(R.string.exporting)

    val parentDir = File(context.filesDir, "exports")
    if (!parentDir.exists()) {
        parentDir.mkdir()
    }

    val name = "zipexport-${
        LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH_mm_ss_SSS"))
    }.zip"

    val zipOutputFile = File(parentDir, name)

    var counter = 0
    val digitsNeeded = scanningViewModel.scanningScreenData.currentScansState.size.toString().length
    zipFiles(
        scanningViewModel.scanningScreenData.currentScansState.map { File(it.filePath) },
        zipOutputFile,
        {
            counter++
            "scan-${counter.toString().padStart(digitsNeeded, '0')}.jpg"
        }
    )

    scanningViewModel.setLoadingText(null)

    scanningViewModel.addTempFile(zipOutputFile)

    if (saveFileLauncher == null) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "application/zip"
        share.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipOutputFile
            )
        )
        context.startActivity(share)
    } else {
        scanningViewModel.setFileToSave(zipOutputFile)
        saveFileLauncher.launch(zipOutputFile.name)
    }
}

suspend fun doPdfExport(
    scanningViewModel: ScanningScreenViewModel,
    context: Context,
    onError: (String) -> Unit,
    saveFileLauncher: ActivityResultLauncher<String>? = null
) {
    val scannerCapsNullable = scanningViewModel.scanningScreenData.capabilities
    val scannerCaps = if (scannerCapsNullable == null) {
        onError(context.getString(R.string.scannercapabilities_null))
        return
    } else {
        scannerCapsNullable
    }

    if (scanningViewModel.scanningScreenData.currentScansState.isEmpty()) {
        onError(context.getString(R.string.no_scans_yet))
        return
    }
    if (scanningViewModel.scanningScreenData.scanJobRunning) {
        onError(context.getString(R.string.job_still_running))
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

    val chunkSize = try {
        context.appSettingsStore.data.first().chunkSizePdfExportOrNull?.value ?: 50
    } catch (exception: Exception) {
        Timber.e("doPdfExport couldn't access app settings. Returning default value: $exception")
        50
    }

    val chunks = scanningViewModel.scanningScreenData.currentScansState.chunked(chunkSize)

    chunks.forEachIndexed { index, chunk ->
        val pdfFile = File(
            parentDir,
            "$nameRoot-$index.pdf"
        )
        PdfWriter(pdfFile).use { writer ->
            PdfDocument(writer).use { pdf ->
                Document(pdf).use { document ->
                    chunk.forEachIndexed { i, scan ->
                        val scanRegion =
                            scan.originalScanSettings.scanRegions?.regions?.first() ?: ScanRegion(
                                297.millimeters().toThreeHundredthsOfInch(),
                                210.millimeters().toThreeHundredthsOfInch(),
                                0.threeHundredthsOfInch(),
                                0.threeHundredthsOfInch()
                            )

                        val imageData = ImageDataFactory.create(scan.filePath)

                        val rotated = scan.rotation == ScanRelativeRotation.Rotated

                        val inputSource = scan.originalScanSettings.inputSource ?: InputSource.Platen

                        val fallbackResolution = scannerCaps.getMaxResolution(inputSource)
                        val scannerXResolution = scan.originalScanSettings.xResolution ?: fallbackResolution.xResolution
                        val scannerYResolution = scan.originalScanSettings.yResolution ?: fallbackResolution.yResolution

                        val rotationCorrectedXRes = if (rotated) scannerYResolution else scannerXResolution
                        val rotationCorrectedYRes = if (rotated) scannerXResolution else scannerYResolution

                        // pts are 1/72th inch
                        val widthPts = (imageData.width / rotationCorrectedXRes.toFloat()).inches().toPoints().value
                        val heightPts = (imageData.height / rotationCorrectedYRes.toFloat()).inches().toPoints().value

                        pdf.addNewPage(
                            PageSize(
                                widthPts.toFloat(),
                                heightPts.toFloat()
                            )
                        )

                        val imageElem = Image(imageData)
                        imageElem.setFixedPosition(i + 1, 0f, 0f)
                        imageElem.setHeight(heightPts.toFloat())
                        imageElem.setWidth(widthPts.toFloat())

                        document.add(imageElem)

                        pageCounter++
                        Timber.tag(TAG).d("Added page $pageCounter to PDF")
                    }
                }
            }
        }
    }

    val digitsNeeded = chunks.size.toString().length
    val tempPdfFiles = List(chunks.size) { index ->
        File(parentDir, "$nameRoot-${index.toString().padStart(digitsNeeded, '0')}.pdf")
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

    val mimetype = if (tempPdfFiles.size > 1) "application/zip" else "application/pdf"

    if (saveFileLauncher == null) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = mimetype
        share.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        )
        context.startActivity(share)
    } else {
        scanningViewModel.setFileToSave(outputFile)
        saveFileLauncher.launch(outputFile.name)
    }
}

@Composable
fun ScanningScreenBottomBar(
    scanningViewModel: ScanningScreenViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onSaveButtonPositionChanged: (Triple<Int, Int, Int>) -> Unit,
    onExportPositionChange: (Triple<Int, Int, Int>) -> Unit
) {
    BottomAppBar(
        actions = {
            IconButton(
                modifier = Modifier.testTag("scansettings"),
                onClick = {
                    scanningViewModel.setScanSettingsMenuOpen(
                        true
                    )
                }
            ) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
            }
            IconButton(
                onClick = {
                    scanningViewModel.setShowSaveOptionsPopup(true)
                },
                modifier = Modifier.onGloballyPositioned {
                    onSaveButtonPositionChanged(
                        Triple(
                            it.positionInWindow().x.toInt(),
                            it.positionInWindow().y.toInt(),
                            it.size.height
                        )
                    )
                }
            ) {
                Icon(
                    painterResource(R.drawable.outline_file_save_24),
                    contentDescription = stringResource(R.string.save_to_file)
                )
            }
            IconButton(
                onClick = {
                    scanningViewModel.setShowExportOptionsPopup(true)
                },
                modifier = Modifier.onGloballyPositioned {
                    onExportPositionChange(
                        Triple(
                            it.positionInWindow().x.toInt(),
                            it.positionInWindow().y.toInt(),
                            it.size.height
                        )
                    )
                }
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = stringResource(R.string.export)
                )
            }
        },
        floatingActionButton = {
            AnimatedContent(
                scanningViewModel.scanningScreenData.scanJobRunning
            ) {
                if (it) {
                    val isCancelling = scanningViewModel.scanningScreenData.scanJobCancelling
                    val containerColor by animateColorAsState(
                        targetValue = if (isCancelling) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        animationSpec = tween(durationMillis = 300)
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isCancelling) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onError
                        },
                        animationSpec = tween(durationMillis = 300)
                    )

                    // Show cancel button when scanning
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (!isCancelling) {
                                scanningViewModel.setScanJobCancelling(true)
                            }
                        },
                        containerColor = containerColor,
                        contentColor = contentColor,
                        icon = {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                                contentDescription = if (scanningViewModel.scanningScreenData.scanJobCancelling) {
                                    stringResource(R.string.cancelling_scan)
                                } else {
                                    stringResource(R.string.cancel_scan)
                                }
                            )
                        },
                        text = {
                            AnimatedContent(
                                targetState = isCancelling,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(200)))
                                }
                            ) { targetCancelling ->
                                Text(
                                    text = if (targetCancelling) {
                                        stringResource(R.string.cancelling_scan)
                                    } else {
                                        stringResource(R.string.cancel_scan)
                                    }
                                )
                            }
                        }
                    )
                } else {
                    // Show scan button when not scanning
                    ExtendedFloatingActionButton(
                        onClick = {
                            scanningViewModel.scan(
                                scope,
                                snackbarHostState
                            )
                        },
                        modifier = Modifier.testTag("scanbtn"),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.outline_scan_24),
                                contentDescription = stringResource(R.string.scan)
                            )
                        },
                        text = { Text(stringResource(R.string.scan)) }
                    )
                }
            }
        }
    )
}

fun saveFile(context: Context, sourceFile: File, destUri: Uri) {
    context.contentResolver.openOutputStream(destUri)?.use { out ->
        sourceFile.inputStream().use { input ->
            input.copyTo(out)
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanningScreen(
    scannerName: String,
    scannerAddress: Url,
    navController: NavHostController,
    timeout: UInt,
    withDebug: Boolean,
    certificateValidationDisabled: Boolean,
    sessionID: String,
    application: Application,
    scanningViewModel: ScanningScreenViewModel = viewModel {
        ScanningScreenViewModel(
            application = application,
            address = scannerAddress,
            timeout = timeout,
            withDebugInterceptor = withDebug,
            certificateValidationDisabled = certificateValidationDisabled,
            sessionID = sessionID
        )
    }
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
            scanningViewModel.scanningScreenData.error != null
        }
    }

    if (!isLoaded) {
        BackHandler {
            navController.clearAndNavigateTo(StartUpScreenRoute)
        }

        Scaffold { innerPadding ->
            if (!isError) {
                LaunchedEffect(Unit) {
                    scanningViewModel.retrieveScannerCapabilities()
                }

                LoadingScreen(
                    loadingText = R.string.trying_to_retrieve_scannercapabilities
                )
            }

            AnimatedVisibility(
                isError,
                enter = fadeIn(animationSpec = tween(1000)),
                exit = fadeOut(animationSpec = tween(1000))
            ) {
                val errorDescription = scanningViewModel.scanningScreenData.error
                FullScreenError(
                    errorDescription?.icon ?: R.drawable.twotone_wifi_find_24,
                    stringResource(
                        errorDescription?.pretext ?: R.string.scannercapabilities_retrieve_error,
                        errorDescription?.text ?: "Error text not found"
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
                    containerColor = if (data.visuals.message.contains("Error")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        SnackbarDefaults.color
                    }
                )
            }
        },
        topBar = { },
        bottomBar = {
            ScanningScreenBottomBar(
                scanningViewModel = scanningViewModel,
                scope = scope,
                snackbarHostState = snackbarHostState,
                onExportPositionChange = {
                    scanningViewModel.setExportPopupPosition(
                        it.first,
                        it.second,
                        it.third
                    )
                },
                onSaveButtonPositionChanged = {
                    scanningViewModel.setSavePopupPosition(
                        it.first,
                        it.second,
                        it.third
                    )
                }
            )
        }
    ) { innerPadding ->

        if (scanningViewModel.scanningScreenData.capabilities != null) {
            ScanContent(innerPadding, scannerName, scanningViewModel, scope, navController)
        }

        if (scanningViewModel.scanningScreenData.scanSettingsMenuOpen) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            ModalBottomSheet({ scanningViewModel.setScanSettingsMenuOpen(false) }) {
                ScanSettingsUI(
                    Modifier.heightIn(max = screenHeight * 0.8f),
                    scanningViewModel.scanningScreenData.scanSettingsVM!!
                )
            }
        }
        val exportOptionsPopupPosition =
            scanningViewModel.scanningScreenData.exportOptionsPopupPosition
        var exportOptionsWidth by remember { mutableIntStateOf(0) }
        val exportAlpha by animateFloatAsState(
            targetValue = if (scanningViewModel.scanningScreenData.showExportOptions) 1f else 0f,
            label = "alphaAnimationExportOptions",
            animationSpec = tween(300)
        )

        val saveOptionsPopupPosition =
            scanningViewModel.scanningScreenData.saveOptionsPopupPosition
        var saveOptionsWidth by remember { mutableIntStateOf(0) }
        val saveOptionsAlpha by animateFloatAsState(
            targetValue = if (scanningViewModel.scanningScreenData.showSaveOptions) 1f else 0f,
            label = "alphaAnimationSaveOptions",
            animationSpec = tween(300)
        )

        val currentSourceFileToSave = scanningViewModel.scanningScreenData.sourceFileToSave

        val saveFileLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                uri?.let { uri ->
                    currentSourceFileToSave?.let { sourceFile ->
                        saveFile(context, sourceFile, uri)
                    }
                }
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (exportAlpha > 0) {
                ExportSettingsPopup(
                    exportOptionsPopupPosition,
                    exportOptionsWidth,
                    exportAlpha,
                    onDismiss = { scanningViewModel.setShowExportOptionsPopup(false) },
                    updateWidth = { exportOptionsWidth = it },
                    onExportPdf = {
                        scanningViewModel.setShowExportOptionsPopup(false)
                        thread {
                            scanningViewModel.viewModelScope.launch {
                                doPdfExport(
                                    scanningViewModel,
                                    context,
                                    { error ->
                                        snackBarError(
                                            error,
                                            scope,
                                            context,
                                            snackbarHostState,
                                            false
                                        )
                                    }
                                )
                            }
                        }
                    },
                    onExportArchive = {
                        scanningViewModel.setShowExportOptionsPopup(false)
                        thread {
                            doZipExport(
                                scanningViewModel,
                                context,
                                { error ->
                                    snackBarError(
                                        error,
                                        scope,
                                        context,
                                        snackbarHostState,
                                        false
                                    )
                                }
                            )
                        }
                    }
                )
            }

            if (saveOptionsAlpha > 0) {
                ExportSettingsPopup(
                    saveOptionsPopupPosition,
                    saveOptionsWidth,
                    saveOptionsAlpha,
                    onDismiss = { scanningViewModel.setShowSaveOptionsPopup(false) },
                    updateWidth = { exportOptionsWidth = it },
                    onExportPdf = {
                        scanningViewModel.setShowSaveOptionsPopup(false)
                        thread {
                            scanningViewModel.viewModelScope.launch {
                                doPdfExport(
                                    scanningViewModel,
                                    context,
                                    { error ->
                                        snackBarError(
                                            error,
                                            scope,
                                            context,
                                            snackbarHostState,
                                            false
                                        )
                                    },
                                    saveFileLauncher
                                )
                            }
                        }
                    },
                    onExportArchive = {
                        scanningViewModel.setShowSaveOptionsPopup(false)
                        thread {
                            doZipExport(
                                scanningViewModel,
                                context,
                                { error ->
                                    snackBarError(
                                        error,
                                        scope,
                                        context,
                                        snackbarHostState,
                                        false
                                    )
                                },
                                saveFileLauncher
                            )
                        }
                    }
                )
            }
        }

        if (scanningViewModel.scanningScreenData.confirmPageDeleteDialogShown) {
            DeletionDialog(
                onDismiss = { scanningViewModel.setDeletePageDialogShown(false) },
                onConfirmed = {
                    Timber.d("Deleting page")
                    val index = scanningViewModel.scanningScreenData.pagerState.currentPage
                    Files.delete(Path(scanningViewModel.scanningScreenData.currentScansState[index].filePath))
                    scanningViewModel.removeScanAtIndex(index)
                    scanningViewModel.setDeletePageDialogShown(false)
                }
            )
        }

        if (scanningViewModel.scanningScreenData.progressStringResource != null) {
            LoadingDialog(text = scanningViewModel.scanningScreenData.progressStringResource!!)
        }

        if (scanningViewModel.scanningScreenData.confirmDialogShown) {
            ConfirmCloseDialog(
                onDismiss = { scanningViewModel.setConfirmDialogShown(false) },
                onConfirmed = {
                    scanningViewModel.scanningScreenData.currentScansState.forEach {
                        Files.delete(Path(it.filePath))
                    }
                    scanningViewModel.scanningScreenData.createdTempFiles.forEach(File::delete)

                    scanningViewModel.scanningScreenData.currentScansState.clear()
                    scanningViewModel.setConfirmDialogShown(false)
                    navController.clearAndNavigateTo(StartUpScreenRoute)
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
    coroutineScope: CoroutineScope,
    navController: NavHostController? = null
) {
    val pagerState = scanningViewModel.scanningScreenData.pagerState
    val context = LocalContext.current
    val currentScans = scanningViewModel.scanningScreenData.currentScansState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!scanningViewModel.scanningScreenData.currentScansState.isEmpty()) {
            Text(modifier = Modifier.padding(horizontal = 5.dp), text = scannerName, textAlign = TextAlign.Center)
            Text(
                stringResource(
                    R.string.page_x_of_y,
                    pagerState.currentPage + 1,
                    scanningViewModel.scanningScreenData.currentScansState.size +
                        if (scanningViewModel.scanningScreenData.scanJobRunning) 1 else 0
                )
            )

            if (currentScans.size > pagerState.currentPage) {
                Text(
                    currentScans[pagerState.currentPage].originalScanSettings.inputSource?.toReadableString(
                        context
                    ).toString()
                )
            }
        }

        HorizontalPager(
            modifier = Modifier
                .fillMaxSize(),
            state = pagerState
        ) { page ->
            if (page >= scanningViewModel.scanningScreenData.currentScansState.size) {
                if (scanningViewModel.scanningScreenData.scanJobRunning) {
                    DownloadingPageFullscreen(innerPadding)
                } else {
                    FullScreenError(
                        R.drawable.rounded_document_scanner_24,
                        stringResource(R.string.no_scans_yet)
                    )
                }
                return@HorizontalPager
            } else {
                val zoomState = rememberZoomableState(zoomSpec = ZoomSpec(5f))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 5.dp)
                        .zoomable(zoomState),
                    contentAlignment = Alignment.Center
                ) {
                    val imagePath = scanningViewModel.scanningScreenData.currentScansState.getOrNull(page)?.filePath
                    AsyncImage(
                        model = imagePath,
                        contentDescription = stringResource(R.string.desc_scanned_page),
                        modifier = Modifier
                            .testTag("scan_page")
                    )
                }
            }
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
                    .background(MaterialTheme.colorScheme.inverseOnSurface)
            ) {
                Row {
                    IconButton(onClick = {
                        if (scanningViewModel.scanningScreenData.currentScansState.size <= pagerState.currentPage) {
                            return@IconButton
                        }
                        scanningViewModel.setDeletePageDialogShown(true)
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
                        val currentPageIndex = pagerState.currentPage
                        navController?.currentBackStackEntry?.toTypedRoute()?.let { currentRoute ->
                            scanningViewModel.scanningScreenData.currentScansState.getOrNull(currentPageIndex)?.let { currentPage ->
                                navController.clearAndNavigateTo(
                                    CropImageRoute(
                                        scanningViewModel.scanningScreenData.sessionID,
                                        currentPageIndex,
                                        Json.encodeToString(currentRoute)
                                    )
                                )
                            }
                        }
                    }) {
                        Icon(
                            painterResource(R.drawable.outline_crop_24),
                            contentDescription = stringResource(
                                R.string.crop
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
                    IconButton(onClick = {
                        thread {
                            scanningViewModel.rotateCurrentPage()
                        }
                    }) {
                        Icon(
                            painterResource(R.drawable.baseline_rotate_right_24),
                            contentDescription = stringResource(
                                R.string.rotate_right
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadingPageFullscreen(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(modifier = Modifier.padding(vertical = 15.dp), text = stringResource(R.string.retrieving_page))
    }
}
