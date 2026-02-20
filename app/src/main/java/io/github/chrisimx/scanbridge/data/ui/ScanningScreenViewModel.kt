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
import android.graphics.BitmapFactory
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import getTrustAllTM
import io.github.chrisimx.esclkt.ESCLHttpCallResult
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.JobState
import io.github.chrisimx.esclkt.ScanJob
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.esclkt.getInputSourceCaps
import io.github.chrisimx.esclkt.getInputSourceOptions
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.data.model.Session
import io.github.chrisimx.scanbridge.data.model.StatelessImmutableScanRegion
import io.github.chrisimx.scanbridge.stores.DefaultScanSettingsStore
import io.github.chrisimx.scanbridge.stores.SessionsStore
import io.github.chrisimx.scanbridge.util.calculateDefaultESCLScanSettingsState
import io.github.chrisimx.scanbridge.util.getEditedImageName
import io.github.chrisimx.scanbridge.util.rotateBy90
import io.github.chrisimx.scanbridge.util.saveAsJPEG
import io.github.chrisimx.scanbridge.util.snackbarErrorRetrievingPage
import io.github.chrisimx.scanbridge.util.toJobStateString
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.Url
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.get
import org.koin.mp.KoinPlatform.getKoin
import timber.log.Timber

class ScanningScreenViewModel(
    application: Application,
    address: Url,
    timeout: UInt,
    withDebugInterceptor: Boolean,
    certificateValidationDisabled: Boolean,
    sessionID: String
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

    fun addTempFile(file: File) {
        _scanningScreenData.createdTempFiles.add(file)
        saveSessionFile()
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

    fun removeTempFile(index: Int) {
        _scanningScreenData.createdTempFiles.removeAt(index)
        saveSessionFile()
    }

    fun setLoadingText(stringRes: Int?) {
        _scanningScreenData.stateProgressStringRes.value = stringRes
    }

    fun scrollToPage(pageNr: Int, scope: CoroutineScope) {
        scope.launch {
            _scanningScreenData.pagerState.animateScrollToPage(
                scanningScreenData.currentScansState.size
            )
        }
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

    fun setScanJobRunning(value: Boolean) {
        _scanningScreenData.scanJobRunning.value = value
    }

    fun setScanJobCancelling(value: Boolean) {
        _scanningScreenData.scanJobCancelling.value = value
    }

    fun setError(error: String?, titleResource: Int? = null, errorIcon: Int? = null) {
        _scanningScreenData.error.value = ErrorDescription(
            titleResource,
            errorIcon,
            error
        )
    }

    fun rotateCurrentPage() {
        if (_scanningScreenData.stateCurrentScans.isEmpty() || _scanningScreenData.isRotating.value) {
            return
        }
        _scanningScreenData.isRotating.value = true
        setLoadingText(R.string.rotating_page)
        try {
            val currentScans = scanningScreenData.currentScansState
            val currentPagePath =
                currentScans[scanningScreenData.pagerState.currentPage].filePath
            val currentPageFile = File(currentPagePath)

            Timber.d("Decoding $currentPagePath")
            val originalBitmap = BitmapFactory.decodeFile(currentPagePath)
            if (originalBitmap == null) {
                Timber.e("Failed to decode bitmap for $currentPagePath")
                setLoadingText(null)
                _scanningScreenData.isRotating.value = false
                return
            }
            Timber.d("Rotating $currentPagePath")
            val rotatedBitmap = originalBitmap.rotateBy90()
            originalBitmap.recycle()

            val editedImageName = currentPageFile.getEditedImageName()
            val newFile = File(application.filesDir, editedImageName)

            Timber.d("Saving rotated $currentPagePath")
            rotatedBitmap.saveAsJPEG(newFile)
            rotatedBitmap.recycle()

            Timber.d("Finished saving rotated $currentPagePath")

            val index = scanningScreenData.pagerState.currentPage
            val scanSettings = currentScans[index].originalScanSettings
            val priorRotation = currentScans[index].rotation
            Timber.d("Updating UI state after rotation")
            removeScanAtIndex(index)
            addTempFile(currentPageFile)
            addScanAtIndex(newFile.absolutePath, scanSettings, priorRotation.toggleRotation(), index)
        } finally {
            setLoadingText(null)
            _scanningScreenData.isRotating.value = false
        }
    }

    fun setScannerCapabilities(caps: ScannerCapabilities) {
        _scanningScreenData.capabilities.value = caps
        val storedSessionResult = loadSessionFile(caps)

        storedSessionResult.onFailure {
            setError(
                it.toString(),
                R.string.loading_previous_session_failed,
                R.drawable.rounded_warning_24
            )
            return
        }

        val storedSession = storedSessionResult.getOrThrow()

        if (storedSession != null) {
            scanningScreenData.currentScansState.addAll(storedSession.scannedPages)
            _scanningScreenData.createdTempFiles.addAll(storedSession.tmpFiles.map { File(it) })
            _scanningScreenData.scanSettingsVM.value = getKoin().get{
                parametersOf(
                    ScanSettingsComposableData(
                        storedSession.scanSettings ?: caps.calculateDefaultESCLScanSettingsState(),
                        caps
                    ),
                    {
                        saveScanSettings()
                        saveSessionFile()
                    },
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
                        savedSettings.inputSource ?: InputSource.Platen,
                        savedSettings.duplex ?: false
                    )

                    val intent = if (!selectedInputSourceCaps.supportedIntents.contains(savedSettings.intent)) {
                        selectedInputSourceCaps.supportedIntents.first()
                    } else {
                        savedSettings.intent
                    }

                    val scanRegion = if (savedSettings.scanRegions != null) {
                        val storedWidthThreeHOfInch = savedSettings.scanRegions.width.toDoubleOrNull()
                        val storedHeightThreeHOfInch = savedSettings.scanRegions.height.toDoubleOrNull()

                        val maxWidth = selectedInputSourceCaps.maxWidth.toMillimeters().value
                        val minWidth = selectedInputSourceCaps.minWidth.toMillimeters().value

                        val maxHeight = selectedInputSourceCaps.maxHeight.toMillimeters().value
                        val minHeight = selectedInputSourceCaps.minHeight.toMillimeters().value

                        val width = if (storedWidthThreeHOfInch != null &&
                            (storedWidthThreeHOfInch > maxWidth || storedWidthThreeHOfInch < minWidth)
                        ) {
                            "max"
                        } else {
                            savedSettings.scanRegions.width
                        }
                        val height = if (storedHeightThreeHOfInch != null &&
                            (storedHeightThreeHOfInch > maxHeight || storedHeightThreeHOfInch < minHeight)
                        ) {
                            "max"
                        } else {
                            savedSettings.scanRegions.height
                        }
                        val xOffset = savedSettings.scanRegions.xOffset
                        val yOffset = savedSettings.scanRegions.yOffset
                        StatelessImmutableScanRegion(height, width, xOffset, yOffset)
                    } else {
                        null
                    }

                    val validatedSettings = savedSettings.copy(
                        inputSource = validatedInputSource,
                        duplex = duplex,
                        intent = intent,
                        scanRegions = scanRegion
                    )

                    validatedSettings.toESCLKtScanSettings(selectedInputSourceCaps)
                } catch (e: Exception) {
                    Timber.e(e, "Error applying saved settings, using defaults")
                    caps.calculateDefaultESCLScanSettingsState()
                }
            } else {
                caps.calculateDefaultESCLScanSettingsState()
            }

            _scanningScreenData.scanSettingsVM.value = getKoin().get {
                parametersOf(
                    ScanSettingsComposableData(
                        initialSettings,
                        caps
                    ),
                    {
                        saveScanSettings()
                        saveSessionFile()
                    },
                    viewModelScope
                )
            }
            val sessionFile = application.applicationInfo.dataDir + "/files/" + scanningScreenData.sessionID + ".session"
            addTempFile(File(sessionFile))
            saveSessionFile()
        }
    }

    fun addScan(path: String, settings: ScanSettings, rotation: ScanRelativeRotation) {
        _scanningScreenData.stateCurrentScans.add(ScanMetadata(path, settings, rotation))
        saveSessionFile()
    }
    fun addScanAtIndex(path: String, settings: ScanSettings, rotation: ScanRelativeRotation, index: Int) {
        _scanningScreenData.stateCurrentScans.add(index, ScanMetadata(path, settings, rotation))
        saveSessionFile()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun saveSessionFile(): String {
        val currentSessionState = Session(
            scanningScreenData.sessionID,
            scanningScreenData.currentScansState.toList(),
            scanningScreenData.scanSettingsVM?.uiState?.value?.scanSettings,
            scanningScreenData.createdTempFiles.map { it.absolutePath }
        )
        return SessionsStore.saveSession(currentSessionState, application, scanningScreenData.sessionID)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun loadSessionFile(caps: ScannerCapabilities): Result<Session?> =
        SessionsStore.loadSession(application, scanningScreenData.sessionID, caps)

    fun swapTwoPages(index1: Int, index2: Int) {
        if (index1 < 0 ||
            index1 >= _scanningScreenData.stateCurrentScans.size ||
            index2 < 0 ||
            index2 >= _scanningScreenData.stateCurrentScans.size
        ) {
            return
        }
        val tmp = _scanningScreenData.stateCurrentScans[index1]
        _scanningScreenData.stateCurrentScans[index1] =
            _scanningScreenData.stateCurrentScans[index2]
        _scanningScreenData.stateCurrentScans[index2] = tmp
        saveSessionFile()
    }

    fun removeScanAtIndex(index: Int) {
        if (index < 0 || index >= _scanningScreenData.stateCurrentScans.size) {
            return
        }
        _scanningScreenData.stateCurrentScans.removeAt(index)
        saveSessionFile()
    }

    fun saveScanSettings() {
        scanningScreenData.scanSettingsVM?.uiState?.value?.scanSettings?.let { settings ->
            DefaultScanSettingsStore.save(application.applicationContext, settings)
            Timber.d("Scan settings saved to persistent storage")
        }
    }

    fun clearSavedScanSettings() {
        DefaultScanSettingsStore.clear(application.applicationContext)
        Timber.d("Saved scan settings cleared")
    }

    fun scan(snackBarScope: CoroutineScope, snackBarHostState: SnackbarHostState) {
        viewModelScope.launch {
            doScan(snackBarScope, snackBarHostState)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun doScan(snackBarScope: CoroutineScope, snackBarHostState: SnackbarHostState) {
        if (scanningScreenData.scanJobRunning) {
            Timber.e("Job still running")
            snackbarErrorRetrievingPage(
                application.getString(R.string.job_still_running),
                snackBarScope,
                application,
                snackBarHostState,
                false
            )
            return
        }

        val currentScanSettings =
            scanningScreenData.scanSettingsVM!!.uiState.value.scanSettings

        val esclRequestClient = _scanningScreenData.esclClient

        setScanJobRunning(true)
        setScanJobCancelling(false)
        scrollToPage(
            scope = snackBarScope,
            pageNr = scanningScreenData.currentScansState.size + 1
        )

        if (abortIfCancelling()) return

        Timber.d("Creating scan job. eSCLKt scan settings: $currentScanSettings")
        val job =
            esclRequestClient.createJob(currentScanSettings)
        Timber.d("Creation request done. Result: $job")
        if (job !is ESCLRequestClient.ScannerCreateJobResult.Success) {
            Timber.e("Job creation failed. Result: $job")
            setScanJobRunning(false)
            snackbarErrorRetrievingPage(job.toString(), snackBarScope, application, snackBarHostState)
            return
        }
        val jobResult = job.scanJob

        if (abortIfCancelling(jobResult)) return

        var polling = false

        while (true) {
            if (polling) {
                for (retries in 0..60) {
                    if (abortIfCancelling(jobResult)) return
                    val status = jobResult.getJobStatus()
                    val isRunning = status?.jobState == JobState.Processing || status?.jobState == JobState.Pending
                    val imagesToTransfer = status?.imagesToTransfer
                    Timber.d(
                        "Polling job status. Retry: $retries Result: $status imagesToTransfer: $imagesToTransfer isRunning: $isRunning"
                    )
                    if (!isRunning) {
                        Timber.d("Job is reported to be not running anymore. jobRunning = false")

                        val deleteResult = jobResult.cancel()
                        Timber.d("Cancelling job after (a likely) failure: $deleteResult")

                        setScanJobRunning(false)
                        scrollToPage(
                            scope = snackBarScope,
                            pageNr = scanningScreenData.currentScansState.size
                        )
                        if (status?.jobState != JobState.Completed) {
                            val jobStateString = status?.jobState.toJobStateString(application)
                            Timber.w("Job info doesn't indicate completion: $jobStateString")
                            snackBarScope.launch {
                                snackBarHostState.showSnackbar(
                                    application.getString(
                                        R.string.no_further_pages,
                                        jobStateString
                                    ),
                                    withDismissAction = true
                                )
                            }
                        }
                        return
                    }
                    if (imagesToTransfer != null && imagesToTransfer > 0u) {
                        Timber.d("There seem to be images to transfer. Breaking out of polling loop")
                        break
                    }
                    delay(1000)
                }
            }

            if (abortIfCancelling(jobResult)) return

            Timber.d("Retrieving next page")
            val nextPage = jobResult.retrieveNextPage()
            Timber.d("Next page result: $nextPage")
            val status = jobResult.getJobStatus()
            Timber.d("Retrieved job info: $status")
            val jobStateString = status?.jobState.toJobStateString(application)
            Timber.d("Job info as human readable: $jobStateString")
            when (nextPage) {
                is ESCLRequestClient.ScannerNextPageResult.NoFurtherPages -> {
                    Timber.d("Next page result is seen as no further pages. jobRunning = false")
                    setScanJobRunning(false)
                    scrollToPage(
                        scope = snackBarScope,
                        pageNr = scanningScreenData.currentScansState.size
                    )
                    if (status?.jobState != JobState.Completed) {
                        Timber.w("Job info doesn't indicate completion: $jobStateString")
                        snackBarScope.launch {
                            snackBarHostState.showSnackbar(
                                application.getString(
                                    R.string.no_further_pages,
                                    jobStateString
                                ),
                                withDismissAction = true
                            )
                        }
                    }
                    val deletionResult = jobResult.cancel()
                    Timber.d("Cancelling job after no further pages is reported: $deletionResult")
                    return
                }

                is ESCLRequestClient.ScannerNextPageResult.RequestFailure -> {
                    if (nextPage.exception !is ESCLHttpCallResult.Error.HttpError) {
                        reportErrorWhileScanning(nextPage, snackBarScope, application, snackBarHostState, jobResult)
                        return
                    }
                    val error = nextPage.exception as ESCLHttpCallResult.Error.HttpError

                    if (status?.jobState == JobState.Completed) {
                        Timber.d("Job info indicates completion but response was not 404: $jobStateString")
                        setScanJobRunning(false)
                        scrollToPage(
                            scope = snackBarScope,
                            pageNr = scanningScreenData.currentScansState.size
                        )
                        snackBarScope.launch {
                            snackBarHostState.showSnackbar(
                                application.getString(
                                    R.string.no_further_pages,
                                    jobStateString
                                ),
                                withDismissAction = true
                            )
                        }
                        val deletionResult = jobResult.cancel()
                        Timber.d("Cancelling job after non-standard completion: $deletionResult")
                        return
                    } else {
                        Timber.e("Not successful code while retrieving next page: $nextPage")
                        if (error.code == 503) {
                            // Retry with polling
                            Timber.d("503 error received. Retrying with polling")
                            polling = true
                            continue
                        } else {
                            setScanJobRunning(false)
                            scrollToPage(
                                scope = snackBarScope,
                                pageNr = scanningScreenData.currentScansState.size
                            )
                            snackbarErrorRetrievingPage(
                                nextPage.toString(),
                                snackBarScope,
                                application,
                                snackBarHostState
                            )
                            val deletionResult = jobResult.cancel()
                            Timber.d("Cancelling job after not successful response while trying to retrieve page: $deletionResult")
                            return
                        }
                    }
                }

                is ESCLRequestClient.ScannerNextPageResult.Success -> {
                }

                else -> {
                    reportErrorWhileScanning(nextPage, snackBarScope, application, snackBarHostState, jobResult)
                    return
                }
            }
            Timber.d("Received page. Copying to file")
            var filePath: Path
            while (true) {
                val scanPageFile = "scan-" + Uuid.random().toString() + ".jpg"
                val file = File(application.filesDir, scanPageFile)
                file.exists().let {
                    if (!it) {
                        filePath = file.toPath()
                        break
                    }
                }
            }

            Timber.d("Scan page file created: $filePath")

            try {
                withContext(Dispatchers.IO) {
                    Files.copy(nextPage.page.data.inputStream(), filePath)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error while copying received image to file. Aborting!")
                setScanJobRunning(false)
                snackbarErrorRetrievingPage(
                    application.getString(
                        R.string.error_while_copying_received_image_to_file,
                        e.message
                    ),
                    snackBarScope,
                    application,
                    snackBarHostState
                )
                val deletionResult = jobResult.cancel()
                Timber.d("Cancelling job after error while trying to copy received page to file: $deletionResult")
                return
            }

            val imageBitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(filePath.toString())?.asImageBitmap()
            }

            if (imageBitmap == null) {
                Timber.e("Couldn't decode received image as Bitmap. Aborting!")
                snackbarErrorRetrievingPage(
                    application.getString(R.string.couldn_t_decode_received_image, jobStateString),
                    snackBarScope,
                    application,
                    snackBarHostState
                )
                filePath.toFile().delete()
                val deletionResult = jobResult.cancel()
                Timber.d("Cancelling job after error while trying to decode received page as bitmap: $deletionResult")
                return
            }
            addScan(filePath.toString(), currentScanSettings, ScanRelativeRotation.Original)
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

    private suspend fun abortIfCancelling(scanJob: ScanJob? = null): Boolean = if (scanningScreenData.scanJobCancelling) {
        Timber.d("Scan job cancelling is set. Aborting, canceling job if possible. scanJob: $scanJob")
        scanJob?.cancel()
        setScanJobRunning(false)
        setScanJobCancelling(false)
        true
    } else {
        false
    }

    private suspend fun reportErrorWhileScanning(
        nextPage: ESCLRequestClient.ScannerNextPageResult,
        scope: CoroutineScope,
        context: Context,
        snackbarHostState: SnackbarHostState,
        jobResult: ScanJob
    ) {
        Timber.e("Error while retrieving next page: $nextPage")
        snackbarErrorRetrievingPage(
            nextPage.toString(),
            scope,
            context,
            snackbarHostState
        )
        setScanJobRunning(false)
        val deletionResult = jobResult.cancel()
        Timber.d("Cancelling job after error while trying to retrieve page: $deletionResult")
    }
}
