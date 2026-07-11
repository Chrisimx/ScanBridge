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

package io.github.chrisimx.scanbridge.data.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.SnackbarHostState
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.anyscan.CommonScanSettingsEditor
import io.github.chrisimx.anyscan.CommonScannerCapabilities
import io.github.chrisimx.anyscan.ScanSettingsMap
import io.github.chrisimx.anyscan.inches
import io.github.chrisimx.anyscan.millimeters
import io.github.chrisimx.anyscan.threeHundredthsOfInch
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.ScanRegion
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.androidservice.ScanJobForegroundService
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.db.entities.Session
import io.github.chrisimx.scanbridge.db.entities.TempFile
import io.github.chrisimx.scanbridge.model.ScanJob
import io.github.chrisimx.scanbridge.model.ScanRelativeRotation
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV1
import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.model.scannerCapabilities
import io.github.chrisimx.scanbridge.model.toggleRotation
import io.github.chrisimx.scanbridge.ports.ScannerCapabilitiesResult
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import io.github.chrisimx.scanbridge.proto.chunkSizePdfExportOrNull
import io.github.chrisimx.scanbridge.services.ScanJobRepository
import io.github.chrisimx.scanbridge.stores.DefaultScanSettingsStore
import io.github.chrisimx.scanbridge.util.getEditedImageName
import io.github.chrisimx.scanbridge.util.rotateBy90
import io.github.chrisimx.scanbridge.util.saveAsJPEG
import io.github.chrisimx.scanbridge.util.snackbarErrorRetrievingPage
import io.github.chrisimx.scanbridge.util.zipFiles
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.InjectedParam
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform.getKoin
import timber.log.Timber

enum class ScanningScreenEvent {
    SCAN_FINISHED,
    SCAN_STARTED
}

