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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.model.toUIInputSourceType
import io.github.chrisimx.scanbridge.scanning.ScanJobEvent
import io.github.chrisimx.scanbridge.uicomponents.ExportSettingsPopup
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import io.github.chrisimx.scanbridge.uicomponents.LoadingScreen
import io.github.chrisimx.scanbridge.uicomponents.dialog.ConfirmCloseDialog
import io.github.chrisimx.scanbridge.uicomponents.dialog.DeletionDialog
import io.github.chrisimx.scanbridge.util.CustomSnackbarVisuals
import io.github.chrisimx.scanbridge.util.SnackbarType
import io.github.chrisimx.scanbridge.util.clearAndNavigateTo
import io.github.chrisimx.scanbridge.util.snackbarErrorRetrievingPage
import io.github.chrisimx.scanbridge.localizationhelper.toReadableString
import io.github.chrisimx.scanbridge.model.PositionAndHeight
import io.github.chrisimx.scanbridge.platformhelper.PlatformBackHandler
import io.github.chrisimx.scanbridge.scanning.FatalScanningScreenError
import io.github.chrisimx.scanbridge.scanning.FileSaveType
import io.github.chrisimx.scanbridge.scanning.ScanningScreenError
import io.github.chrisimx.scanbridge.scanning.ScanningScreenViewModel
import io.github.chrisimx.scanbridge.uicomponents.dialog.LoadingDialog
import io.github.chrisimx.scanbridge.util.platformShareFile
import io.github.chrisimx.scanbridge.util.snackBarError
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.nameWithoutExtension
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.baseline_rotate_right_24
import scanbridge.composeui.generated.resources.cancel_scan
import scanbridge.composeui.generated.resources.cancelling_scan
import scanbridge.composeui.generated.resources.crop
import scanbridge.composeui.generated.resources.delete_current_page
import scanbridge.composeui.generated.resources.desc_scanned_page
import scanbridge.composeui.generated.resources.export
import scanbridge.composeui.generated.resources.export_error
import scanbridge.composeui.generated.resources.exporting
import scanbridge.composeui.generated.resources.job_still_running
import scanbridge.composeui.generated.resources.no_scans_yet
import scanbridge.composeui.generated.resources.outline_crop_24
import scanbridge.composeui.generated.resources.outline_file_save_24
import scanbridge.composeui.generated.resources.outline_scan_24
import scanbridge.composeui.generated.resources.page_deletion_confirmation
import scanbridge.composeui.generated.resources.page_x_of_y
import scanbridge.composeui.generated.resources.retrieving_page
import scanbridge.composeui.generated.resources.rotate_right
import scanbridge.composeui.generated.resources.rotating_page
import scanbridge.composeui.generated.resources.rounded_document_scanner_24
import scanbridge.composeui.generated.resources.save_to_file
import scanbridge.composeui.generated.resources.scan
import scanbridge.composeui.generated.resources.scan_deletion_error
import scanbridge.composeui.generated.resources.scannercapabilities_retrieve_error
import scanbridge.composeui.generated.resources.selected_export_module_not_found
import scanbridge.composeui.generated.resources.settings
import scanbridge.composeui.generated.resources.swap_with_next_page
import scanbridge.composeui.generated.resources.swap_with_previous_page
import scanbridge.composeui.generated.resources.trying_to_retrieve_scannercapabilities
import scanbridge.composeui.generated.resources.twotone_wifi_find_24

private const val TAG = "ScanningScreen"

