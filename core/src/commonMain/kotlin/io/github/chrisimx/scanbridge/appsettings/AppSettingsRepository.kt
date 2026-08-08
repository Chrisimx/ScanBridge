package io.github.chrisimx.scanbridge.appsettings

import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.scanbridge.db.entities.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AppSettingsRepository {
    suspend fun getAppSettings(): AppSettings
    fun getAppSettingsFlow(): Flow<AppSettings>

    suspend fun setAppSettings(appSettings: AppSettings)
    suspend fun updateAppSettings(updateOperation: AppSettings.() -> AppSettings)
    suspend fun resetAppSettings() {
        setAppSettings(AppSettings())
    }

    suspend fun getPreferredInitialScanSettings(): CommonScanSettings = getAppSettings().preferredInitialScanSettings

    fun getPreferredInitialScanSettingsFlow(): Flow<CommonScanSettings> = getAppSettingsFlow().map {
        it.preferredInitialScanSettings
    }

    suspend fun updatePreferredInitialScanSettings(updateOperation: CommonScanSettings.() -> CommonScanSettings) {
        updateAppSettings {
            copy(preferredInitialScanSettings = preferredInitialScanSettings.updateOperation())
        }
    }

    suspend fun setWriteDebugLogs(value: Boolean) = updateAppSettings {
        copy(writeDebugLogs = value)
    }

    fun getWriteDebugLogsFlow(): Flow<Boolean> = getAppSettingsFlow().map {
        it.writeDebugLogs
    }

    suspend fun setDisableCertValidation(value: Boolean) = updateAppSettings {
        copy(disableCertValidation = value)
    }

    fun getDisableCertValidationFlow(): Flow<Boolean> = getAppSettingsFlow().map {
        it.disableCertValidation
    }

    suspend fun setScanningResponseTimeout(value: Int) = updateAppSettings {
        copy(scanningResponseTimeoutInS = value)
    }

    fun getScanningResponseTimeoutFlow(): Flow<Int> = getAppSettingsFlow().map {
        it.scanningResponseTimeoutInS
    }

    suspend fun setPdfExportChunkSize(value: Int) = updateAppSettings {
        copy(chunkSizeForPDFExport = value)
    }

    fun getPdfExportChunkSizeFlow(): Flow<Int> = getAppSettingsFlow().map {
        it.chunkSizeForPDFExport
    }

    suspend fun setRememberScanSettings(value: Boolean) = updateAppSettings {
        copy(rememberScanSettings = value)
    }

    fun getRememberScanSettingsFlow(): Flow<Boolean> = getAppSettingsFlow().map {
        it.rememberScanSettings
    }
}
