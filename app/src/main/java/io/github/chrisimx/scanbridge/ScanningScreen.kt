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
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import io.github.chrisimx.scanbridge.data.ui.ScanningScreenViewModel
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.services.ScanJobEvent
import io.github.chrisimx.scanbridge.uicomponents.ExportSettingsPopup
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import io.github.chrisimx.scanbridge.uicomponents.LoadingScreen
import io.github.chrisimx.scanbridge.uicomponents.dialog.ConfirmCloseDialog
import io.github.chrisimx.scanbridge.uicomponents.dialog.DeletionDialog
import io.github.chrisimx.scanbridge.uicomponents.dialog.LoadingDialog
import io.github.chrisimx.scanbridge.util.clearAndNavigateTo
import io.github.chrisimx.scanbridge.util.snackBarError
import io.github.chrisimx.scanbridge.util.snackbarErrorRetrievingPage
import io.github.chrisimx.scanbridge.util.toReadableString
import io.ktor.http.Url
import java.io.File
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

private const val TAG = "ScanningScreen"

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
            val isScanJobRunning by scanningViewModel.isScanJobRunning.collectAsState()
            val isCancelling by scanningViewModel.isScanJobCancelling.collectAsState()
            AnimatedContent(
                isScanJobRunning
            ) {
                if (it) {
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
                                scanningViewModel.setCancelling(true)
                            }
                        },
                        containerColor = containerColor,
                        contentColor = contentColor,
                        icon = {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                                contentDescription = if (isCancelling) {
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
    sessionID: Uuid,
    application: Application,
    scanningViewModel: ScanningScreenViewModel = koinViewModel {
        parametersOf(
            scannerAddress,
            timeout,
            withDebug,
            certificateValidationDisabled,
            sessionID
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

    val scannedPages by scanningViewModel.scannedPages.collectAsState()
    val currentPage by scanningViewModel.currentPage.collectAsState()

    val currentPageIdx by scanningViewModel.currentPageIdx.collectAsState()
    val scanJobRunning by scanningViewModel.isScanJobRunning.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = currentPageIdx,
        pageCount = {
            (scannedPages.size + if (scanJobRunning) 1 else 0).coerceAtLeast(1)
        }
    )

    LaunchedEffect(scanningViewModel.scanJobRepo.events) {
        scanningViewModel.scanJobRepo.events.collect { event ->
            when (event) {
                is ScanJobEvent.Completed -> scope.launch { pagerState.animateScrollToPage(scannedPages.size - 1) }
                is ScanJobEvent.Failed -> snackbarErrorRetrievingPage(event.reason, scope, context, snackbarHostState)
                is ScanJobEvent.Started -> scope.launch { pagerState.animateScrollToPage(scannedPages.size) }
            }
        }
    }

    // Load stored page idx
    LaunchedEffect(Unit) {
        pagerState.scrollToPage(scanningViewModel.getPageIdx())
    }

    LaunchedEffect(pagerState.currentPage) {
        scanningViewModel.setPageIdx(pagerState.currentPage)
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
                    modifier = Modifier.padding(20.dp),
                    containerColor = if (data.visuals.message.contains("Error")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        SnackbarDefaults.color
                    },
                    action = {
                        IconButton(
                            onClick = { data.dismiss() },
                            modifier = Modifier.testTag("snackbar_dismiss")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                ) {
                    Text(data.visuals.message)
                }
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
            ScanContent(
                innerPadding,
                scannerName,
                scanningViewModel,
                scope,
                navController, currentPage,
                scannedPages,
                pagerState,
                scanJobRunning
            )
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
                        scanningViewModel.doPdfExport(
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
                    },
                    onExportArchive = {
                        scanningViewModel.setShowExportOptionsPopup(false)
                        scanningViewModel.doZipExport(
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
                        scanningViewModel.doPdfExport(
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
                    },
                    onExportArchive = {
                        scanningViewModel.setShowSaveOptionsPopup(false)
                        scanningViewModel.doZipExport(
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
                )
            }
        }

        if (scanningViewModel.scanningScreenData.confirmPageDeleteDialogShown) {
            DeletionDialog(
                onDismiss = { scanningViewModel.setDeletePageDialogShown(false) },
                onConfirmed = {
                    Timber.d("Deleting page")

                    currentPage?.let {
                        scanningViewModel.removeScan(it)
                    }

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
                    scanningViewModel.deleteSession {
                        scanningViewModel.setConfirmDialogShown(false)
                        navController.clearAndNavigateTo(StartUpScreenRoute)
                    }
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
    navController: NavHostController? = null,
    currentPage: ScannedPage?,
    currentPages: List<ScannedPage>,
    pagerState: PagerState,
    scanJobRunning: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!currentPages.isEmpty()) {
            Text(modifier = Modifier.padding(horizontal = 5.dp), text = scannerName, textAlign = TextAlign.Center)
            Text(
                stringResource(
                    R.string.page_x_of_y,
                    pagerState.currentPage + 1,
                    currentPages.size +
                        if (scanJobRunning) 1 else 0
                )
            )

            if (currentPages.size > pagerState.currentPage) {
                Text(
                    currentPage?.originalScanSettings?.inputSource?.toReadableString(
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
            if (page >= currentPages.size) {
                if (scanJobRunning) {
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
                    val imagePath = currentPages.getOrNull(page)?.filePath
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

    if (pagerState.currentPage < currentPages.size) {
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
                        if (currentPages.size <= pagerState.currentPage) {
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
                        val previousPage = currentPages.getOrNull(pagerState.currentPage - 1)
                        val currentPage = currentPage

                        if (previousPage == null || currentPage == null) {
                            return@IconButton
                        }
                        scanningViewModel.swapTwoPages(
                            currentPage,
                            previousPage
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
                        navController?.currentBackStackEntry?.toTypedRoute()?.let { currentRoute ->
                            currentPage?.let { currentPage ->
                                navController.clearAndNavigateTo(
                                    CropImageRoute(
                                        currentPage.scanId.toString(),
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
                        val nextPage = currentPages.getOrNull(pagerState.currentPage + 1)
                        val currentPage = currentPage

                        if (nextPage == null || currentPage == null) {
                            return@IconButton
                        }
                        scanningViewModel.swapTwoPages(
                            currentPage,
                            nextPage
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
                        currentPage?.let {
                            scanningViewModel.rotatePage(it.scanId)
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