class ScanningScreenViewModel(
    @InjectedParam
    val scannerHandle: ScannerHandle,
    @InjectedParam
    val timeout: UInt,
    @InjectedParam
    val withDebugInterceptor: Boolean,
    @InjectedParam
    val certificateValidationDisabled: Boolean,
    @InjectedParam
    val sessionID: Uuid,
    val db: ScanBridgeDb,
    application: Application,
    val scanJobRepo: ScanJobRepository,
) : AndroidViewModel(application) {
    private val _scanningScreenData =
        ScanningScreenData(
            sessionID
        )
    val scanningScreenData: ImmutableScanningScreenData
        get() = _scanningScreenData.toImmutable()

    private val tmpFileDao = db.tmpFileDao()
    private val scannedPageDao = db.scannedPageDao()
    private val sessionDao = db.sessionDao()

    val tempFiles: StateFlow<List<TempFile>> = tmpFileDao.getFilesFlowBySessionId(sessionID)
        .onEach { Timber.d("Temp files changed: $it") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    val scannedPages: StateFlow<List<ScannedPage>> = scannedPageDao.getAllForSessionFlow(sessionID)
        .onEach { Timber.d("Scanned pages changed: $it") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    val session: StateFlow<Session?> = sessionDao.getSessionFlowById(sessionID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isScanJobRunning = scanJobRepo.isJobRunning
    val isScanJobCancelling = scanJobRepo.shouldCancel

    val currentPageIdx = session.map {
        it?.currentPage ?: 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val currentPage = combine(currentPageIdx, scannedPages) { pageIdx, pages ->
        pages.getOrNull(pageIdx)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    suspend fun addTempFile(file: File) {
        val tmpFile = TempFile(Uuid.generateV4(), sessionID, file.absolutePath)
        tmpFileDao.insertAll(tmpFile)
    }

    suspend fun getPageIdx(): Int = sessionDao.getSessionById(sessionID)?.currentPage ?: 0

    fun setCancelling(value: Boolean) {
        scanJobRepo.setCancel(value)
    }

    init {
        session
            .map { it?.currentScanSettings to it?.currentSettingsUIData }
            .distinctUntilChanged()
            .filter { it.first != null }
            .onEach { (scanSettings, settingsUiState) ->
                DefaultScanSettingsStore.save(application, scanSettings!!, settingsUiState)
            }
            .launchIn(viewModelScope)
    }

    fun setPageIdx(idx: Int) {
        viewModelScope.launch {
            sessionDao.updateCurrentPage(sessionID, idx)
        }
    }

    fun setShowExportOptionsPopup(show: Boolean) {
        _scanningScreenData.showExportOptions.value = show
    }

    fun setExportPopupPosition(x: Int, y: Int, height: Int) {
        _scanningScreenData.exportOptionsPopupPosition.value = Triple(x, y, height)
    }

    fun setFileToSave(file: File) {
        _scanningScreenData.sourceFileToSave.value = file
    }

    fun setShowSaveOptionsPopup(show: Boolean) {
        _scanningScreenData.showSaveOptions.value = show
    }

    fun setSavePopupPosition(x: Int, y: Int, height: Int) {
        _scanningScreenData.savePopupPosition.value = Triple(x, y, height)
    }

    fun setLoadingText(stringRes: Int?) {
        _scanningScreenData.stateProgressStringRes.value = stringRes
    }

    fun setScanSettingsMenuOpen(value: Boolean) {
        _scanningScreenData.scanSettingsMenuOpen.value = value
    }

    fun setConfirmDialogShown(value: Boolean) {
        _scanningScreenData.confirmDialogShown.value = value
    }

    fun setDeletePageDialogShown(value: Boolean) {
        _scanningScreenData.confirmPageDeleteDialogShown.value = value
    }

    fun setError(error: String?, titleResource: Int? = null, errorIcon: Int? = null) {
        _scanningScreenData.error.value = ErrorDescription(
            titleResource,
            errorIcon,
            error
        )
    }

    fun rotatePage(pageId: Uuid) {
        viewModelScope.launch {
            rotatePageInternal(pageId)
        }
    }

    suspend fun rotatePageInternal(pageId: Uuid) {
        val scannedPage = scannedPageDao.getByScanId(pageId)
        if (scannedPage == null || _scanningScreenData.isRotating.value) {
            return
        }
        _scanningScreenData.isRotating.value = true
        setLoadingText(R.string.rotating_page)
        try {
            val pagePath =
                scannedPage.filePath
            val pageFile = File(pagePath)

            Timber.d("Decoding $pagePath")
            val originalBitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(pagePath) }
            if (originalBitmap == null) {
                Timber.e("Failed to decode bitmap for $pagePath")
                setLoadingText(null)
                _scanningScreenData.isRotating.value = false
                return
            }
            Timber.d("Rotating $pagePath")
            val rotatedBitmap = withContext(Dispatchers.IO) { originalBitmap.rotateBy90() }
            originalBitmap.recycle()

            val editedImageName = pageFile.getEditedImageName()
            val newFile = File(application.filesDir, editedImageName)

            Timber.d("Saving rotated $pagePath")
            withContext(Dispatchers.IO) {
                rotatedBitmap.saveAsJPEG(newFile)
            }
            rotatedBitmap.recycle()
            tmpFileDao.insertAll(
                TempFile(
                    ownerSessionId = sessionID,
                    path = pagePath
                )
            )

            Timber.d("Finished saving rotated $pagePath")

            Timber.d("Updating DB state after rotation")
            scannedPageDao.update(
                scannedPage.copy(
                    rotation = scannedPage.rotation.toggleRotation(),
                    filePath = newFile.absolutePath
                )
            )
        } finally {
            setLoadingText(null)
            _scanningScreenData.isRotating.value = false
        }
    }

    suspend fun saveUpdatedScanSettingsUiData(newData: ScanSettingsEnterableDataV1?) {
        Timber.d("Settings ui data updated $newData")
        sessionDao.updateScanSettingsUiData(sessionID, newData)
    }

    suspend fun setScannerCapabilities(caps: CommonScannerCapabilities) {
        _scanningScreenData.capabilities.value = caps
        val storedSession = sessionDao.getSessionById(sessionID)

        Timber.d("Stored session: $storedSession")

        val updateSettings: suspend (CommonScanSettingsEditor.() -> Unit) -> Unit = { edit ->
            db.useWriterConnection {
                it.immediateTransaction {
                    val oldSession = sessionDao.getSessionById(sessionID)
                        ?: return@immediateTransaction

                    val oldSettings = oldSession.currentScanSettings
                        ?: CommonScanSettings(
                            setting = ScanSettingsMap.empty()
                        )

                    val editor = CommonScanSettingsEditor(
                        capabilities = _scanningScreenData.capabilities.value!!,
                        initial = oldSettings
                    )

                    editor.edit()

                    val newSettings = editor.build()

                    val newSession = oldSession.copy(
                        currentScanSettings = newSettings
                    )

                    Timber.d("Settings updated $newSettings")

                    sessionDao.update(newSession)
                }
            }
        }

        val defaultScanSettingsUIData = ScanSettingsEnterableDataV1(
            caps
        )

        if (storedSession != null) {
            _scanningScreenData.scanSettingsVM.value = getKoin().get {
                parametersOf(
                    session.map { it?.currentScanSettings ?: storedSession.currentScanSettings }
                        .stateIn(viewModelScope, SharingStarted.Lazily, storedSession.currentScanSettings),
                    storedSession.currentSettingsUIData?.copy(capabilities = caps) ?: defaultScanSettingsUIData,
                    updateSettings,
                    viewModelScope
                )
            }
        } else {
            // Try to load saved scan settings first, fallback to defaults if none exist
            val savedSettingsPair = DefaultScanSettingsStore.load(application.applicationContext)
            val (savedSettings, savedSettingsUiState) = savedSettingsPair
            val initialSettings = if (savedSettings != null) {
                val editor = CommonScanSettingsEditor(
                    caps,
                    savedSettings
                )
                editor.build()
            } else {
                // TODO: Use actual defaults here and use swappable default providers
                val editor = CommonScanSettingsEditor(
                    caps,
                    CommonScanSettings(setting = ScanSettingsMap.empty())
                )
                editor.build()
            }

            val savedSettingsUiStateWithCaps = savedSettingsUiState?.copy(capabilities = caps)

            sessionDao.insertAll(Session(sessionID, initialSettings, savedSettingsUiState))

            _scanningScreenData.scanSettingsVM.value = getKoin().get {
                parametersOf(
                    session.map { it?.currentScanSettings ?: initialSettings }
                        .stateIn(viewModelScope, SharingStarted.Lazily, initialSettings),
                    savedSettingsUiStateWithCaps ?: defaultScanSettingsUIData,
                    updateSettings,
                    viewModelScope
                )
            }
        }

        // Subscribe to scan settings ui data changes so that we can save them to the database
        _scanningScreenData.scanSettingsVM.value?.uiState?.onEach {
            saveUpdatedScanSettingsUiData(it)
        }?.launchIn(viewModelScope)
    }

    fun swapTwoPages(page1: ScannedPage, page2: ScannedPage) {
        viewModelScope.launch {
            scannedPageDao.swapPages(page1, page2)
        }
    }

    fun removeScan(page: ScannedPage) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Files.delete(Path(page.filePath))
            }
            scannedPageDao.delete(page)
        }
    }

    fun scan(snackBarScope: CoroutineScope, snackBarHostState: SnackbarHostState) {
        viewModelScope.launch {
            val currentSettings = session.value?.currentScanSettings

            if (currentSettings == null) {
                Timber.e("Could not start scan job. Current scan setttings null")
                return@launch
            }

            if (isScanJobRunning.value) {
                Timber.e("Job still running")
                snackbarErrorRetrievingPage(
                    application.getString(R.string.job_still_running),
                    snackBarScope,
                    application,
                    snackBarHostState,
                    false
                )
                return@launch
            }

            val scanJob = ScanJob(
                Uuid.generateV4(),
                sessionID,
                currentSettings,
                scannerHandle,
                createScannerConnectionSettings()
            )
            scanJobRepo.enqueue(scanJob)
            ScanJobForegroundService.startService(application)
        }
    }

    fun deleteSession(finished: () -> Unit) {
        viewModelScope.launch {
            val filePaths = mutableListOf<String>()
            val tmpPaths = mutableListOf<String>()

            db.useWriterConnection {
                it.immediateTransaction {
                    val scannedPages = scannedPageDao.getAllForSession(sessionID)
                    val tmpFiles = tmpFileDao.getFilesBySessionId(sessionID)

                    filePaths += scannedPages.map { it.filePath }
                    tmpPaths += tmpFiles.map { it.path }

                    sessionDao.deleteById(sessionID)
                }
            }

            withContext(Dispatchers.IO) {
                filePaths.forEach {
                    Files.deleteIfExists(Path(it))
                }
                tmpPaths.forEach {
                    Files.deleteIfExists(Path(it))
                }
            }

            finished()
        }
    }

    fun doPdfExport(context: Context, onError: (String) -> Unit, saveFileLauncher: ActivityResultLauncher<String>? = null) {
        viewModelScope.launch {
            doPdfExportInternal(context, onError, saveFileLauncher)
        }
    }

    private suspend fun doPdfExportInternal(
        context: Context,
        onError: (String) -> Unit,
        saveFileLauncher: ActivityResultLauncher<String>? = null
    ) {
        /*val currentScans = scannedPages.value
        val scannerCapsNullable = scanningScreenData.capabilities
        val scannerCaps = if (scannerCapsNullable == null) {
            onError(application.getString(R.string.scannercapabilities_null))
            return
        } else {
            scannerCapsNullable
        }

        if (currentScans.isEmpty()) {
            onError(application.getString(R.string.no_scans_yet))
            return
        }
        if (isScanJobRunning.value) {
            onError(application.getString(R.string.job_still_running))
            return
        }

        setLoadingText(R.string.exporting)

        val parentDir = File(application.filesDir, "exports")
        if (!parentDir.exists()) {
            parentDir.mkdir()
        }

        val nameRoot = "pdfexport-${
            LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH_mm_ss_SSS"))
        }"

        var pageCounter = 0

        val chunkSize = try {
            application.appSettingsStore.data.first().chunkSizePdfExportOrNull?.value ?: 50
        } catch (exception: Exception) {
            Timber.e("doPdfExport couldn't access app settings. Returning default value: $exception")
            50
        }

        val chunks = currentScans.chunked(chunkSize)

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
                            Timber.d("Added page $pageCounter to PDF")
                        }
                    }
                }
            }
        }

        val digitsNeeded = chunks.size.toString().length
        val tempPdfFiles = List(chunks.size) { index ->
            File(parentDir, "$nameRoot-${index.toString().padStart(digitsNeeded, '0')}.pdf")
        }

        tempPdfFiles.forEach { addTempFile(it) }

        var outputFile: File
        if (tempPdfFiles.size > 1) {
            outputFile = File(parentDir, "$nameRoot.zip")
            zipFiles(tempPdfFiles, outputFile)
            addTempFile(outputFile)
        } else {
            outputFile = tempPdfFiles[0]
        }

        setLoadingText(null)

        val mimetype = if (tempPdfFiles.size > 1) "application/zip" else "application/pdf"

        if (saveFileLauncher == null) {
            val share = Intent(Intent.ACTION_SEND)
            share.type = mimetype
            share.putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(
                    application,
                    "${application.packageName}.fileprovider",
                    outputFile
                )
            )
            context.startActivity(share)
        } else {
            setFileToSave(outputFile)
            saveFileLauncher.launch(outputFile.name)
        }*/
    }

    fun doZipExport(context: Context, onError: (String) -> Unit, saveFileLauncher: ActivityResultLauncher<String>? = null) {
        viewModelScope.launch {
            doZipExportInternal(
                context,
                onError,
                saveFileLauncher
            )
        }
    }

    private suspend fun doZipExportInternal(
        context: Context,
        onError: (String) -> Unit,
        saveFileLauncher: ActivityResultLauncher<String>? = null
    ) {
        val currentScans = scannedPages.value
        if (currentScans.isEmpty()) {
            onError(application.getString(R.string.no_scans_yet))
            return
        }
        if (isScanJobRunning.value) {
            onError(application.getString(R.string.job_still_running))
            return
        }

        setLoadingText(R.string.exporting)

        val parentDir = File(application.filesDir, "exports")
        if (!parentDir.exists()) {
            parentDir.mkdir()
        }

        val name = "zipexport-${
            LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH_mm_ss_SSS"))
        }.zip"

        val zipOutputFile = File(parentDir, name)

        var counter = 0
        val digitsNeeded = currentScans.size.toString().length
        withContext(Dispatchers.IO) {
            zipFiles(
                currentScans.map { File(it.filePath) },
                zipOutputFile,
                {
                    counter++
                    "scan-${counter.toString().padStart(digitsNeeded, '0')}.${it.extension}"
                }
            )
        }

        setLoadingText(null)

        addTempFile(zipOutputFile)

        if (saveFileLauncher == null) {
            val share = Intent(Intent.ACTION_SEND)
            share.type = "application/zip"
            share.putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(
                    application,
                    "${application.packageName}.fileprovider",
                    zipOutputFile
                )
            )
            context.startActivity(share)
        } else {
            setFileToSave(zipOutputFile)
            saveFileLauncher.launch(zipOutputFile.name)
        }
    }

    fun createScannerConnectionSettings() = ScannerConnectionSettings(
        timeout.toULong(),
        timeout.toULong(),
        certificateValidationDisabled,
        withDebugInterceptor
    )

    fun retrieveScannerCapabilities() = viewModelScope.launch {
        val connectionSettings = createScannerConnectionSettings()

        val scannerCapabilities = scannerHandle.scannerCapabilities(connectionSettings)

        if (scannerCapabilities !is ScannerCapabilitiesResult.Success) {
            Timber.e("Error while retrieving ScannerCapabilities: $scannerCapabilities")
            setError("$scannerCapabilities")
            return@launch
        }

        setScannerCapabilities(scannerCapabilities.scannerCapabilities)
    }
}