@Composable
fun ScanningScreenBottomBar(
    scanningViewModel: ScanningScreenViewModel,
    setSaveButtonPositionAndHeight: (PositionAndHeight<Int>) -> Unit,
    setExportButtonPositionAndHeight: (PositionAndHeight<Int>) -> Unit,
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
                Icon(Icons.Filled.Settings, contentDescription = stringResource(Res.string.settings))
            }
            if (scanningViewModel.saveSupported) {
                IconButton(
                    onClick = {
                        scanningViewModel.setShowSaveOptionsPopup(true)
                    },
                    modifier = Modifier.onGloballyPositioned {
                        setSaveButtonPositionAndHeight(
                            PositionAndHeight(
                                it.positionInWindow().x.toInt(),
                                it.positionInWindow().y.toInt(),
                                it.size.height
                            )
                        )
                    }
                ) {
                    Icon(
                        painterResource(Res.drawable.outline_file_save_24),
                        contentDescription = stringResource(Res.string.save_to_file)
                    )
                }
            }
            if (scanningViewModel.shareSupported) {
                IconButton(
                    onClick = {
                        scanningViewModel.setShowExportOptionsPopup(true)
                    },
                    modifier = Modifier.onGloballyPositioned {
                        setExportButtonPositionAndHeight(PositionAndHeight(
                            it.positionInWindow().x.toInt(),
                            it.positionInWindow().y.toInt(),
                            it.size.height
                        ))
                    }
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = stringResource(Res.string.export)
                    )
                }
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
                                Icons.Default.Close,
                                contentDescription = if (isCancelling) {
                                    stringResource(Res.string.cancelling_scan)
                                } else {
                                    stringResource(Res.string.cancel_scan)
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
                                        stringResource(Res.string.cancelling_scan)
                                    } else {
                                        stringResource(Res.string.cancel_scan)
                                    }
                                )
                            }
                        }
                    )
                } else {
                    // Show scan button when not scanning
                    ExtendedFloatingActionButton(
                        onClick = {
                            scanningViewModel.scan()
                        },
                        modifier = Modifier.testTag("scanbtn"),
                        icon = {
                            Icon(
                                painter = painterResource(Res.drawable.outline_scan_24),
                                contentDescription = stringResource(Res.string.scan)
                            )
                        },
                        text = { Text(stringResource(Res.string.scan)) }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ScanningScreen(
    scannerName: String,
    scannerHandle: ScannerHandle,
    navController: NavHostController,
    timeout: UInt,
    withDebug: Boolean,
    certificateValidationDisabled: Boolean,
    sessionID: Uuid,
    scanningViewModel: ScanningScreenViewModel = koinViewModel {
        parametersOf(
            scannerHandle,
            timeout,
            withDebug,
            certificateValidationDisabled,
            sessionID
        )
    }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isReadyForScans by scanningViewModel.readyForScans.collectAsState()
    val isRotating by scanningViewModel.isRotating.collectAsState()
    val isExporting by scanningViewModel.isExporting.collectAsState()

    val fatalError by scanningViewModel.fatalError.collectAsState()

    val scannedPages by scanningViewModel.scannedPages.collectAsState()
    val currentPage by scanningViewModel.currentPage.collectAsState()

    val currentPageIdx by scanningViewModel.currentPageIdx.collectAsState()
    val scanJobRunning by scanningViewModel.isScanJobRunning.collectAsState()

    val confirmPageDeleteDialogShown by scanningViewModel.confirmPageDeleteDialogShown.collectAsState()
    val confirmLeaveDialogShown by scanningViewModel.confirmLeaveDialogShown.collectAsState()

    val availableExportModules by scanningViewModel.availableExportModules.collectAsState()

    var saveButtonPositionAndHeight by remember {
        mutableStateOf(PositionAndHeight(0,0,0))
    }
    var exportPositionAndHeight by remember {
        mutableStateOf(PositionAndHeight(0,0,0))
    }

    val showExportPopup by scanningViewModel.showExportOptions.collectAsState()
    val showSavePopup by scanningViewModel.showSaveOptions.collectAsState()
    val scanSettingsMenuOpen by scanningViewModel.scanSettingsMenuOpen.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = currentPageIdx,
        pageCount = {
            (scannedPages.size + if (scanJobRunning) 1 else 0).coerceAtLeast(1)
        }
    )

    val scanSettingsUIStateHolder by scanningViewModel.scanSettingsVM.collectAsState()

    val clipboard = LocalClipboard.current

    LaunchedEffect(scanningViewModel.scanJobRepo.events) {
        scanningViewModel.scanJobRepo.events.collect { event ->
            when (event) {
                is ScanJobEvent.Completed -> scope.launch { pagerState.animateScrollToPage(scannedPages.size - 1) }

                // TODO: Localize this
                is ScanJobEvent.Failed -> snackbarErrorRetrievingPage(
                    event.error.unlocalizedMessage,
                    scope,
                    snackbarHostState,
                    true,
                    clipboard
                )

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

    LaunchedEffect(Unit) {
        scanningViewModel.errorStream.collect { error ->
            val errorText = error.errorText()
            val hasErrorText = errorText != null
            val fullErrorText = error.errorPretext() + if (hasErrorText) "\n\n" + error.errorText() else ""

            snackBarError(
                fullErrorText,
                scope,
                snackbarHostState,
                hasErrorText,
                errorText,
                clipboard
            )
        }
    }

    PlatformBackHandler {
        scanningViewModel.leaveRequested()
    }

    if (confirmLeaveDialogShown) {
        ConfirmCloseDialog(
            onDismiss = { scanningViewModel.onLeaveDismissed() },
            onConfirmed = {
                scanningViewModel.onLeaveConfirmed {
                    navController.clearAndNavigateTo(StartUpScreenRoute)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val visuals = data.visuals as? CustomSnackbarVisuals
                val type = visuals?.type ?: SnackbarType.DEFAULT

                Snackbar(
                    modifier = Modifier.padding(20.dp),
                    containerColor = type.containerColor,
                    contentColor = type.contentColor,
                    shape = RoundedCornerShape(16.dp),
                    action = {
                        Row {
                            val actionLabel = visuals?.actionLabel
                            if (actionLabel != null) {
                                Button(
                                    onClick = { data.performAction() },
                                    colors = ButtonColors(
                                        type.containerColor,
                                        type.contentColor,
                                        type.containerColor,
                                        type.contentColor
                                    ),
                                    modifier = Modifier.testTag("snackbar_perform_action")
                                ) {
                                    Text(actionLabel)
                                }
                            }

                            IconButton(
                                onClick = { data.dismiss() },
                                colors = IconButtonColors(
                                    type.containerColor,
                                    type.contentColor,
                                    type.containerColor,
                                    type.contentColor
                                ),
                                modifier = Modifier.testTag("snackbar_dismiss")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                ) {
                    Text(data.visuals.message)
                }
            }
        },
        topBar = { },
        bottomBar = {
            if (isReadyForScans) {
                ScanningScreenBottomBar(
                    scanningViewModel = scanningViewModel,
                    { saveButtonPositionAndHeight = it },
                    { exportPositionAndHeight = it }
                )
            }
        }
    ) { innerPadding ->
        AnimatedVisibility(
            fatalError != null,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(1000))
        ) {
            val errorIcon = fatalError?.errorType?.errorIcon() ?: Res.drawable.twotone_wifi_find_24
            val errorPretext = fatalError?.errorType?.errorPretext() ?: stringResource(Res.string.scannercapabilities_retrieve_error)
            val errorText = fatalError?.text ?: "Error text not found"
            val fullScreenErrorText = errorPretext + "\n\n" + errorText

            FullScreenError(
                errorIcon,
                fullScreenErrorText,
                copyButton = true
            )
        }

        if (!isReadyForScans && fatalError == null) {
            LoadingScreen(
                loadingText = Res.string.trying_to_retrieve_scannercapabilities
            )
        }

        if (isReadyForScans && fatalError == null) {
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

            if (scanSettingsMenuOpen) {
                val configuration = LocalWindowInfo.current
                val screenHeight = configuration.containerDpSize.height

                ModalBottomSheet({ scanningViewModel.setScanSettingsMenuOpen(false) }) {
                    ScanSettingsUI(
                        Modifier.heightIn(max = screenHeight * 0.8f),
                        scanSettingsUIStateHolder!!
                    )
                }
            }

            val exportAlpha by animateFloatAsState(
                targetValue = if (showExportPopup) 1f else 0f,
                label = "alphaAnimationExportOptions",
                animationSpec = tween(300)
            )

            val saveOptionsAlpha by animateFloatAsState(
                targetValue = if (showSavePopup) 1f else 0f,
                label = "alphaAnimationSaveOptions",
                animationSpec = tween(300)
            )

            LaunchedEffect(Unit) {
                scanningViewModel.exportQueue.collect { exportEvent ->

                    when (exportEvent.saveType) {
                        FileSaveType.Share -> {
                            platformShareFile(exportEvent.exportedFile)
                        }
                        FileSaveType.Save -> {
                            val file = FileKit.openFileSaver(
                                suggestedName = exportEvent.exportedFile.nameWithoutExtension,
                                defaultExtension = exportEvent.exportedFile.extension,
                            )
                            scanningViewModel.onSaveLocationSelected(exportEvent, file)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (exportAlpha > 0 && scanningViewModel.shareSupported) {
                    ExportSettingsPopup(
                        exportPositionAndHeight,
                        exportAlpha,
                        onDismiss = { scanningViewModel.setShowExportOptionsPopup(false) },
                        availableExportModuleTypes = availableExportModules,
                        onExportModuleSelected = { selectedModule ->
                            scanningViewModel.exportAllPages(
                                selectedModule,
                                FileSaveType.Share
                            )
                        }
                    )
                }

                if (saveOptionsAlpha > 0 && scanningViewModel.saveSupported) {
                    ExportSettingsPopup(
                        saveButtonPositionAndHeight,
                        saveOptionsAlpha,
                        onDismiss = { scanningViewModel.setShowSaveOptionsPopup(false) },
                        availableExportModuleTypes = availableExportModules,
                        onExportModuleSelected = { selectedModule ->
                            scanningViewModel.exportAllPages(
                                selectedModule,
                                FileSaveType.Save
                            )
                        }
                    )
                }
            }

            if (confirmPageDeleteDialogShown) {
                DeletionDialog(
                    Res.string.delete_current_page,
                    Res.string.page_deletion_confirmation,
                    onDismiss = scanningViewModel::onConfirmPageDeletionDismissed,
                    onConfirmed = {
                        currentPage?.let {
                            scanningViewModel.onConfirmPageDeletion(it)
                        }
                    }
                )
            }

            if (isRotating) {
                LoadingDialog(text = Res.string.rotating_page)
            }

            if (isExporting) {
                LoadingDialog(text = Res.string.exporting)
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
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
                    Res.string.page_x_of_y,
                    pagerState.currentPage + 1,
                    currentPages.size +
                        if (scanJobRunning) 1 else 0
                )
            )

            if (currentPages.size > pagerState.currentPage) {
                Text(
                    currentPage?.originalScanSettings?.inputSource?.toUIInputSourceType()?.toReadableString().toString()
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
                        Res.drawable.rounded_document_scanner_24,
                        stringResource(Res.string.no_scans_yet)
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
                        contentDescription = stringResource(Res.string.desc_scanned_page),
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
            ToolbarScanContent(currentPages, pagerState, scanningViewModel, currentPage, coroutineScope, navController)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ToolbarScanContent(
    currentPages: List<ScannedPage>,
    pagerState: PagerState,
    scanningViewModel: ScanningScreenViewModel,
    currentPage: ScannedPage?,
    coroutineScope: CoroutineScope,
    navController: NavHostController?
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
                scanningViewModel.pageDeletionRequested()
            }) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(
                        Res.string.delete_current_page
                    )
                )
            }
            IconButton(onClick = {
                val previousPage = currentPages.getOrNull(pagerState.currentPage - 1)

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
                        Res.string.swap_with_previous_page
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
                    painterResource(Res.drawable.outline_crop_24),
                    contentDescription = stringResource(
                        Res.string.crop
                    )
                )
            }
            IconButton(onClick = {
                val nextPage = currentPages.getOrNull(pagerState.currentPage + 1)

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
                        Res.string.swap_with_next_page
                    )
                )
            }
            IconButton(onClick = {
                currentPage?.let {
                    scanningViewModel.rotatePage(it.scanId)
                }
            }) {
                Icon(
                    painterResource(Res.drawable.baseline_rotate_right_24),
                    contentDescription = stringResource(
                        Res.string.rotate_right
                    )
                )
            }
        }
    }
}

@Composable
private fun FatalScanningScreenError.errorIcon(): DrawableResource {
    return when (this) {
        FatalScanningScreenError.ScannerCapsRetrieval -> Res.drawable.twotone_wifi_find_24
    }
}

@Composable
private fun FatalScanningScreenError.errorPretext(): String {
    return stringResource(when (this) {
        FatalScanningScreenError.ScannerCapsRetrieval -> Res.string.scannercapabilities_retrieve_error
    })
}

private suspend fun ScanningScreenError.errorPretext(): String {
    return when (this) {
        is ScanningScreenError.DeletionError -> getString(Res.string.scan_deletion_error)
        is ScanningScreenError.ExportError -> getString(Res.string.export_error)
        ScanningScreenError.ExportModuleNotFound -> getString(Res.string.selected_export_module_not_found)
        ScanningScreenError.JobStillRunning -> getString(Res.string.job_still_running)
        ScanningScreenError.NoPagesScannedYet -> getString(Res.string.no_scans_yet)
    }
}

private fun ScanningScreenError.errorText(): String? {
    return when (this) {
        is ScanningScreenError.DeletionError -> this.error.toString()
        is ScanningScreenError.ExportError -> this.error.toString()
        ScanningScreenError.ExportModuleNotFound, ScanningScreenError.JobStillRunning -> null
        ScanningScreenError.NoPagesScannedYet -> null
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
        Text(modifier = Modifier.padding(vertical = 15.dp), text = stringResource(Res.string.retrieving_page))
    }
}
