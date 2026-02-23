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
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.SnackbarHostState
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import getTrustAllTM
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.ScanRegion
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.esclkt.getInputSourceCaps
import io.github.chrisimx.esclkt.getInputSourceOptions
import io.github.chrisimx.esclkt.inches
import io.github.chrisimx.esclkt.millimeters
import io.github.chrisimx.esclkt.scanRegion
import io.github.chrisimx.esclkt.threeHundredthsOfInch
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.androidservice.ScanJobForegroundService
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.db.entities.Session
import io.github.chrisimx.scanbridge.db.entities.TempFile
import io.github.chrisimx.scanbridge.proto.chunkSizePdfExportOrNull
import io.github.chrisimx.scanbridge.services.ScanJobRepository
import io.github.chrisimx.scanbridge.stores.DefaultScanSettingsStore
import io.github.chrisimx.scanbridge.util.calculateDefaultESCLScanSettingsState
import io.github.chrisimx.scanbridge.util.getEditedImageName
import io.github.chrisimx.scanbridge.util.getMaxResolution
import io.github.chrisimx.scanbridge.util.rotateBy90
import io.github.chrisimx.scanbridge.util.saveAsJPEG
import io.github.chrisimx.scanbridge.util.snackbarErrorRetrievingPage
import io.github.chrisimx.scanbridge.util.zipFiles
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.Url
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    address: Url,
    @InjectedParam
    timeout: UInt,
    @InjectedParam
    withDebugInterceptor: Boolean,
    @InjectedParam
    certificateValidationDisabled: Boolean,
    @InjectedParam
    val sessionID: Uuid,
    val db: ScanBridgeDb,
    application: Application,
    val scanJobRepo: ScanJobRepository
) : AndroidViewModel(application) {
    private val _scanningScreenData =
        ScanningScreenData(
            ESCLRequestClient(
                address,
                HttpClient(CIO) {
                    install(HttpTimeout) {
                        requestTimeoutMillis = timeout.toLong() * 1000
                        connectTimeoutMillis = timeout.toLong() * 1000
                        socketTimeoutMillis = timeout.toLong() * 1000
                    }
                    if (withDebugInterceptor) {
                        install(Logging) {
                            logger = object : Logger {
                                override fun log(message: String) {
                                    Timber.tag("ESCLRequestClient").d(message)
                                }
                            }
                        }
                    }
                    if (certificateValidationDisabled) {
                        engine {
                            https {
                                trustManager = getTrustAllTM().second
                            }
                        }
                    }
                }
            ),
            sessionID
        )
    val scanningScreenData: ImmutableScanningScreenData
        get() = _scanningScreenData.toImmutable()

    private val tmpFileDao = db.tmpFileDao()
    private val scannedPageDao = db.scannedPageDao()
    private val sessionDao = db.sessionDao()

    val tempFiles: StateFlow<List<TempFile>> = tmpFileDao.getFilesFlowBySessionId(sessionID)
        .onEach { Timber.d( "Temp files changed: $it") }
        .stateIn(viewModelScope, SharingStarted.Eagerly,  listOf())

    val scannedPages: StateFlow<List<ScannedPage>> = scannedPageDao.getAllForSessionFlow(sessionID)
        .onEach { Timber.d( "Scanned pages changed: $it") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    val session: StateFlow<Session?> = sessionDao.getSessionFlowById(sessionID)
        .onEach { Timber.d( "Session data changed: $it") }
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

    suspend fun getPageIdx(): Int {
        return sessionDao.getSessionById(sessionID)?.currentPage ?: 0
    }

    fun setCancelling(value: Boolean) {
        scanJobRepo.setCancel(value)
    }

    init {
        session
            .map { it?.currentScanSettings }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach {
                DefaultScanSettingsStore.save(application, it)
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
            tmpFileDao.insertAll(TempFile(
                ownerSessionId = sessionID,
                path = pagePath
            ))

            Timber.d("Finished saving rotated $pagePath")

            Timber.d("Updating DB state after rotation")
            scannedPageDao.update(scannedPage.copy(
                rotation = scannedPage.rotation.toggleRotation(),
                filePath = newFile.absolutePath
            ))
        } finally {
            setLoadingText(null)
            _scanningScreenData.isRotating.value = false
        }
    }

    suspend fun setScannerCapabilities(caps: ScannerCapabilities) {
        _scanningScreenData.capabilities.value = caps
        val storedSession = sessionDao.getSessionById(sessionID)

        Timber.d("Stored session: $storedSession")

        val updateSettings: suspend (ScanSettings.() -> ScanSettings) -> Unit = { lambda ->
            db.withTransaction {
                val oldSession = sessionDao.getSessionById(sessionID) ?: return@withTransaction
                val newSession = oldSession.copy(
                    currentScanSettings = oldSession.currentScanSettings?.lambda()
                )
                sessionDao.update(newSession)
            }
        }

        if (storedSession != null) {
            _scanningScreenData.scanSettingsVM.value = getKoin().get {
                parametersOf(
                    session.map { it?.currentScanSettings ?: storedSession.currentScanSettings }
                        .stateIn(viewModelScope, SharingStarted.Lazily, storedSession.currentScanSettings),
                    ScanSettingsStateData(
                        caps
                    ),
                    updateSettings,
                    viewModelScope
                )
            }
        } else {
            // Try to load saved scan settings first, fallback to defaults if none exist
            val savedSettings = DefaultScanSettingsStore.load(application.applicationContext)
            val initialSettings = if (savedSettings != null) {
                try {
                    // Validate that the saved input source is still supported
                    val supportedInputSources = caps.getInputSourceOptions()
                    val validatedInputSource = if (savedSettings.inputSource != null &&
                        !supportedInputSources.contains(savedSettings.inputSource)
                    ) {
                        Timber.w(
                            "Saved input source ${savedSettings.inputSource} not supported by current scanner, falling back to default"
                        )
                        supportedInputSources.firstOrNull() ?: InputSource.Platen
                    } else {
                        savedSettings.inputSource
                    }

                    // Validate duplex setting - only allow if ADF supports duplex
                    val duplex = if (savedSettings.duplex == true &&
                        (savedSettings.inputSource != InputSource.Feeder || caps.adf?.duplexCaps == null)
                    ) {
                        Timber.w("Duplex not supported with current input source, disabling duplex")
                        false
                    } else {
                        savedSettings.duplex
                    }

                    val selectedInputSourceCaps = caps.getInputSourceCaps(
                        validatedInputSource ?: caps.getInputSourceOptions().first(),
                        duplex ?: false
                    )

                    val intent = if (!selectedInputSourceCaps.supportedIntents.contains(savedSettings.intent)) {
                        selectedInputSourceCaps.supportedIntents.first()
                    } else {
                        savedSettings.intent
                    }

                    val savedScanRegion = savedSettings.scanRegions?.regions?.firstOrNull()
                    val scanRegion = if (savedScanRegion != null) {
                        val storedWidthThreeHOfInch = savedScanRegion.width.value
                        val storedHeightThreeHOfInch = savedScanRegion.height.value

                        val maxWidth = selectedInputSourceCaps.maxWidth.toThreeHundredthsOfInch().value
                        val minWidth = selectedInputSourceCaps.minWidth.toThreeHundredthsOfInch().value

                        val maxHeight = selectedInputSourceCaps.maxHeight.toThreeHundredthsOfInch().value
                        val minHeight = selectedInputSourceCaps.minHeight.toThreeHundredthsOfInch().value

                        val width = storedWidthThreeHOfInch.coerceIn(minWidth..maxWidth)
                        val height = storedHeightThreeHOfInch.coerceIn(minHeight..maxHeight)

                        val xOffset = savedScanRegion.xOffset
                        val yOffset = savedScanRegion.yOffset
                        scanRegion(selectedInputSourceCaps) {
                            this.width = width.threeHundredthsOfInch()
                            this.height = height.threeHundredthsOfInch()
                            this.xOffset = xOffset
                            this.yOffset = yOffset
                        }
                    } else {
                        null
                    }

                    val validatedSettings = savedSettings.copy(
                        inputSource = validatedInputSource,
                        duplex = duplex,
                        intent = intent,
                        scanRegions = scanRegion
                    )

                    validatedSettings
                } catch (e: Exception) {
                    Timber.e(e, "Error applying saved settings, using defaults")
                    caps.calculateDefaultESCLScanSettingsState()
                }
            } else {
                caps.calculateDefaultESCLScanSettingsState()
            }

            sessionDao.insertAll(Session(sessionID, initialSettings))

            _scanningScreenData.scanSettingsVM.value = getKoin().get {
                parametersOf(
                    session.map { it?.currentScanSettings ?: initialSettings }
                        .stateIn(viewModelScope, SharingStarted.Lazily, initialSettings),
                    ScanSettingsStateData(
                        caps
                    ),
                    updateSettings,
                    viewModelScope
                )
            }
        }
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

            val scanJob = io.github.chrisimx.scanbridge.data.model.ScanJob(
                Uuid.generateV4(),
                sessionID,
                currentSettings,
                _scanningScreenData.esclClient
            )
            scanJobRepo.enqueue(scanJob)
            ScanJobForegroundService.startService(application)
        }
    }

    fun deleteSession(finished: () -> Unit) {
        viewModelScope.launch {
            val filePaths = mutableListOf<String>()
            val tmpPaths = mutableListOf<String>()

            db.withTransaction {
                val scannedPages = scannedPageDao.getAllForSession(sessionID)
                val tmpFiles = tmpFileDao.getFilesBySessionId(sessionID)

                filePaths += scannedPages.map { it.filePath }
                tmpPaths += tmpFiles.map { it.path }

                sessionDao.deleteById(sessionID)
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



    fun doPdfExport(
        onError: (String) -> Unit,
        saveFileLauncher: ActivityResultLauncher<String>? = null
    ) {
        viewModelScope.launch {
            doPdfExportInternal(onError, saveFileLauncher)
        }
    }

    private suspend fun doPdfExportInternal(
        onError: (String) -> Unit,
        saveFileLauncher: ActivityResultLauncher<String>? = null
    ) {
        val currentScans = scannedPages.value
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
            application.startActivity(share)
        } else {
            setFileToSave(outputFile)
            saveFileLauncher.launch(outputFile.name)
        }
    }


    fun doZipExport(
        onError: (String) -> Unit,
        saveFileLauncher: ActivityResultLauncher<String>? = null
    ) {
        viewModelScope.launch {
            doZipExportInternal(
                onError,
                saveFileLauncher
            )
        }
    }


    private suspend fun doZipExportInternal(
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
        zipFiles(
            currentScans.map { File(it.filePath) },
            zipOutputFile,
            {
                counter++
                "scan-${counter.toString().padStart(digitsNeeded, '0')}.jpg"
            }
        )

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
            application.startActivity(share)
        } else {
            setFileToSave(zipOutputFile)
            saveFileLauncher.launch(zipOutputFile.name)
        }
    }


    fun retrieveScannerCapabilities() = viewModelScope.launch {
        val esclClient = scanningScreenData.esclClient

        val scannerCapabilitiesResult = esclClient.getScannerCapabilities()

        if (scannerCapabilitiesResult !is ESCLRequestClient.ScannerCapabilitiesResult.Success) {
            Timber.e("Error while retrieving ScannerCapabilities: $scannerCapabilitiesResult")
            setError("$scannerCapabilitiesResult")
            return@launch
        }

        setScannerCapabilities(scannerCapabilitiesResult.scannerCapabilities)
    }
}
