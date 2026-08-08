package io.github.chrisimx.scanbridge.appsettings

import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import io.github.chrisimx.anyscan.CommonScanSettingsEditor
import io.github.chrisimx.scanbridge.ScanSettingsComposableStateHolder
import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.db.entities.AppSettings
import io.github.chrisimx.scanbridge.initialscansettings.INITIAL_SCAN_SETTINGS_AVAILABLE_OPTIONS_CAPS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.koin.core.scope.Scope

class AppSettingsViewModel(val appSettingsRepository: AppSettingsRepository, buildInfoProvider: BuildInfoProvider, koinScope: Scope) :
    ViewModel() {
    val versionName = buildInfoProvider.versionName
    val versionCode = buildInfoProvider.versionCode
    val gitCommitHash = buildInfoProvider.commit
    val edition = buildInfoProvider.edition
    val isDebugBuild = buildInfoProvider.isDebugBuild

    val disableCertChecks = appSettingsRepository.getDisableCertValidationFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings().disableCertValidation)
    val rememberScanSettings = appSettingsRepository.getRememberScanSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings().rememberScanSettings)
    val scanningResponseTimeout = appSettingsRepository.getScanningResponseTimeoutFlow()
        .map { it.toUInt() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings().scanningResponseTimeoutInS.toUInt())
    val pdfChunkSize = appSettingsRepository.getPdfExportChunkSizeFlow()
        .map { it.toUInt() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings().chunkSizeForPDFExport.toUInt())
    val writeDebugLogs = appSettingsRepository.getWriteDebugLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings().writeDebugLogs)

    private val _isPreferredInitialScanSettingsMenuVisible = MutableStateFlow(false)
    val isPreferredInitialScanSettingsMenuVisible = _isPreferredInitialScanSettingsMenuVisible.asStateFlow()

    fun setPreferredInitialScanSettingsMenuVisibility(newVisibility: Boolean) {
        _isPreferredInitialScanSettingsMenuVisible.value = newVisibility
    }

    private val preferredInitialScanSettings = appSettingsRepository.getPreferredInitialScanSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings().preferredInitialScanSettings)

    val prefInitialScanSettingsUIStateHolder = ScanSettingsComposableStateHolder(
        MutableStateFlow(INITIAL_SCAN_SETTINGS_AVAILABLE_OPTIONS_CAPS).asStateFlow(),
        preferredInitialScanSettings,
        null,
        ::updatePreferredInitialScanSettings,
        appSettingsRepository::getPreferredInitialScanSettings,
        viewModelScope.coroutineScope,
        koinScope.get(),
        koinScope.get(),
        koinScope.get(),
        koinScope.get()
    )

    private suspend fun updatePreferredInitialScanSettings(updateOperation: CommonScanSettingsEditor.() -> Unit) {
        appSettingsRepository.updatePreferredInitialScanSettings {
            val editor = CommonScanSettingsEditor(
                INITIAL_SCAN_SETTINGS_AVAILABLE_OPTIONS_CAPS,
                this
            )
            editor.updateOperation()
            editor.build()
        }
    }

    fun setDisableCertValidation(value: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setDisableCertValidation(value)
        }
    }

    fun setRememberScanSettings(value: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setRememberScanSettings(value)
        }
    }

    fun setScanningResponseTimeout(value: UInt) {
        if (value > Int.MAX_VALUE.toUInt()) {
            return
        }

        viewModelScope.launch {
            appSettingsRepository.setScanningResponseTimeout(value.toInt())
        }
    }

    fun setPdfExportChunkSize(value: UInt) {
        if (value > Int.MAX_VALUE.toUInt()) {
            return
        }

        viewModelScope.launch {
            appSettingsRepository.setPdfExportChunkSize(value.toInt())
        }
    }

    fun setWriteDebugLogs(value: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setWriteDebugLogs(value)
        }
    }

    fun getDefaultPdfExportChunkSize(): UInt = AppSettings().chunkSizeForPDFExport.toUInt()
    fun getDefaultScanningResponseTimeout(): UInt = AppSettings().scanningResponseTimeoutInS.toUInt()

    suspend fun getPdfExportChunkSize(): UInt = appSettingsRepository.getAppSettings().chunkSizeForPDFExport.toUInt()

    suspend fun getScanningResponseTimeout(): UInt = appSettingsRepository.getAppSettings().scanningResponseTimeoutInS.toUInt()
}
