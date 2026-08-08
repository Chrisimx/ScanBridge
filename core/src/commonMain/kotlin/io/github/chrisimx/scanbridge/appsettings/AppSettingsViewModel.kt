package io.github.chrisimx.scanbridge.appsettings

import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.db.entities.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map

class AppSettingsViewModel(val appSettingsRepository: AppSettingsRepository, buildInfoProvider: BuildInfoProvider) : ViewModel() {
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
