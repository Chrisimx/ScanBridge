package io.github.chrisimx.scanbridge.migrations.ds2room

import android.content.Context
import io.github.chrisimx.scanbridge.appsettings.D2RAppSettingsMigrationDataSource
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.db.entities.AppSettings
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import io.github.chrisimx.scanbridge.proto.chunkSizePdfExportOrNull
import io.github.chrisimx.scanbridge.proto.rememberScanSettingsOrNull
import io.github.chrisimx.scanbridge.proto.scanningResponseTimeoutOrNull
import kotlinx.coroutines.flow.first

class D2RAppSettingsMigrationDataSourceImpl(context: Context) : D2RAppSettingsMigrationDataSource {
    private val appSettingsStore = context.appSettingsStore

    override suspend fun getAppSettings(): AppSettings {
        val dataStoreAppSettings = appSettingsStore.data.first()

        return dataStoreAppSettings.toAppSettings()
    }

    private fun ScanBridgeSettings.toAppSettings(): AppSettings {
        // The defaults are set in the AppSettings constructor
        // To use them, we create a new instance of AppSettings with these default params
        // and then migrate the values from the legacy datastore to the new that actually have a value.
        var appSettings = AppSettings(
            writeDebugLogs = this.writeDebug,
            disableCertValidation = this.disableCertChecks
        )

        this.scanningResponseTimeoutOrNull?.let {
            appSettings = appSettings.copy(scanningResponseTimeoutInS = it.value)
        }

        this.chunkSizePdfExportOrNull?.let {
            appSettings = appSettings.copy(chunkSizeForPDFExport = it.value)
        }

        this.rememberScanSettingsOrNull?.let {
            appSettings = appSettings.copy(rememberScanSettings = it.value)
        }

        return appSettings
    }
}
