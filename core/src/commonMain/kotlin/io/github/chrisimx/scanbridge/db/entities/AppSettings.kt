package io.github.chrisimx.scanbridge.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appsettings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val writeDebugLogs: Boolean = false,
    val disableCertValidation: Boolean = false,
    val scanningResponseTimeoutInS: Int = 25,
    val chunkSizeForPDFExport: Int = 50,
    val rememberScanSettings: Boolean = true
)
