package io.github.chrisimx.scanbridge.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.anyscan.CommonScanSettingsEditor
import io.github.chrisimx.scanbridge.initialscansettings.INITIAL_SCAN_SETTINGS_AVAILABLE_OPTIONS_CAPS

@Entity(tableName = "appsettings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val writeDebugLogs: Boolean = false,
    val disableCertValidation: Boolean = false,
    val scanningResponseTimeoutInS: Int = 25,
    val chunkSizeForPDFExport: Int = 50,
    val rememberScanSettings: Boolean = true,
    // This will use the default values from the capabilities
    val preferredInitialScanSettings: CommonScanSettings = CommonScanSettingsEditor(
        INITIAL_SCAN_SETTINGS_AVAILABLE_OPTIONS_CAPS
    ).build()
)
