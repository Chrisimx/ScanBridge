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

package io.github.chrisimx.scanbridge.scanning

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.anyscan.CommonScanSettingsEditor
import io.github.chrisimx.anyscan.CommonScannerCapabilities
import io.github.chrisimx.anyscan.ScanSettingsMap
import io.github.chrisimx.scanbridge.ScanSettingsComposableStateHolder
import io.github.chrisimx.scanbridge.appsettings.AppSettingsRepository
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.LastUsedScanSettings
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.db.entities.Session
import io.github.chrisimx.scanbridge.db.entities.TempFile
import io.github.chrisimx.scanbridge.export.ExportCapabilitiesProvider
import io.github.chrisimx.scanbridge.export.ExportModuleManager
import io.github.chrisimx.scanbridge.export.ExportModuleType
import io.github.chrisimx.scanbridge.initialscansettings.InitialScanSettingsProvider
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV1
import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.model.scannerCapabilities
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.protocol.ScannerCapabilitiesResult
import io.github.chrisimx.scanbridge.protocol.ScannerConnectionSettings
import io.github.chrisimx.scanbridge.savelastusedscansettings.LastUsedScanSettingsRepository
import io.github.chrisimx.scanbridge.imagerotation.RotateScanUseCase
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.path
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.annotation.InjectedParam
import org.koin.core.scope.Scope

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
    val scanJobRepo: ScanJobRepository,
    val appSettingsRepository: AppSettingsRepository,
    val lastUsedScanSettingsRepo: LastUsedScanSettingsRepository,
    val initialScanSettingsProvider: InitialScanSettingsProvider,
    val startScanUseCase: StartScanUseCase,
    val koinScope: Scope,
    val loggerFactory: ScanBridgeLoggerFactory,
    val exportModuleManager: ExportModuleManager,
    exportCapabilitiesProvider: ExportCapabilitiesProvider,
    val deleteScanUseCase: DeleteScanUseCase,
    val deleteSessionUseCase: DeleteSessionUseCase,
    val rotatePageUseCase: RotateScanUseCase,
    val swapPagesUseCase: SwapPagesUseCase,
    val exportAllPagesUseCase: ExportAllPagesUseCase
) : ViewModel() {
    private val logger = loggerFactory.withClass(this::class)

    private val tmpFileDao = db.tmpFileDao()
    private val scannedPageDao = db.scannedPageDao()
    private val sessionDao = db.sessionDao()
    
    private val _confirmLeaveDialogShown = MutableStateFlow(false)
    val confirmLeaveDialogShown: StateFlow<Boolean> = _confirmLeaveDialogShown.asStateFlow()

    private val _confirmPageDeleteDialogShown = MutableStateFlow(false)
    val confirmPageDeleteDialogShown: StateFlow<Boolean> = _confirmPageDeleteDialogShown.asStateFlow()

    private val _fatalError = MutableStateFlow<FatalErrorDescription?>(null)
    val fatalError: StateFlow<FatalErrorDescription?> = _fatalError.asStateFlow()

    private val _scanSettingsVM = MutableStateFlow<ScanSettingsComposableStateHolder?>(null)
    val scanSettingsVM: StateFlow<ScanSettingsComposableStateHolder?> = _scanSettingsVM.asStateFlow()

    private val _capabilities = MutableStateFlow<CommonScannerCapabilities?>(null)
    val capabilities: StateFlow<CommonScannerCapabilities?> = _capabilities.asStateFlow()

    private val _scanSettingsMenuOpen = MutableStateFlow(false)
    val scanSettingsMenuOpen: StateFlow<Boolean> = _scanSettingsMenuOpen.asStateFlow()

    private val _showExportOptions = MutableStateFlow(false)
    val showExportOptions: StateFlow<Boolean> = _showExportOptions.asStateFlow()

    private val _showSaveOptions = MutableStateFlow(false)
    val showSaveOptions: StateFlow<Boolean> = _showSaveOptions.asStateFlow()

    private val _isRotating = MutableStateFlow(false)
    val isRotating: StateFlow<Boolean> = _isRotating.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    val tempFiles: StateFlow<List<TempFile>> = tmpFileDao.getFilesFlowBySessionId(sessionID)
        .onEach { logger.debug { "Temp files changed: $it" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    val scannedPages: StateFlow<List<ScannedPage>> = scannedPageDao.getAllForSessionFlow(sessionID)
        .onEach { logger.debug { "Scanned pages changed: $it" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    val session: StateFlow<Session?> = sessionDao.getSessionFlowById(sessionID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val availableExportModules = exportModuleManager.getAllExportModulesFlow()
        .map { exportModuleList -> exportModuleList.map { it.type } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    val readyForScans = combine(scanSettingsVM, capabilities) { scanSettingsVM, capabilities ->
        scanSettingsVM != null && capabilities != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isScanJobRunning = scanJobRepo.isJobRunning
    val isScanJobCancelling = scanJobRepo.shouldCancel

    val currentPageIdx = session.map {
        it?.currentPage ?: 0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val currentPage = combine(currentPageIdx, scannedPages) { pageIdx, pages ->
        pages.getOrNull(pageIdx)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val saveSupported = FileSaveType.Save in exportCapabilitiesProvider.supportedFileSaveTypes
    val shareSupported = FileSaveType.Share in exportCapabilitiesProvider.supportedFileSaveTypes

    private val _errorStream = MutableSharedFlow<ScanningScreenError>()
    val errorStream = _errorStream.asSharedFlow()

    private val _exportQueue = Channel<ExportEvent>(
        capacity = Channel.BUFFERED
    )

    val exportQueue = _exportQueue.receiveAsFlow()

    private suspend fun addTempFile(file: PlatformFile) {
        val tmpFile = TempFile(Uuid.generateV4(), sessionID, file.path)
        tmpFileDao.insertAll(tmpFile)
    }

    init {
        retrieveScannerCapabilities()
    }

    suspend fun getPageIdx(): Int = sessionDao.getSessionById(sessionID)?.currentPage ?: 0

    fun setCancelling(value: Boolean) {
        scanJobRepo.setCancel(value)
    }

    init {
        session
            .filter { it?.currentScanSettings != null && it.currentSettingsUIData != null }
            .map {
                LastUsedScanSettings(
                    lastUsedScanSettings = it!!.currentScanSettings!!,
                    lastUsedScanSettingsEnterable = it.currentSettingsUIData!!
                )
            }
            .distinctUntilChanged()
            .onEach { newLastUsedScanSettings ->
                lastUsedScanSettingsRepo.setLastUsedScanSettings(
                    newLastUsedScanSettings
                )
            }
            .launchIn(viewModelScope.coroutineScope)
    }

    fun setPageIdx(idx: Int) {
        viewModelScope.launch {
            sessionDao.updateCurrentPage(sessionID, idx)
        }
    }

    fun setShowExportOptionsPopup(show: Boolean) {
        _showExportOptions.value = show
    }

    fun setShowSaveOptionsPopup(show: Boolean) {
        _showSaveOptions.value = show
    }

    fun setScanSettingsMenuOpen(value: Boolean) {
        _scanSettingsMenuOpen.value = value
    }


    private fun setFatalError(errorType: FatalScanningScreenError?, error: String?) {
        _fatalError.value = FatalErrorDescription(errorType, error)
    }

    fun rotatePage(pageId: Uuid) {
        viewModelScope.launch {
            if (!_isRotating.compareAndSet(expect = false, update = true)) {
                return@launch
            }

            try {
                rotatePageUseCase.rotateScan(pageId)
            } finally {
                _isRotating.update { false }
            }
        }
    }

    fun onSaveLocationSelected(exportEvent: ExportEvent, platformFile: PlatformFile?) {
        if (platformFile == null) {
            return
        }

        viewModelScope.launch {
            copyExportToPlatformFile(exportEvent, platformFile)
        }
    }

    private fun copyExportToPlatformFile(exportEvent: ExportEvent, destination: PlatformFile) = viewModelScope.launch {
        val source = exportEvent.exportedFile
        withContext(Dispatchers.IO) {
            source.copyTo(destination)
        }
    }

    suspend fun saveUpdatedScanSettingsUiData(newData: ScanSettingsEnterableDataV1?) {
        logger.debug { "Settings ui data updated $newData" }
        sessionDao.updateScanSettingsUiData(sessionID, newData)
    }

    suspend fun setScannerCapabilities(caps: CommonScannerCapabilities) {
        _capabilities.value = caps
        val storedSession = sessionDao.getSessionById(sessionID)

        logger.debug { "Stored session: $storedSession" }

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
                        capabilities = caps,
                        initial = oldSettings
                    )

                    editor.edit()

                    val newSettings = editor.build()

                    val newSession = oldSession.copy(
                        currentScanSettings = newSettings
                    )

                    logger.debug {"Settings updated $newSettings" }

                    sessionDao.update(newSession)
                }
            }
        }

        val localScanSettingsVM: ScanSettingsComposableStateHolder

        if (storedSession != null) {
            localScanSettingsVM = ScanSettingsComposableStateHolder(
                MutableStateFlow(caps).asStateFlow(),
                session.map { it?.currentScanSettings ?: storedSession.currentScanSettings!! }
                    .stateIn(
                        viewModelScope,
                        SharingStarted.Lazily,
                        storedSession.currentScanSettings!!
                    ),
                storedSession.currentSettingsUIData?.copy(),
                updateSettings,
                null,
                viewModelScope.coroutineScope,
                koinScope.get(),
                koinScope.get(),
                koinScope.get(),
                koinScope.get()
            )

            _scanSettingsVM.value = localScanSettingsVM
        } else {
            // Try to load saved scan settings first, fallback to defaults if none exist
            val savedSettings = lastUsedScanSettingsRepo.getLastUsedScanSettings()

            val lastUsedScanSettings = savedSettings?.lastUsedScanSettings
            val lastUsedScanSettingsEnterable = savedSettings?.lastUsedScanSettingsEnterable

            val initialSettings = if (lastUsedScanSettings != null) {
                val editor = CommonScanSettingsEditor(
                    caps,
                    lastUsedScanSettings
                )
                editor.build()
            } else {
                val editor = CommonScanSettingsEditor(
                    caps,
                    CommonScanSettings(setting = ScanSettingsMap.empty())
                )
                initialScanSettingsProvider.applyDefaults(editor, caps)
                editor.build()
            }

            sessionDao.insertAll(
                Session(
                    sessionID,
                    initialSettings,
                    lastUsedScanSettingsEnterable
                )
            )

            localScanSettingsVM = ScanSettingsComposableStateHolder(
                MutableStateFlow(caps).asStateFlow(),
                session.map { it?.currentScanSettings ?: initialSettings }
                    .stateIn(viewModelScope, SharingStarted.Lazily, initialSettings),
                lastUsedScanSettingsEnterable,
                updateSettings,
                { initialSettings },
                viewModelScope.coroutineScope,
                koinScope.get(),
                koinScope.get(),
                koinScope.get(),
                koinScope.get()
            )

            _scanSettingsVM.value = localScanSettingsVM
        }

        // Subscribe to scan settings ui data changes so that we can save them to the database
        localScanSettingsVM.uiState.onEach {
            saveUpdatedScanSettingsUiData(it)
        }.launchIn(viewModelScope.coroutineScope)
    }

    fun swapTwoPages(page1: ScannedPage, page2: ScannedPage) {
        viewModelScope.launch {
            swapPagesUseCase(page1, page2)
        }
    }

    fun pageDeletionRequested() {
        _confirmPageDeleteDialogShown.value = true
    }

    fun onConfirmPageDeletion(page: ScannedPage) {
        viewModelScope.launch {
            try {
                deleteScanUseCase.deleteScan(page)
            } catch (e: Exception) {
                sendErrorToUI(ScanningScreenError.DeletionError(e))
            } finally {
                _confirmPageDeleteDialogShown.value = false
            }
        }
    }

    fun onConfirmPageDeletionDismissed() {
        _confirmPageDeleteDialogShown.value = false
    }

    fun leaveRequested() {
        _confirmLeaveDialogShown.value = true
    }

    fun onLeaveDismissed() {
        _confirmLeaveDialogShown.value = false
    }

    fun onLeaveConfirmed(onFinish: () -> Unit) {
        viewModelScope.launch {
            deleteSessionUseCase.deleteSession(sessionID)
            _confirmLeaveDialogShown.value = false
            onFinish()
        }
    }

    private suspend fun sendErrorToUI(error: ScanningScreenError) {
        _errorStream.emit(error)
    }

    fun exportAllPages(exportModuleType: ExportModuleType, saveType: FileSaveType) = viewModelScope.launch {
        if (!_isExporting.compareAndSet(expect = false, update = true)) {
            return@launch
        }

        try {
            when (val result = exportAllPagesUseCase(exportModuleType, sessionID)) {
                ExportAllPagesResult.ExportModuleNotFound -> {
                    sendErrorToUI(ScanningScreenError.ExportModuleNotFound)
                }

                ExportAllPagesResult.NoPages -> {
                    sendErrorToUI(ScanningScreenError.NoPagesScannedYet)
                }

                is ExportAllPagesResult.ExportFailed -> {
                    sendErrorToUI(ScanningScreenError.ExportError(result.error))
                }

                is ExportAllPagesResult.Success -> {
                    _exportQueue.send(
                        ExportEvent(result.exportedFile, saveType)
                    )
                }
            }
        } catch (e: Exception) {
            sendErrorToUI(ScanningScreenError.ExportError(e))
        } finally {
            _isExporting.update { false }
        }
    }

    fun scan() {
        viewModelScope.launch {
            val currentSettings = session.value?.currentScanSettings

            if (currentSettings == null) {
                logger.error { "Could not start scan job. Current scan settings null" }
                return@launch
            }

            if (isScanJobRunning.value) {
                logger.error { "Job still running" }
                sendErrorToUI(ScanningScreenError.JobStillRunning)
                return@launch
            }

            startScanUseCase.startScan(
                ownerSessionId = sessionID,
                scannerHandle = scannerHandle,
                scanSettings = currentSettings,
                connectionSettings = scannerConnectionSettings()
            )
        }
    }

    fun scannerConnectionSettings() = ScannerConnectionSettings(
        timeout.toULong(),
        timeout.toULong(),
        certificateValidationDisabled,
        withDebugInterceptor
    )

    fun retrieveScannerCapabilities() = viewModelScope.launch {
        val connectionSettings = scannerConnectionSettings()

        val scannerCapabilities = scannerHandle.scannerCapabilities(connectionSettings)

        if (scannerCapabilities !is ScannerCapabilitiesResult.Success) {
            logger.error { "Error while retrieving ScannerCapabilities: $scannerCapabilities" }
            setFatalError(FatalScanningScreenError.ScannerCapsRetrieval, "$scannerCapabilities")
            return@launch
        }

        setScannerCapabilities(scannerCapabilities.scannerCapabilities)
    }
}

sealed class ScanningScreenError {
    object JobStillRunning : ScanningScreenError()
    object ExportModuleNotFound : ScanningScreenError()
    object NoPagesScannedYet : ScanningScreenError()
    class ExportError(val error: Throwable) : ScanningScreenError()

    class DeletionError(val error: Throwable) : ScanningScreenError()
}


enum class FatalScanningScreenError {
    ScannerCapsRetrieval,
}

data class FatalErrorDescription(val errorType: FatalScanningScreenError?, val text: String?)

enum class FileSaveType {
    Share,
    Save
}

data class ExportEvent(val exportedFile: PlatformFile, val saveType: FileSaveType, val exportId: Uuid = Uuid.generateV4())
